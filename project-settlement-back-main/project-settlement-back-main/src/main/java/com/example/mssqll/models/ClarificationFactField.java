package com.example.mssqll.models;

import com.example.mssqll.dto.request.ConnectionFeeUpdateRequestDto;

import java.util.function.Function;

/**
 * Whitelisted {@link ConnectionFee} fields a custom clarification fact may attach to.
 * Each constant knows how to extract its (trimmed) value both from the existing
 * {@link ConnectionFee} (the "old" value) and from the submitted
 * {@link ConnectionFeeUpdateRequestDto} (the "new" value), so a fact can compare the
 * two. Using an explicit whitelist (instead of reflection) keeps user-defined facts
 * from reaching arbitrary properties. Only fields present on both the entity and the
 * update DTO are exposed.
 */
public enum ClarificationFactField {

    ORDER_STATUS("Order status",
            f -> f.getOrderStatus() == null ? null : f.getOrderStatus().name(),
            d -> d.getOrderStatus() == null ? null : d.getOrderStatus().name()),
    STATUS("Status",
            f -> f.getStatus() == null ? null : f.getStatus().name(),
            d -> d.getStatus() == null ? null : d.getStatus().name()),
    ORDER_N("Order number (orderN)", ConnectionFee::getOrderN, ConnectionFeeUpdateRequestDto::getOrderN),
    REGION("Region", ConnectionFee::getRegion, ConnectionFeeUpdateRequestDto::getRegion),
    SERVICE_CENTER("Service center", ConnectionFee::getServiceCenter, ConnectionFeeUpdateRequestDto::getServiceCenter),
    PROJECT_ID("Project number (projectID)", ConnectionFee::getProjectID, ConnectionFeeUpdateRequestDto::getProjectID),
    WITHDRAW_TYPE("Withdraw type", ConnectionFee::getWithdrawType, ConnectionFeeUpdateRequestDto::getWithdrawType),
    NOTE("Note", ConnectionFee::getNote, ConnectionFeeUpdateRequestDto::getNote),
    PURPOSE("Purpose", ConnectionFee::getPurpose, ConnectionFeeUpdateRequestDto::getPurpose),
    DESCRIPTION("Description", ConnectionFee::getDescription, ConnectionFeeUpdateRequestDto::getDescription),
    TAX("Tax id", ConnectionFee::getTax, ConnectionFeeUpdateRequestDto::getTax),
    TOTAL_AMOUNT("Total amount",
            f -> f.getTotalAmount() == null ? null : String.valueOf(f.getTotalAmount()),
            d -> d.getTotalAmount() == null ? null : String.valueOf(d.getTotalAmount())),
    TREASURY_REFUND_DATE("Treasury refund date",
            f -> f.getTreasuryRefundDate() == null ? null : f.getTreasuryRefundDate().toString(),
            d -> d.getTreasuryRefundDate() == null ? null : d.getTreasuryRefundDate().toString()),
    CLARIFICATION_DATE("Clarification date",
            f -> f.getClarificationDate() == null ? null : f.getClarificationDate().toString(),
            d -> d.getClarificationDate() == null ? null : d.getClarificationDate().toString());

    private final String label;
    private final Function<ConnectionFee, String> oldExtractor;
    private final Function<ConnectionFeeUpdateRequestDto, String> newExtractor;

    ClarificationFactField(String label,
                           Function<ConnectionFee, String> oldExtractor,
                           Function<ConnectionFeeUpdateRequestDto, String> newExtractor) {
        this.label = label;
        this.oldExtractor = oldExtractor;
        this.newExtractor = newExtractor;
    }

    public String label() {
        return label;
    }

    /** Extract this field's "old" value from the existing fee, trimmed; {@code null} if absent. */
    public String extractOld(ConnectionFee fee) {
        if (fee == null) return null;
        String value = oldExtractor.apply(fee);
        return value == null ? null : value.trim();
    }

    /** Extract this field's "new" value from the submitted update, trimmed; {@code null} if absent. */
    public String extractNew(ConnectionFeeUpdateRequestDto submitted) {
        if (submitted == null) return null;
        String value = newExtractor.apply(submitted);
        return value == null ? null : value.trim();
    }
}
