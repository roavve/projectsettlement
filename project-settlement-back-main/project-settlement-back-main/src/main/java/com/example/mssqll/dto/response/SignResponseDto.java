package com.example.mssqll.dto.response;

import com.example.mssqll.models.JwtAuthenticationResponse;
import lombok.Builder;
import lombok.Data;


@Data
@Builder
public class SignResponseDto {
    private JwtAuthenticationResponse jwtAuthenticationResponse;
    private UserResponseDto user;
}
