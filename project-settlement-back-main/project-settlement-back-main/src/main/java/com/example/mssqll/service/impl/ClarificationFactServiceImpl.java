package com.example.mssqll.service.impl;

import com.example.mssqll.dto.request.ClarificationFactRequestDto;
import com.example.mssqll.dto.request.ConnectionFeeUpdateRequestDto;
import com.example.mssqll.models.ClarificationFact;
import com.example.mssqll.models.ClarificationFactField;
import com.example.mssqll.models.ClarificationFactModification;
import com.example.mssqll.models.ConnectionFee;
import com.example.mssqll.models.FactValueType;
import com.example.mssqll.repository.ClarificationFactRepository;
import com.example.mssqll.service.ClarificationFactService;
import com.example.mssqll.utiles.exceptions.InvalidFormulaException;
import com.example.mssqll.utiles.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import static com.example.mssqll.utiles.SecurityUtils.getCurrentUsername;

/**
 * CRUD + runtime computation for user-defined clarification facts.
 * See {@link ClarificationFactService}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClarificationFactServiceImpl implements ClarificationFactService {

    /** A legal formula variable identifier. */
    private static final Pattern NAME_PATTERN = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]*$");

    /** Built-in fact names a custom fact may not shadow (mirrors ClarificationRuleServiceImpl). */
    private static final Set<String> RESERVED_NAMES = Set.of(
            "clarificationAlreadySet", "projectIdChanged", "isNewStatusInProgress",
            "isMtbOrder", "isStatusInProgressToReturned", "isMtbReturnTransition",
            "withdrawType", "oldStatus", "newStatus");

    private final ClarificationFactRepository clarificationFactRepository;

    @Override
    public List<ClarificationFact> getAll() {
        return clarificationFactRepository.findAll();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ClarificationFact create(ClarificationFactRequestDto request, String user) {
        String name = request.getName() == null ? null : request.getName().trim();
        ClarificationFactField field = parseField(request.getSourceField());
        ClarificationFactModification modification = parseModification(request.getModificationType());
        validateName(name, null);
        validateValues(modification, request.getValueA(), request.getValueB());

        ClarificationFact fact = ClarificationFact.builder()
                .name(name)
                .description(request.getDescription())
                .sourceField(field)
                .modificationType(modification)
                .valueType(modification.valueType())
                .valueA(request.getValueA())
                .valueB(request.getValueB())
                .active(request.getActive() == null || request.getActive())
                .createdBy(user)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        ClarificationFact saved = clarificationFactRepository.save(fact);
        log.info("[clarificationFact] Created fact '{}' ({} {}) (by {})", saved.getName(), field, modification, user);
        return saved;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ClarificationFact update(Long id, ClarificationFactRequestDto request, String user) {
        ClarificationFact fact = clarificationFactRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Clarification fact not found: " + id));
        String name = request.getName() == null ? null : request.getName().trim();
        ClarificationFactField field = parseField(request.getSourceField());
        ClarificationFactModification modification = parseModification(request.getModificationType());
        validateName(name, id);
        validateValues(modification, request.getValueA(), request.getValueB());

        fact.setName(name);
        fact.setDescription(request.getDescription());
        fact.setSourceField(field);
        fact.setModificationType(modification);
        fact.setValueType(modification.valueType());
        fact.setValueA(request.getValueA());
        fact.setValueB(request.getValueB());
        if (request.getActive() != null) fact.setActive(request.getActive());
        fact.setUpdatedAt(LocalDateTime.now());
        ClarificationFact saved = clarificationFactRepository.save(fact);
        log.info("[clarificationFact] Updated fact '{}' (id={}) (by {})", saved.getName(), id, user);
        return saved;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        ClarificationFact fact = clarificationFactRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Clarification fact not found: " + id));
        clarificationFactRepository.delete(fact);
        log.info("[clarificationFact] Deleted fact '{}' (id={}) (by {})", fact.getName(), id, getCurrentUsername());
    }

    @Override
    public Map<String, Object> computeFacts(ConnectionFee existing, ConnectionFeeUpdateRequestDto submitted) {
        Map<String, Object> facts = new LinkedHashMap<>();
        for (ClarificationFact fact : clarificationFactRepository.findAllByActiveTrue()) {
            try {
                String oldValue = fact.getSourceField().extractOld(existing);
                String newValue = fact.getSourceField().extractNew(submitted);
                Object value = fact.getModificationType().compute(oldValue, newValue, fact.getValueA(), fact.getValueB());
                facts.put(fact.getName(), value);
            } catch (Exception ex) {
                log.error("[clarificationFact] Failed to compute fact '{}', skipping.", fact.getName(), ex);
            }
        }
        return facts;
    }

    @Override
    public Map<String, Object> sampleValues() {
        Map<String, Object> samples = new LinkedHashMap<>();
        for (ClarificationFact fact : clarificationFactRepository.findAllByActiveTrue()) {
            samples.put(fact.getName(), fact.getValueType() == FactValueType.STRING ? "" : Boolean.FALSE);
        }
        return samples;
    }

    @Override
    public Map<String, String> describeAll() {
        Map<String, String> descriptions = new LinkedHashMap<>();
        for (ClarificationFact fact : clarificationFactRepository.findAllByActiveTrue()) {
            descriptions.put(fact.getName(), describe(fact));
        }
        return descriptions;
    }

    @Override
    public Map<String, String> fieldOptions() {
        Map<String, String> options = new LinkedHashMap<>();
        for (ClarificationFactField field : ClarificationFactField.values()) {
            options.put(field.name(), field.label());
        }
        return options;
    }

    @Override
    public List<Map<String, Object>> modificationTypeOptions() {
        List<Map<String, Object>> options = new ArrayList<>();
        for (ClarificationFactModification modification : ClarificationFactModification.values()) {
            Map<String, Object> option = new LinkedHashMap<>();
            option.put("value", modification.name());
            option.put("label", modification.label());
            option.put("valueType", modification.valueType().label());
            option.put("needsValueA", modification.needsValueA());
            option.put("needsValueB", modification.needsValueB());
            options.add(option);
        }
        return options;
    }

    // ---- helpers -----------------------------------------------------------

    private String describe(ClarificationFact fact) {
        StringBuilder sb = new StringBuilder(fact.getValueType().label()).append(" — ");
        if (fact.getDescription() != null && !fact.getDescription().isBlank()) {
            sb.append(fact.getDescription());
        } else {
            sb.append(fact.getSourceField().label()).append(": ").append(fact.getModificationType().label());
            if (fact.getModificationType().needsValueA() && fact.getValueA() != null) sb.append(" '").append(fact.getValueA()).append("'");
            if (fact.getModificationType().needsValueB() && fact.getValueB() != null) sb.append(" → '").append(fact.getValueB()).append("'");
        }
        return sb.toString();
    }

    private ClarificationFactField parseField(String raw) {
        if (raw == null || raw.isBlank()) throw new InvalidFormulaException("Fact field is required.");
        try {
            return ClarificationFactField.valueOf(raw.trim());
        } catch (IllegalArgumentException ex) {
            throw new InvalidFormulaException("Unknown fact field: " + raw);
        }
    }

    private ClarificationFactModification parseModification(String raw) {
        if (raw == null || raw.isBlank()) throw new InvalidFormulaException("Fact modification type is required.");
        try {
            return ClarificationFactModification.valueOf(raw.trim());
        } catch (IllegalArgumentException ex) {
            throw new InvalidFormulaException("Unknown modification type: " + raw);
        }
    }

    private void validateName(String name, Long currentId) {
        if (name == null || name.isBlank()) {
            throw new InvalidFormulaException("Fact name is required.");
        }
        if (!NAME_PATTERN.matcher(name).matches()) {
            throw new InvalidFormulaException("Fact name must be a valid identifier (letters, digits, underscore; not starting with a digit): " + name);
        }
        if (RESERVED_NAMES.contains(name)) {
            throw new InvalidFormulaException("'" + name + "' is a built-in fact name and cannot be reused.");
        }
        clarificationFactRepository.findByName(name).ifPresent(existing -> {
            if (!existing.getId().equals(currentId)) {
                throw new InvalidFormulaException("A fact named '" + name + "' already exists.");
            }
        });
    }

    private void validateValues(ClarificationFactModification modification, String valueA, String valueB) {
        if (modification.needsValueA() && (valueA == null || valueA.isBlank())) {
            throw new InvalidFormulaException("Modification '" + modification.label() + "' requires a value.");
        }
        if (modification.needsValueB() && (valueB == null || valueB.isBlank())) {
            throw new InvalidFormulaException("Modification '" + modification.label() + "' requires a second (\"to\") value.");
        }
    }
}
