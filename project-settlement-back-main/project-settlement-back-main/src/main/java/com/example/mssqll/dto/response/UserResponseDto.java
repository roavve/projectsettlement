package com.example.mssqll.dto.response;

import com.example.mssqll.models.Role;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class UserResponseDto {

    Long id;

    String firstName;

    String lastName;

    String email;

    Role role;

    LocalDateTime createdAt;

    LocalDateTime updatedAt;
}
