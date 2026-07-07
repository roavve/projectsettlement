package com.example.mssqll.controller.Auth;

import com.example.mssqll.dto.response.SignResponseDto;
import com.example.mssqll.dto.response.UserResponseDto;
import com.example.mssqll.models.JwtAuthenticationResponse;
import com.example.mssqll.models.SignInRequest;
import com.example.mssqll.models.SignUpRequest;
import com.example.mssqll.models.User;
import com.example.mssqll.service.impl.AuthenticationService;
import com.example.mssqll.utiles.exceptions.UserAlreadyExistsException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import static com.example.mssqll.utiles.SecurityUtils.getCurrentUsername;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;


@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Sign up, sign in, logout and token management")
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    @Operation(summary = "Register a new user (ADMIN only)")
    @PostMapping("/signup")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> signup(@RequestBody SignUpRequest request) {
        log.info("User signup attempt for email: {} (requested by {})", request.getEmail(), getCurrentUsername());
        try {
            JwtAuthenticationResponse response = authenticationService.signup(request);
            log.info("User successfully signed up: {} (requested by {})", request.getEmail(), getCurrentUsername());
            return ResponseEntity.ok(response);
        } catch (UserAlreadyExistsException e) {
            log.warn("Signup failed - user already exists: {} (by {})", request.getEmail(), getCurrentUsername());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(e.getMessage());
        } catch (Exception e) {
            log.error("Error during signup for email: {}, error: {} (by {})", request.getEmail(), e.getMessage(), getCurrentUsername());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("სერვერზე დავიქსირდა შეცდომა.");
        }
    }

    @Operation(summary = "Authenticate and receive JWT tokens")
    @PostMapping("/signin")
    public ResponseEntity<?> signIn(@RequestBody SignInRequest request) {
        log.info("User signin attempt for email: {} (requested by {})", request.getEmail(), getCurrentUsername());
        HashMap<String, String> response = new HashMap<>();
        try {
            SignResponseDto res = authenticationService.signin(request);
            log.info("User successfully signed in: {} (requested by {})", request.getEmail(), getCurrentUsername());
            return ResponseEntity.ok(res);
        } catch (BadCredentialsException e) {
            log.warn("Signin failed - bad credentials for email: {} (by {})", request.getEmail(), getCurrentUsername());
            response.put("error", "მონაცემები არასწორია.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(response);
        } catch (Exception e) {
            log.error("Error during signin for email: {} (by {})", request.getEmail(), getCurrentUsername(), e);
            response.put("error", "INTERNAL_SERVER_ERROR");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(response);
        }
    }


    @Operation(summary = "Get the currently authenticated user")
    @GetMapping("/user")
    public ResponseEntity<?> getAuthenticatedUser() {
        log.info("Fetching authenticated user details (requested by {})", getCurrentUsername());
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User userDetails = (User) authentication.getPrincipal();
        UserResponseDto dto = UserResponseDto.builder()
                .id(userDetails.getId())
                .role(userDetails.getRole())
                .email(userDetails.getEmail())
                .updatedAt(userDetails.getUpdatedAt())
                .firstName(userDetails.getFirstName())
                .lastName(userDetails.getLastName())
                .createdAt(userDetails.getCreatedAt())
                .build();
        log.info("Retrieved user details for user id: {} (requested by {})", userDetails.getId(), getCurrentUsername());
        return ResponseEntity.ok(dto);
    }

    @Operation(summary = "Invalidate the current access token")
    @PostMapping("/logout")
    public ResponseEntity<?> logoutUser(@RequestHeader(name = "Authorization") String authorization) {
        log.info("User logout request (requested by {})", getCurrentUsername());
        return authenticationService.logout(authorization);
    }

    @Operation(summary = "Issue a new access token using a refresh token (header: AuthRefresh)")
    @PostMapping("/refresh-token")
    public ResponseEntity<?> refreshAccessToken(@RequestHeader(name="AuthRefresh") String refreshToken) {
        log.info("Token refresh request (requested by {})", getCurrentUsername());
        try {
            String newAccessToken = authenticationService.refreshAccessToken(refreshToken);
            log.info("Successfully refreshed access token (requested by {})", getCurrentUsername());
            return ResponseEntity.ok(new JwtAuthenticationResponse(newAccessToken, refreshToken));
        } catch (Exception e) {
            log.error("Token refresh failed: {} (by {})", e.getMessage(), getCurrentUsername());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or expired refresh token.");
        }
    }
}