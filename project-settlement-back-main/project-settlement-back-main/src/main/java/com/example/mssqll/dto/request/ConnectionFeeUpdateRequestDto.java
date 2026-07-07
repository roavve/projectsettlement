package com.example.mssqll.dto.request;

import com.example.mssqll.models.OrderStatus;
import com.example.mssqll.models.Status;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConnectionFeeUpdateRequestDto {

    @Schema(implementation = Status.class)
    private Status status;

    private String orderN;

    private String region;

    private String serviceCenter;

    private String projectID;

    private String withdrawType;

    private Long extractionTaskId;

    private LocalDateTime clarificationDate;

    private LocalDate treasuryRefundDate;

    private LocalDate paymentOrderSentDate;

    private Long extractionId;

    private String note;

    private LocalDate extractionDate;

    private Double totalAmount;

    private String purpose;

    private String description;

    private String tax;

    @Schema(implementation = OrderStatus.class)
    private OrderStatus orderStatus;

    private List<String> canceledProject;

}
