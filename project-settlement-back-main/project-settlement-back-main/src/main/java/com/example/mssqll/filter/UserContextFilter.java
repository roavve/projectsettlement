package com.example.mssqll.filter;

import com.example.mssqll.models.User;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

import static com.example.mssqll.utiles.SecurityUtils.getCurrentUsername;

/**
 * Filter to add user email to request attributes.
 * This allows including user context in log messages.
 */
@Slf4j
@Component
@Order(2) // Execute after JwtAuthenticationFilter (which is Order 1)
public class UserContextFilter extends OncePerRequestFilter {

    public static final String USER_EMAIL_ATTRIBUTE = "userEmail";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // Get authenticated user from SecurityContext
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal())) {

            try {
                Object principal = authentication.getPrincipal();

                if (principal instanceof User) {
                    User user = (User) principal;

                    // Store user email in request attribute
                    request.setAttribute(USER_EMAIL_ATTRIBUTE, user.getEmail());
                }
            } catch (Exception e) {
                log.warn("Failed to extract user information from authentication: {} (by {})", e.getMessage(), getCurrentUsername());
            }
        }

        // Continue with the filter chain
        filterChain.doFilter(request, response);
    }

    /**
     * Helper method to get user email from SecurityContext
     */
    public static String getCurrentUserEmail() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof User) {
                User user = (User) authentication.getPrincipal();
                return user.getEmail();
            }
        } catch (Exception e) {
            // Ignore
        }
        return null;
    }
}
