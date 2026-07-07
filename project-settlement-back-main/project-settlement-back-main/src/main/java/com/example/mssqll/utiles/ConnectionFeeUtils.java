package com.example.mssqll.utiles;

import com.example.mssqll.dto.request.ConnectionFeeUpdateRequestDto;
import com.example.mssqll.models.ConnectionFee;
import com.example.mssqll.models.OrderStatus;
import com.example.mssqll.models.User;
import com.example.mssqll.service.ClarificationFactService;
import com.example.mssqll.service.ClarificationRuleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.example.mssqll.utiles.SecurityUtils.getCurrentUsername;

/**
 * Helper logic extracted from {@code ConnectionFeeServiceImpl.updateFee}: the field-change audit
 * logging and the runtime-editable {@code clarificationDate} rule application. Both read the
 * existing fee's persisted ("old") state and therefore must be invoked BEFORE any setter mutates
 * {@code existingFee}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ConnectionFeeUtils {

    private final ClarificationRuleService clarificationRuleService;
    private final ClarificationFactService clarificationFactService;

    /**
     * Build a human-readable diff of the fields that will change on this update and emit it as the
     * update audit log line. Call BEFORE any setter runs on {@code existingFee}, while it still
     * holds the persisted ("old") values.
     */
    public void logFieldChanges(ConnectionFee existingFee,
                                ConnectionFeeUpdateRequestDto connectionFeeDetails,
                                Long connectionFeeId,
                                User userDetails) {
        String newRegion = connectionFeeDetails.getRegion() != null ? connectionFeeDetails.getRegion().trim() : null;
        String newServiceCenter = connectionFeeDetails.getServiceCenter() != null ? connectionFeeDetails.getServiceCenter().trim() : null;
        String newWithdrawType = connectionFeeDetails.getWithdrawType() != null ? connectionFeeDetails.getWithdrawType().trim() : null;
        String newProjectID = connectionFeeDetails.getProjectID() != null && !connectionFeeDetails.getProjectID().trim().isEmpty() ? connectionFeeDetails.getProjectID().trim() : null;
        String newNote = connectionFeeDetails.getNote() != null ? connectionFeeDetails.getNote().trim() : null;
        String newPurpose = connectionFeeDetails.getPurpose() != null ? connectionFeeDetails.getPurpose().trim() : null;
        String newDescription = connectionFeeDetails.getDescription() != null ? connectionFeeDetails.getDescription().trim() : null;

        StringBuilder changes = new StringBuilder();
        if (!Objects.equals(existingFee.getStatus(), connectionFeeDetails.getStatus()))
            changes.append(String.format("%n  status: '%s' -> '%s'", existingFee.getStatus(), connectionFeeDetails.getStatus()));
        if (!Objects.equals(existingFee.getOrderStatus(), connectionFeeDetails.getOrderStatus()))
            changes.append(String.format("%n  orderStatus: '%s' -> '%s'", existingFee.getOrderStatus(), connectionFeeDetails.getOrderStatus()));
        if (!Objects.equals(existingFee.getRegion(), newRegion))
            changes.append(String.format("%n  region: '%s' -> '%s'", existingFee.getRegion(), newRegion));
        if (!Objects.equals(existingFee.getServiceCenter(), newServiceCenter))
            changes.append(String.format("%n  serviceCenter: '%s' -> '%s'", existingFee.getServiceCenter(), newServiceCenter));
        if (!Objects.equals(existingFee.getWithdrawType(), newWithdrawType))
            changes.append(String.format("%n  withdrawType: '%s' -> '%s'", existingFee.getWithdrawType(), newWithdrawType));
        if (!Objects.equals(existingFee.getPaymentOrderSentDate(), connectionFeeDetails.getPaymentOrderSentDate()))
            changes.append(String.format("%n  paymentOrderSentDate: '%s' -> '%s'", existingFee.getPaymentOrderSentDate(), connectionFeeDetails.getPaymentOrderSentDate()));
        if (connectionFeeDetails.getExtractionTaskId() != null && !Objects.equals(
                existingFee.getExtractionTask() != null ? existingFee.getExtractionTask().getId() : null,
                connectionFeeDetails.getExtractionTaskId()))
            changes.append(String.format("%n  extractionTaskId: '%s' -> '%s'",
                    existingFee.getExtractionTask() != null ? existingFee.getExtractionTask().getId() : null,
                    connectionFeeDetails.getExtractionTaskId()));
        if (!Objects.equals(existingFee.getNote(), newNote))
            changes.append(String.format("%n  note: '%s' -> '%s'", existingFee.getNote(), newNote));
        if (!Objects.equals(existingFee.getExtractionDate(), connectionFeeDetails.getExtractionDate()))
            changes.append(String.format("%n  extractionDate: '%s' -> '%s'", existingFee.getExtractionDate(), connectionFeeDetails.getExtractionDate()));
        if (!Objects.equals(existingFee.getTotalAmount(), connectionFeeDetails.getTotalAmount()))
            changes.append(String.format("%n  totalAmount: '%s' -> '%s'", existingFee.getTotalAmount(), connectionFeeDetails.getTotalAmount()));
        if (connectionFeeDetails.getPurpose() != null && !Objects.equals(existingFee.getPurpose(), newPurpose))
            changes.append(String.format("%n  purpose: '%s' -> '%s'", existingFee.getPurpose(), newPurpose));
        if (connectionFeeDetails.getDescription() != null && !Objects.equals(existingFee.getDescription(), newDescription))
            changes.append(String.format("%n  description: '%s' -> '%s'", existingFee.getDescription(), newDescription));
        if (!Objects.equals(existingFee.getTreasuryRefundDate(), connectionFeeDetails.getTreasuryRefundDate()))
            changes.append(String.format("%n  treasuryRefundDate: '%s' -> '%s'", existingFee.getTreasuryRefundDate(), connectionFeeDetails.getTreasuryRefundDate()));
        if (!Objects.equals(existingFee.getProjectID(), newProjectID))
            changes.append(String.format("%n  projectID: '%s' -> '%s'", existingFee.getProjectID(), newProjectID));
        if (!Objects.equals(existingFee.getOrderN(), connectionFeeDetails.getOrderN()))
            changes.append(String.format("%n  orderN: '%s' -> '%s'", existingFee.getOrderN(), connectionFeeDetails.getOrderN()));
        if (!Objects.equals(existingFee.getClarificationDate(), connectionFeeDetails.getClarificationDate()))
            changes.append(String.format("%n  clarificationDate: '%s' -> '%s'", existingFee.getClarificationDate(), connectionFeeDetails.getClarificationDate()));

        log.info("[updateFee] User '{}' (id={}) updating ConnectionFee id={}.{}",
                userDetails.getUsername(), userDetails.getId(), connectionFeeId,
                changes.length() > 0 ? " Changes:" + changes : " No field changes detected.");
    }

    /**
     * Decide — via the runtime-editable rule (a SpEL formula over the computed facts) — whether
     * {@code clarificationDate} should be stamped on this update, and apply it. When the rule
     * matches, sets {@code clarificationDate = now} and records the change person. Reads the
     * existing fee's old {@code orderStatus} / {@code orderN} / {@code clarificationDate}, so it
     * must run BEFORE those setters.
     * <p>
     * Main rule: once {@code clarificationDate} is set it is never changed again. It is set only
     * when it is not already set and either (1) the new status is IN_PROGRESS and a project number
     * is written or changed, or (2) an MTB order (withdrawType 6) moves from IN_PROGRESS to
     * RETURNED. See {@link ClarificationRuleService} / the admin UI at {@code /clarification-rule.html}.
     */
    public void applyClarificationDate(ConnectionFee existingFee,
                                       ConnectionFeeUpdateRequestDto connectionFeeDetails,
                                       Long connectionFeeId,
                                       String newProjectID,
                                       User userDetails) {
        String newWithdrawType = connectionFeeDetails.getWithdrawType() != null ? connectionFeeDetails.getWithdrawType().trim() : null;

        boolean clarificationAlreadySet = existingFee.getClarificationDate() != null;
        boolean projectIdChanged = !Objects.equals(existingFee.getProjectID(), newProjectID);
        boolean isNewStatusInProgress = connectionFeeDetails.getOrderStatus() == OrderStatus.IN_PROGRESS;

        // "MTB" if either the new or the existing orderN is MTB
        String existingOrderN = existingFee.getOrderN() != null ? existingFee.getOrderN().trim() : null;
        String submittedOrderN = connectionFeeDetails.getOrderN() != null ? connectionFeeDetails.getOrderN().trim() : null;
        boolean isMtbOrder = "MTB".equals(existingOrderN) || "MTB".equals(submittedOrderN);

        // Order status moving from IN_PROGRESS to RETURNED
        boolean isStatusInProgressToReturned = existingFee.getOrderStatus() == OrderStatus.IN_PROGRESS
                && connectionFeeDetails.getOrderStatus() == OrderStatus.RETURNED;
        boolean isMtbReturnTransition = isMtbOrder
                && "6".equals(newWithdrawType)
                && isStatusInProgressToReturned;

        Map<String, Object> clarificationFacts = new HashMap<>();
        clarificationFacts.put("clarificationAlreadySet", clarificationAlreadySet);
        clarificationFacts.put("projectIdChanged", projectIdChanged);
        clarificationFacts.put("isNewStatusInProgress", isNewStatusInProgress);
        clarificationFacts.put("isMtbOrder", isMtbOrder);
        clarificationFacts.put("isStatusInProgressToReturned", isStatusInProgressToReturned);
        clarificationFacts.put("isMtbReturnTransition", isMtbReturnTransition);
        clarificationFacts.put("withdrawType", newWithdrawType);
        clarificationFacts.put("oldStatus", existingFee.getOrderStatus() != null ? existingFee.getOrderStatus().name() : null);
        clarificationFacts.put("newStatus", connectionFeeDetails.getOrderStatus() != null ? connectionFeeDetails.getOrderStatus().name() : null);
        // Merge user-defined facts (attached to fields via the admin UI).
        clarificationFacts.putAll(clarificationFactService.computeFacts(existingFee, connectionFeeDetails));

        String activeClarificationFormula = clarificationRuleService.getActiveRule().getFormula();
        boolean shouldUpdateClarificationDate = clarificationRuleService.evaluate(clarificationFacts);

        if (shouldUpdateClarificationDate) {
            // The reasons are the facts that evaluated to true (built-in + user-defined).
            List<String> triggeringFacts = clarificationFacts.entrySet().stream()
                    .filter(e -> Boolean.TRUE.equals(e.getValue()))
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());
            log.info("[updateFee] clarificationDate SET for ConnectionFee id={} using formula '{}' | triggering facts: {} | all facts: {} (by {})",
                    connectionFeeId, activeClarificationFormula, triggeringFacts, clarificationFacts, getCurrentUsername());
            existingFee.setClarificationDate(LocalDateTime.now());
            existingFee.setChangePerson(userDetails);
        } else {
            log.info("[updateFee] clarificationDate left UNCHANGED for ConnectionFee id={} using formula '{}' | facts: {} (by {})",
                    connectionFeeId, activeClarificationFormula, clarificationFacts, getCurrentUsername());
        }
    }

    /**
     * If EVERY editable order/identification field is null in the update request
     * ({@code orderN}, {@code orderStatus}, {@code region}, {@code serviceCenter},
     * {@code projectID}, {@code withdrawType}, {@code note}, {@code canceledProject}), treat it as
     * a "reset this fee" request: clear those fields on {@code existingFee} and return {@code true}
     * so the caller can persist and stop. Otherwise leave the fee untouched and return
     * {@code false} (a normal field update should proceed).
     */
    public boolean clearConnectionFeeIfRequestEmpty(ConnectionFee existingFee,
                                                    ConnectionFeeUpdateRequestDto connectionFeeDetails) {
        // Treat null, blank strings ("") and empty lists ([]) all as "empty" — the frontend does
        // not always send literal null when clearing a field.
        boolean allEmpty = isBlank(connectionFeeDetails.getOrderN())
                && connectionFeeDetails.getOrderStatus() == null
                && isBlank(connectionFeeDetails.getRegion())
                && isBlank(connectionFeeDetails.getServiceCenter())
                && isBlank(connectionFeeDetails.getProjectID())
                && isBlank(connectionFeeDetails.getWithdrawType())
                && isBlank(connectionFeeDetails.getNote())
                && (connectionFeeDetails.getCanceledProject() == null || connectionFeeDetails.getCanceledProject().isEmpty());
        if (!allEmpty) {
            return false;
        }
        clearConnectionFee(existingFee);
        return true;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    /**
     * Reset the editable order/identification fields of a fee back to empty: clears
     * {@code orderN}, {@code orderStatus}, {@code region}, {@code serviceCenter},
     * {@code projectID}, {@code withdrawType}, {@code note}, the {@code canceledProject} list and
     * {@code clarificationDate}. All other fields (amounts, other dates, audit columns, relations,
     * children) are left untouched.
     */
    private void clearConnectionFee(ConnectionFee existingFee) {
        existingFee.setOrderN(null);
        existingFee.setOrderStatus(null);
        existingFee.setRegion(null);
        existingFee.setServiceCenter(null);
        existingFee.setProjectID(null);
        existingFee.setWithdrawType(null);
        existingFee.setNote(null);
        existingFee.setClarificationDate(null);
        // Mutate the managed @ElementCollection in place rather than swapping the reference.
        if (existingFee.getCanceledProject() != null) {
            existingFee.getCanceledProject().clear();
        } else {
            existingFee.setCanceledProject(new ArrayList<>());
        }
        log.info("[clearConnectionFee] Cleared orderN/orderStatus/region/serviceCenter/projectID/withdrawType/note/canceledProject for ConnectionFee id={} (by {})",
                existingFee.getId(), getCurrentUsername());
    }
}
