package com.example.mssqll.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClarificationRuleUpdateRequestDto {
    /** The new SpEL boolean formula to activate. */
    private String formula;
    /** Optional note describing the change. */
    private String description;
}
