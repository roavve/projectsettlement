package com.example.mssqll.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Create/update payload for a user-defined clarification fact. {@code sourceField}
 * and {@code modificationType} are the enum names (see the {@code /fields} and
 * {@code /modification-types} endpoints); {@code valueType} is derived server-side.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClarificationFactRequestDto {
    /** Formula variable name, referenced as {@code #name}. */
    private String name;
    private String description;
    /** {@link com.example.mssqll.models.ClarificationFactField} name. */
    private String sourceField;
    /** {@link com.example.mssqll.models.ClarificationFactModification} name. */
    private String modificationType;
    /** First comparison value (equals value / transition "from"). */
    private String valueA;
    /** Second comparison value (transition "to"). */
    private String valueB;
    /** Whether the fact is active (defaults to true when null). */
    private Boolean active;
}
