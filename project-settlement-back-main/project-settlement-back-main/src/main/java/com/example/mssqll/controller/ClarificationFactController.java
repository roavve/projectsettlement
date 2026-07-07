package com.example.mssqll.controller;

import com.example.mssqll.dto.request.ClarificationFactRequestDto;
import com.example.mssqll.models.ClarificationFact;
import com.example.mssqll.service.ClarificationFactService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import static com.example.mssqll.utiles.SecurityUtils.getCurrentUsername;

/**
 * Admin UI backend for user-defined clarification facts: attach a fact to a
 * ConnectionFee field, choose a modification type, and reference it from the
 * clarificationDate formula.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/clarification-facts")
@RequiredArgsConstructor
@Tag(name = "Clarification Fact", description = "User-defined facts referenced by the clarificationDate formula")
public class ClarificationFactController {

    private final ClarificationFactService clarificationFactService;

    @Operation(summary = "List all defined facts")
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','OPERATOR')")
    public ResponseEntity<List<ClarificationFact>> getAll() {
        log.info("Fetching all clarification facts (requested by {})", getCurrentUsername());
        return ResponseEntity.ok(clarificationFactService.getAll());
    }

    @Operation(summary = "List attachable ConnectionFee fields")
    @GetMapping("/fields")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','OPERATOR')")
    public ResponseEntity<Map<String, String>> getFields() {
        log.info("Fetching clarification fact fields (requested by {})", getCurrentUsername());
        return ResponseEntity.ok(clarificationFactService.fieldOptions());
    }

    @Operation(summary = "List available modification types")
    @GetMapping("/modification-types")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','OPERATOR')")
    public ResponseEntity<List<Map<String, Object>>> getModificationTypes() {
        log.info("Fetching clarification fact modification types (requested by {})", getCurrentUsername());
        return ResponseEntity.ok(clarificationFactService.modificationTypeOptions());
    }

    @Operation(summary = "Create a new fact")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','OPERATOR')")
    public ResponseEntity<ClarificationFact> create(@RequestBody ClarificationFactRequestDto request) {
        log.info("Creating clarification fact '{}' (requested by {})", request.getName(), getCurrentUsername());
        return ResponseEntity.ok(clarificationFactService.create(request, getCurrentUsername()));
    }

    @Operation(summary = "Update an existing fact")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','OPERATOR')")
    public ResponseEntity<ClarificationFact> update(@PathVariable Long id,
                                                    @RequestBody ClarificationFactRequestDto request) {
        log.info("Updating clarification fact id={} (requested by {})", id, getCurrentUsername());
        return ResponseEntity.ok(clarificationFactService.update(id, request, getCurrentUsername()));
    }

    @Operation(summary = "Delete a fact")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','OPERATOR')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        log.info("Deleting clarification fact id={} (requested by {})", id, getCurrentUsername());
        clarificationFactService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
