package com.example.mssqll.dto.response;


import com.example.mssqll.models.ExtractionTask;
import com.example.mssqll.models.Status;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;


@Data
@Builder
public class ExtractionResponseDto {

    private Long id;

    private LocalDate date;

    private Double totalAmount;

    private String purpose;

    private String description;

    private Status status;

    private ExtractionTask extractionTask;


}
