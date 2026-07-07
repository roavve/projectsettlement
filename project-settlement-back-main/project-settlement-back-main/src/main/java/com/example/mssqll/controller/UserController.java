package com.example.mssqll.controller;

import com.example.mssqll.dto.response.UserResponseDto;
import com.example.mssqll.dto.response.UserSearchDto;
import com.example.mssqll.models.User;
import com.example.mssqll.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

import static com.example.mssqll.utiles.SecurityUtils.getCurrentUsername;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/user")
@Tag(name = "Users", description = "User management operations")
public class UserController {
    @Autowired
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "Update a user (ADMIN only)")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<UserResponseDto> updateUser(@RequestBody User user, @PathVariable Long id) {
        log.info("Updating user with id: {} (requested by {})", id, getCurrentUsername());
        UserResponseDto updatedUser = userService.updateUser(user, id);
        log.info("Successfully updated user with id: {} (requested by {})", id, getCurrentUsername());
        return ResponseEntity.ok(updatedUser);
    }

    @Operation(summary = "Delete a user (ADMIN only)")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        log.info("Deleting user with id: {} (requested by {})", id, getCurrentUsername());
        UserResponseDto isDeleted = userService.deleteUser(id);
        Map<String, Object> response = new HashMap<>();
        response.put("user", isDeleted);
        log.info("Successfully deleted user with id: {} (requested by {})", id, getCurrentUsername());
        return ResponseEntity.ok(response);
    }


    @Operation(summary = "Search users by username or email")
    @GetMapping
    public ResponseEntity<List<UserSearchDto>> getUsers(
            @RequestParam(required =false) String username,
            @RequestParam(required = false) String email) {
        log.info("Fetching users with filters - username {}, email: {} (requested by {})",
                username, email, getCurrentUsername());

        System.out.println("username: " + username);
        Map<String, Object> filters = new HashMap<>();
        if (email != null) filters.put("email", email);
        if (username != null) filters.put("username", username);

        List<UserSearchDto> dtos;
        dtos = userService.searchUsers(filters);

        log.info("Retrieved {} users (requested by {})", dtos.size(), getCurrentUsername());
        return ResponseEntity.ok(dtos);
    }

}
