package com.example.mssqll.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * A dry-run request: evaluate {@code formula} against a caller-supplied set of
 * sample facts, without persisting anything.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClarificationRuleTestRequestDto {
    /** Formula to test. If null/blank, the currently active formula is used. */
    private String formula;
    /** Sample fact values, e.g. {"clarificationAlreadySet": false, "withdrawType": "6"}. */
    private Map<String, Object> facts;
}
