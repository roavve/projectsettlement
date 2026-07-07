package com.example.mssqll.dto.response;

import com.example.mssqll.models.BusinessUnit;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class BusinessUnitResponseDto {
    public BusinessUnitResponseDto(Long id, String name, Integer unitTypeKey) {
        this.id = id;
        this.name = name;
        this.unitTypeKey = unitTypeKey;
    }

    private Long id;
    private Integer unitNumber;
    private String name;
    private Integer unitTypeKey;
    private BusinessUnit parent;

}
