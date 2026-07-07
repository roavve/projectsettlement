package com.example.mssqll.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class UserSearchDto {
    Long id;
    String firstName;
    String lastName;
}
