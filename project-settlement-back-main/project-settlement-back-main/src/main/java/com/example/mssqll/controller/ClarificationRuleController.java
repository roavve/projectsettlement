package com.example.mssqll.controller;

import com.example.mssqll.dto.request.ClarificationRuleTestRequestDto;
import com.example.mssqll.dto.request.ClarificationRuleUpdateRequestDto;
import com.example.mssqll.models.ClarificationRule;
import com.example.mssqll.service.ClarificationRuleService;
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
 * Admin UI backend for the runtime-editable {@code clarificationDate} rule.
 * Lets an admin inspect the available facts, edit / test / reset the SpEL formula
 * that decides whether {@code clarificationDate} is set on a ConnectionFee update.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/clarification-rules")
@RequiredArgsConstructor
@Tag(name = "Clarification Rule", description = "Runtime-editable clarificationDate business rule")
public class ClarificationRuleController {

    private final ClarificationRuleService clarificationRuleService;

    @Operation(summary = "Get the currently active clarificationDate rule")
    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','OPERATOR')")
    public ResponseEntity<ClarificationRule> getActiveRule() {
        log.info("Fetching active clarification rule (requested by {})", getCurrentUsername());
        return ResponseEntity.ok(clarificationRuleService.getActiveRule());
    }

    @Operation(summary = "List the facts a formula may reference, with descriptions")
    @GetMapping("/facts")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','OPERATOR')")
    public ResponseEntity<Map<String, String>> getAvailableFacts() {
        log.info("Fetching available clarification-rule facts (requested by {})", getCurrentUsername());
        return ResponseEntity.ok(clarificationRuleService.getAvailableFacts());
    }

    @Operation(summary = "Get the built-in default formula")
    @GetMapping("/default")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','OPERATOR')")
    public ResponseEntity<Map<String, String>> getDefaultFormula() {
        log.info("Fetching default clarification formula (requested by {})", getCurrentUsername());
        return ResponseEntity.ok(Map.of("formula", clarificationRuleService.getDefaultFormula()));
    }

    @Operation(summary = "Validate a formula against a sample fact set (no persistence)")
    @PostMapping("/validate")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','OPERATOR')")
    public ResponseEntity<Map<String, Object>> validateFormula(@RequestBody ClarificationRuleUpdateRequestDto request) {
        log.info("Validating clarification formula (requested by {})", getCurrentUsername());
        return ResponseEntity.ok(clarificationRuleService.validateFormula(request.getFormula()));
    }

    @Operation(summary = "Dry-run a formula against caller-supplied facts (no persistence)")
    @PostMapping("/test")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','OPERATOR')")
    public ResponseEntity<Map<String, Object>> testFormula(@RequestBody ClarificationRuleTestRequestDto request) {
        log.info("Testing clarification formula against sample facts (requested by {})", getCurrentUsername());
        return ResponseEntity.ok(clarificationRuleService.testFormula(request.getFormula(), request.getFacts()));
    }

    @Operation(summary = "Validate then persist a new active formula")
    @PutMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','OPERATOR')")
    public ResponseEntity<ClarificationRule> updateFormula(@RequestBody ClarificationRuleUpdateRequestDto request) {
        log.info("Updating active clarification formula (requested by {})", getCurrentUsername());
        ClarificationRule saved = clarificationRuleService.updateFormula(
                request.getFormula(), request.getDescription(), getCurrentUsername());
        return ResponseEntity.ok(saved);
    }

    @Operation(summary = "Reset the active rule back to the built-in default formula")
    @PostMapping("/reset")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','OPERATOR')")
    public ResponseEntity<ClarificationRule> resetToDefault() {
        log.info("Resetting active clarification formula to default (requested by {})", getCurrentUsername());
        return ResponseEntity.ok(clarificationRuleService.resetToDefault(getCurrentUsername()));
    }

    @Operation(summary = "Rule change history (most recent first)")
    @GetMapping("/history")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','OPERATOR')")
    public ResponseEntity<List<ClarificationRule>> history() {
        log.info("Fetching clarification rule history (requested by {})", getCurrentUsername());
        return ResponseEntity.ok(clarificationRuleService.history());
    }
}
