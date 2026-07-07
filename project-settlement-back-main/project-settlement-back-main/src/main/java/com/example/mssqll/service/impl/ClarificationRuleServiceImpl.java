package com.example.mssqll.service.impl;

import com.example.mssqll.models.ClarificationRule;
import com.example.mssqll.repository.ClarificationRuleRepository;
import com.example.mssqll.service.ClarificationFactService;
import com.example.mssqll.service.ClarificationRuleService;
import com.example.mssqll.utiles.exceptions.InvalidFormulaException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.SimpleEvaluationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.example.mssqll.utiles.SecurityUtils.getCurrentUsername;

/**
 * Evaluates the runtime-editable {@code clarificationDate} rule as a SpEL boolean
 * expression against a map of pre-computed "facts". Facts are exposed to the formula
 * as SpEL variables (e.g. {@code #clarificationAlreadySet}). Only boolean/relational
 * logic is allowed (no type references or bean lookups) via {@link SimpleEvaluationContext}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClarificationRuleServiceImpl implements ClarificationRuleService {

    /**
     * Built-in default formula:
     * set the clarification date only if it is not already set, the new status is
     * NOT IN_PROGRESS, and an MTB order (withdrawType 6) moves from IN_PROGRESS to
     * RETURNED. When the new status is IN_PROGRESS the date is never set/updated.
     */
    private static final String DEFAULT_FORMULA =
            "!#clarificationAlreadySet && !#isNewStatusInProgress && #isMtbReturnTransition";

    /** Facts a formula may reference, with a short human description of each. */
    private static final Map<String, String> AVAILABLE_FACTS = buildAvailableFacts();

    private final ClarificationRuleRepository clarificationRuleRepository;
    private final ClarificationFactService clarificationFactService;

    private final ExpressionParser parser = new SpelExpressionParser();

    private static Map<String, String> buildAvailableFacts() {
        Map<String, String> facts = new LinkedHashMap<>();
        facts.put("clarificationAlreadySet", "boolean — the clarificationDate is already filled in (once set, it is frozen).");
        facts.put("projectIdChanged", "boolean — the project number (projectID) was written or changed in this update.");
        facts.put("isNewStatusInProgress", "boolean — the new order status is IN_PROGRESS (პროცესში).");
        facts.put("isMtbOrder", "boolean — either the new or existing order number (orderN) equals \"MTB\".");
        facts.put("isStatusInProgressToReturned", "boolean — the order status moves from IN_PROGRESS to RETURNED.");
        facts.put("isMtbReturnTransition", "boolean — MTB order + withdrawType 6 + IN_PROGRESS → RETURNED transition.");
        facts.put("withdrawType", "String — the submitted withdraw type (e.g. \"6\").");
        facts.put("oldStatus", "String — the existing order status name (e.g. \"IN_PROGRESS\").");
        facts.put("newStatus", "String — the submitted order status name (e.g. \"RETURNED\").");
        return facts;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ClarificationRule getActiveRule() {
        return clarificationRuleRepository.findFirstByActiveTrueOrderByUpdatedAtDesc()
                .orElseGet(this::seedDefault);
    }

    @Override
    public Map<String, String> getAvailableFacts() {
        Map<String, String> facts = new LinkedHashMap<>(AVAILABLE_FACTS);
        facts.putAll(clarificationFactService.describeAll());
        return facts;
    }

    @Override
    public String getDefaultFormula() {
        return DEFAULT_FORMULA;
    }

    @Override
    public boolean evaluate(Map<String, Object> facts) {
        String formula = getActiveRule().getFormula();
        try {
            boolean result = evaluateFormula(formula, facts);
            log.info("[clarificationRule] clarificationDate decision={} using ACTIVE formula '{}' | facts={} (by {})",
                    result, formula, facts, getCurrentUsername());
            return result;
        } catch (Exception ex) {
            log.error("[clarificationRule] Active formula failed to evaluate, falling back to default. formula='{}' (by {})",
                    formula, getCurrentUsername(), ex);
            try {
                boolean result = evaluateFormula(DEFAULT_FORMULA, facts);
                log.warn("[clarificationRule] clarificationDate decision={} using DEFAULT formula '{}' | facts={} (by {})",
                        result, DEFAULT_FORMULA, facts, getCurrentUsername());
                return result;
            } catch (Exception fallbackEx) {
                log.error("[clarificationRule] Default formula also failed to evaluate, defaulting to false. (by {})",
                        getCurrentUsername(), fallbackEx);
                return false;
            }
        }
    }

    @Override
    public Map<String, Object> validateFormula(String formula) {
        return testFormula(formula, sampleFacts());
    }

    @Override
    public Map<String, Object> testFormula(String formula, Map<String, Object> facts) {
        String effectiveFormula = (formula == null || formula.isBlank())
                ? getActiveRule().getFormula()
                : formula;
        Map<String, Object> factsToUse = (facts == null || facts.isEmpty()) ? sampleFacts() : facts;

        Map<String, Object> response = new LinkedHashMap<>();
        try {
            boolean result = evaluateFormula(effectiveFormula, factsToUse);
            response.put("valid", true);
            response.put("result", result);
            response.put("message", "Formula is valid.");
        } catch (Exception ex) {
            response.put("valid", false);
            response.put("result", null);
            response.put("message", ex.getMessage());
        }
        return response;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ClarificationRule updateFormula(String formula, String description, String updatedBy) {
        if (formula == null || formula.isBlank()) {
            throw new InvalidFormulaException("Formula must not be empty.");
        }
        // Fail fast if the formula does not parse/evaluate against the sample facts.
        try {
            evaluateFormula(formula, sampleFacts());
        } catch (Exception ex) {
            log.warn("[clarificationRule] Rejected invalid formula '{}': {} (by {})", formula, ex.getMessage(), getCurrentUsername());
            throw new InvalidFormulaException("Invalid formula: " + ex.getMessage());
        }

        deactivateCurrent();

        ClarificationRule rule = ClarificationRule.builder()
                .formula(formula.trim())
                .description(description)
                .active(true)
                .updatedBy(updatedBy)
                .updatedAt(LocalDateTime.now())
                .build();
        ClarificationRule saved = clarificationRuleRepository.save(rule);
        log.info("[clarificationRule] Active formula updated to '{}' (by {})", saved.getFormula(), updatedBy);
        return saved;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ClarificationRule resetToDefault(String updatedBy) {
        log.info("[clarificationRule] Resetting formula to default (by {})", updatedBy);
        return updateFormula(DEFAULT_FORMULA, "Reset to built-in default formula.", updatedBy);
    }

    @Override
    public List<ClarificationRule> history() {
        return clarificationRuleRepository.findAll(Sort.by(Sort.Direction.DESC, "updatedAt"));
    }

    // ---- helpers -----------------------------------------------------------

    private boolean evaluateFormula(String formula, Map<String, Object> facts) {
        Expression expression = parser.parseExpression(formula);
        SimpleEvaluationContext context = SimpleEvaluationContext
                .forReadOnlyDataBinding()
                .withInstanceMethods()
                .build();
        if (facts != null) {
            facts.forEach(context::setVariable);
        }
        Boolean result = expression.getValue(context, Boolean.class);
        if (result == null) {
            throw new InvalidFormulaException("Formula did not evaluate to a boolean.");
        }
        return result;
    }

    private ClarificationRule seedDefault() {
        log.info("[clarificationRule] No active rule found, seeding built-in default formula.");
        ClarificationRule rule = ClarificationRule.builder()
                .formula(DEFAULT_FORMULA)
                .description("Built-in default formula (auto-seeded).")
                .active(true)
                .updatedBy("system")
                .updatedAt(LocalDateTime.now())
                .build();
        return clarificationRuleRepository.save(rule);
    }

    private void deactivateCurrent() {
        clarificationRuleRepository.findFirstByActiveTrueOrderByUpdatedAtDesc()
                .ifPresent(current -> {
                    current.setActive(false);
                    clarificationRuleRepository.save(current);
                });
    }

    /** A representative fact set used to validate formulas before persisting. */
    private Map<String, Object> sampleFacts() {
        Map<String, Object> facts = new LinkedHashMap<>();
        facts.put("clarificationAlreadySet", false);
        facts.put("projectIdChanged", true);
        facts.put("isNewStatusInProgress", true);
        facts.put("isMtbOrder", false);
        facts.put("isStatusInProgressToReturned", false);
        facts.put("isMtbReturnTransition", false);
        facts.put("withdrawType", "6");
        facts.put("oldStatus", "IN_PROGRESS");
        facts.put("newStatus", "RETURNED");
        // Include user-defined facts so formulas referencing them still validate.
        facts.putAll(clarificationFactService.sampleValues());
        return facts;
    }
}
