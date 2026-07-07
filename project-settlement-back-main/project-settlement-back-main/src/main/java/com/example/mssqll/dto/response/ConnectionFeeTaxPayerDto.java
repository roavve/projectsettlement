package com.example.mssqll.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class  ConnectionFeeTaxPayerDto {

    private Long id;

    private String region;

    private String serviceCenter;

    private String tax;

    private Double totalAmount;

}
