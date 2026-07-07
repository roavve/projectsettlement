package com.example.mssqll.service;

import com.example.mssqll.dto.request.ClarificationFactRequestDto;
import com.example.mssqll.dto.request.ConnectionFeeUpdateRequestDto;
import com.example.mssqll.models.ClarificationFact;
import com.example.mssqll.models.ConnectionFee;

import java.util.List;
import java.util.Map;

/**
 * Manages user-defined "facts" that the clarificationDate formula may reference.
 * Facts attach to a ConnectionFee field and are derived via a modification type
 * (changed / equals / transition / ...). They are edited from the admin UI and
 * computed at update time.
 */
public interface ClarificationFactService {

    /** All defined facts (active and inactive). */
    List<ClarificationFact> getAll();

    /** Create a new fact (validates name/field/modification/values). */
    ClarificationFact create(ClarificationFactRequestDto request, String user);

    /** Update an existing fact. */
    ClarificationFact update(Long id, ClarificationFactRequestDto request, String user);

    /** Delete a fact by id. */
    void delete(Long id);

    /**
     * Compute every active fact against the old (existing) fee and the new (submitted)
     * update. Returns a map of {factName -> value} (Boolean or String) to merge into
     * the facts passed to the formula.
     */
    Map<String, Object> computeFacts(ConnectionFee existing, ConnectionFeeUpdateRequestDto submitted);

    /** Default sample values per active fact (for formula validation dry-runs). */
    Map<String, Object> sampleValues();

    /** {factName -> description} for every active fact (merged into the available-facts list). */
    Map<String, String> describeAll();

    /** Attachable ConnectionFee fields, as {enumName -> label}. */
    Map<String, String> fieldOptions();

    /** Available modification types with their metadata (valueType, needs values, label). */
    List<Map<String, Object>> modificationTypeOptions();
}
