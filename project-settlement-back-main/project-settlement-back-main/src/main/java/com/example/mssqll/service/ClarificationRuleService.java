package com.example.mssqll.service;

import com.example.mssqll.models.ClarificationRule;

import java.util.List;
import java.util.Map;

/**
 * Runtime-editable business rule that decides whether {@code clarificationDate}
 * should be set on a ConnectionFee update. The rule is a SpEL boolean formula
 * evaluated against a map of pre-computed facts.
 */
public interface ClarificationRuleService {

    /** The current active rule (seeded with the default formula if none exists). */
    ClarificationRule getActiveRule();

    /** Names of the facts a formula may reference, with a short description of each. */
    Map<String, String> getAvailableFacts();

    /** The built-in default formula (mirrors the original hardcoded logic). */
    String getDefaultFormula();

    /**
     * Evaluate the active formula against the given facts. Never throws: if the
     * stored formula fails, it falls back to the default formula, and ultimately
     * to {@code false} (i.e. leave clarificationDate untouched).
     */
    boolean evaluate(Map<String, Object> facts);

    /**
     * Parse + evaluate a formula against a sample fact set without persisting.
     * Returns {valid, message, result?}.
     */
    Map<String, Object> validateFormula(String formula);

    /** Evaluate an arbitrary formula against caller-supplied facts (dry run). */
    Map<String, Object> testFormula(String formula, Map<String, Object> facts);

    /** Validate then persist a new active formula. */
    ClarificationRule updateFormula(String formula, String description, String updatedBy);

    /** Reset the active rule back to the built-in default formula. */
    ClarificationRule resetToDefault(String updatedBy);

    /** History of rules (most recent first). */
    List<ClarificationRule> history();
}
