package com.example.mssqll.utiles;

import com.example.mssqll.models.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Utility class for security-related operations
 */
public class SecurityUtils {

    /**
     * Gets the current authenticated user's email/username
     * @return User email or "anonymous" if not authenticated
     */
    public static String getCurrentUsername() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) {
                Object principal = authentication.getPrincipal();

                if (principal instanceof User) {
                    return ((User) principal).getEmail();
                } else if (principal instanceof String && !"anonymousUser".equals(principal)) {
                    return (String) principal;
                }
            }
        } catch (Exception e) {
            // Ignore - not in an authenticated context
        }
        return "anonymous";
    }

    /**
     * Gets the current authenticated user's email from Authentication object
     * @param authentication The authentication object
     * @return User email or "anonymous" if not authenticated
     */
    public static String getUsername(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            Object principal = authentication.getPrincipal();

            if (principal instanceof User) {
                return ((User) principal).getEmail();
            } else if (principal instanceof String && !"anonymousUser".equals(principal)) {
                return (String) principal;
            }
        }
        return "anonymous";
    }

    /**
     * Gets the current authenticated user
     * @return User object or null if not authenticated
     */
    public static User getCurrentUser() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof User) {
                return (User) authentication.getPrincipal();
            }
        } catch (Exception e) {
            // Ignore
        }
        return null;
    }
}
