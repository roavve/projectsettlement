package com.example.mssqll.models;

import java.util.Objects;

/**
 * How a custom clarification fact is derived from the old (existing) and new
 * (submitted) value of its attached {@link ClarificationFactField}. Each modification
 * type knows its result {@link FactValueType} and whether it needs one or two
 * comparison values supplied by the admin.
 */
public enum ClarificationFactModification {

    /** old != new */
    CHANGED(FactValueType.BOOLEAN, false, false, "Value changed (old ≠ new)"),
    /** old == new */
    NOT_CHANGED(FactValueType.BOOLEAN, false, false, "Value did not change (old = new)"),
    /** new == value */
    NEW_EQUALS(FactValueType.BOOLEAN, true, false, "New value equals ..."),
    /** old == value */
    OLD_EQUALS(FactValueType.BOOLEAN, true, false, "Old value equals ..."),
    /** new is non-empty */
    NEW_PRESENT(FactValueType.BOOLEAN, false, false, "New value is written (not empty)"),
    /** new is empty */
    NEW_BLANK(FactValueType.BOOLEAN, false, false, "New value is empty"),
    /** old == from && new == to */
    TRANSITION(FactValueType.BOOLEAN, true, true, "Status/value moves from ... to ..."),
    /** expose the new value as text */
    RAW_NEW_VALUE(FactValueType.STRING, false, false, "Expose the new value (text)"),
    /** expose the old value as text */
    RAW_OLD_VALUE(FactValueType.STRING, false, false, "Expose the old value (text)");

    private final FactValueType valueType;
    private final boolean needsValueA;
    private final boolean needsValueB;
    private final String label;

    ClarificationFactModification(FactValueType valueType, boolean needsValueA, boolean needsValueB, String label) {
        this.valueType = valueType;
        this.needsValueA = needsValueA;
        this.needsValueB = needsValueB;
        this.label = label;
    }

    public FactValueType valueType() {
        return valueType;
    }

    public boolean needsValueA() {
        return needsValueA;
    }

    public boolean needsValueB() {
        return needsValueB;
    }

    public String label() {
        return label;
    }

    /**
     * Compute the fact value. Boolean modifications return a {@link Boolean};
     * {@code RAW_*} modifications return the raw {@link String} (possibly null).
     */
    public Object compute(String oldValue, String newValue, String valueA, String valueB) {
        String a = trim(valueA);
        String b = trim(valueB);
        switch (this) {
            case CHANGED:       return !Objects.equals(oldValue, newValue);
            case NOT_CHANGED:   return Objects.equals(oldValue, newValue);
            case NEW_EQUALS:    return Objects.equals(newValue, a);
            case OLD_EQUALS:    return Objects.equals(oldValue, a);
            case NEW_PRESENT:   return isPresent(newValue);
            case NEW_BLANK:     return !isPresent(newValue);
            case TRANSITION:    return Objects.equals(oldValue, a) && Objects.equals(newValue, b);
            case RAW_NEW_VALUE: return newValue;
            case RAW_OLD_VALUE: return oldValue;
            default:            return false;
        }
    }

    private static boolean isPresent(String value) {
        return value != null && !value.isBlank();
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }
}
