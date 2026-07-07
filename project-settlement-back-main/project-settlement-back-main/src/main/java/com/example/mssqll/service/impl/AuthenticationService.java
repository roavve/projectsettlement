package com.example.mssqll.service.impl;

import com.example.mssqll.configuration.LoginAttemptService;
import com.example.mssqll.dto.response.SignResponseDto;
import com.example.mssqll.dto.response.UserResponseDto;
import com.example.mssqll.models.*;
import com.example.mssqll.repository.UserRepository;
import com.example.mssqll.utiles.exceptions.UserAlreadyExistsException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestHeader;

import static com.example.mssqll.utiles.SecurityUtils.getCurrentUsername;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final UserRepository userRepository;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final LoginAttemptService loginAttemptService;

    @PreAuthorize("hasRole('ADMIN')")
    public JwtAuthenticationResponse signup(SignUpRequest request) {
        log.info("Attempting user signup for email: {} (by {})", request.getEmail(), getCurrentUsername());
        log.debug("Signup request - FirstName: {}, LastName: {}, Role: {} (by {})",
                  request.getFirstName(), request.getLastName(), request.getRole(), getCurrentUsername());

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            log.warn("Signup failed - user already exists with email: {} (by {})", request.getEmail(), getCurrentUsername());
            throw new UserAlreadyExistsException("User already exists");
        }

        var user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .build();

        user = userService.save(user);
        log.info("User successfully created with ID: {}, Email: {} (by {})", user.getId(), user.getEmail(), getCurrentUsername());

        log.debug("Generating JWT tokens for user ID: {} (by {})", user.getId(), getCurrentUsername());
        var jwt = jwtService.generateToken(user, false);
        log.info("Signup completed successfully for user: {} (by {})", user.getEmail(), getCurrentUsername());

        return JwtAuthenticationResponse.builder()
                .refreshToken(jwtService.generateRefreshToken(user))
                .token(jwt)
                .build();
    }


    public SignResponseDto signin(SignInRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        log.info("Attempting user signin for email: {} (by {})", email, getCurrentUsername());

        if (loginAttemptService.isLocked(email)) {
            log.warn("Signin attempt on locked account: {} (by {})",
                    email, getCurrentUsername());
            throw new BadCredentialsException("Invalid email or password.");
        }

        long delayMs = loginAttemptService.getDelayMs(email);
        if (delayMs > 0) {
            log.debug("Applying {} ms delay for email: {}", delayMs, email);
            try {
                Thread.sleep(delayMs);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.getPassword()));
            log.debug("Authentication successful for email: {} (by {})", email, getCurrentUsername());
        } catch (BadCredentialsException e) {
            loginAttemptService.loginFailed(email);
            log.warn("Signin failed - bad credentials for email: {} (by {})", email, getCurrentUsername());
            throw new BadCredentialsException("Invalid email or password.");
        } catch (Exception e) {
            log.error("Unexpected error during authentication for email: {} (by {})", email, getCurrentUsername(), e);
            throw e;
        }

        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    loginAttemptService.loginFailed(email);
                    log.warn("User not found after successful authentication for email: {} (by {})", email, getCurrentUsername());
                    return new BadCredentialsException("Invalid email or password.");
                });

        if (user.getRole() == Role.SOFT_DELETED) {
            log.warn("Signin attempt for soft-deleted user: {} (by {})", email, getCurrentUsername());
            loginAttemptService.loginFailed(email);
            throw new BadCredentialsException("Invalid email or password.");
        }

        loginAttemptService.loginSucceeded(email);

        log.debug("Generating JWT tokens for user ID: {} (by {})", user.getId(), getCurrentUsername());
        var jwt = jwtService.generateToken(user, false);

        UserResponseDto userDto = UserResponseDto.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();

        log.info("Signin completed successfully for user: {} (by {})", user.getEmail(), getCurrentUsername());

        return SignResponseDto.builder().
                jwtAuthenticationResponse(JwtAuthenticationResponse.builder().token(jwt).refreshToken(jwtService.generateRefreshToken(user)).build())
                .user(userDto)
                .build();
    }

    public ResponseEntity<?> logout(@RequestHeader(name = "Authorization") String authorization) {
        log.info("Attempting user logout (by {})", getCurrentUsername());

        if (authorization != null && authorization.startsWith("Bearer ")) {
            String token = authorization.substring(7);
            log.debug("Processing logout with token length: {} (by {})", token.length(), getCurrentUsername());

            jwtService.logout(token);
            log.info("User logged out successfully (by {})", getCurrentUsername());

            Map<String, String> response = new HashMap<>();
            response.put("message", "Logged out successfully.");
            return ResponseEntity.status(200).body(response);
        }

        log.warn("Logout failed - invalid authorization header (by {})", getCurrentUsername());
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("error", "Invalid authorization.");

        return ResponseEntity.badRequest().body(errorResponse);
    }

    public String refreshAccessToken(String refreshToken){
        log.info("Attempting to refresh access token (by {})", getCurrentUsername());
        log.debug("Refresh token length: {} (by {})", refreshToken != null ? refreshToken.length() : 0, getCurrentUsername());

        try {
            String newAccessToken = jwtService.refreshAccessToken(refreshToken);
            log.info("Access token refreshed successfully (by {})", getCurrentUsername());
            return newAccessToken;
        } catch (Exception e) {
            log.error("Failed to refresh access token (by {})", getCurrentUsername(), e);
            throw e;
        }
    }
}