package com.example.mssqll.models;

/**
 * The value type a clarification fact produces when evaluated. Boolean facts are
 * true/false flags (e.g. "projectID changed"); string facts expose a raw field
 * value for {@code ==} comparisons inside a formula.
 */
public enum FactValueType {
    BOOLEAN("boolean"),
    STRING("String");

    private final String label;

    FactValueType(String label) {
        this.label = label;
    }

    /** Human label used in fact descriptions / the editor UI. */
    public String label() {
        return label;
    }
}
