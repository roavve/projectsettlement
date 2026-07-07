package com.example.mssqll.utiles.exceptions;

import com.example.mssqll.service.WebhookNotifierService;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.AccessDeniedException;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import static com.example.mssqll.utiles.SecurityUtils.getCurrentUsername;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {
    private final WebhookNotifierService webhookNotifierService;

    public GlobalExceptionHandler(WebhookNotifierService webhookNotifierService) {
        this.webhookNotifierService = webhookNotifierService;
    }

    private ResponseEntity<ErrorResponse> notifyAndBuildResponse(Exception ex, String userMessage, HttpStatus status) {
        log.error("Exception occurred: {} - Status: {} - Message: {} (by {})", ex.getClass().getSimpleName(), status, userMessage, getCurrentUsername(), ex);
        webhookNotifierService.sendExceptionNotification(ex);
        ErrorResponse errorResponse = new ErrorResponse(userMessage, ex.getMessage());
        return ResponseEntity.status(status).body(errorResponse);
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NoHandlerFoundException ex) {
        log.warn("No handler found for request: {} (by {})", ex.getRequestURL(), getCurrentUsername());
        return notifyAndBuildResponse(ex, "The requested endpoint does not exist.", HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(RefreshTokenException.class)
    public ResponseEntity<ErrorResponse> handleRefreshToken(RefreshTokenException ex) {
        log.error("Refresh token validation failed: {} (by {})", ex.getMessage(), getCurrentUsername());
        return notifyAndBuildResponse(ex, "Refresh token has some problems.", HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(ResourceNotFoundException ex) {
        log.error("Resource not found: {} (by {})", ex.getMessage(), getCurrentUsername());
        return notifyAndBuildResponse(ex, ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(SQLException.class)
    public ResponseEntity<ErrorResponse> handleSQLException(SQLException ex) {
        log.error("Database error - SQL State: {}, Error Code: {} (by {})", ex.getSQLState(), ex.getErrorCode(), getCurrentUsername(), ex);
        return notifyAndBuildResponse(ex, "Database error occurred.", HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(TokenValidationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ResponseEntity<ErrorResponse> handleTokenValidationException(TokenValidationException ex) {
        log.error("Token validation failed: {} (by {})", ex.getMessage(), getCurrentUsername());
        return notifyAndBuildResponse(ex, "Token validation failed.", HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(FileAlreadyTransferredException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ResponseEntity<ErrorResponse> handleFileAlreadyTransferredException(FileAlreadyTransferredException ex) {
        log.error("File already transferred: {} (by {})", ex.getMessage(), getCurrentUsername());
        return notifyAndBuildResponse(ex, "File transfer conflict.", HttpStatus.CONFLICT);
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException ex) {
        log.error("Access denied: {} (by {})", ex.getMessage(), getCurrentUsername());
        return notifyAndBuildResponse(ex, "Access denied.", HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(UserIsDeletedException.class)
    @ResponseStatus(HttpStatus.GONE)
    public ResponseEntity<ErrorResponse> handleUserIsDeletedException(UserIsDeletedException ex) {
        log.error("Attempt to access deleted user: {} (by {})", ex.getMessage(), getCurrentUsername());
        return notifyAndBuildResponse(ex, "User is deleted.", HttpStatus.GONE);
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleUserAlreadyExistsException(UserAlreadyExistsException ex) {
        log.error("User registration conflict: {} (by {})", ex.getMessage(), getCurrentUsername());
        return notifyAndBuildResponse(ex, "User already exists.", HttpStatus.CONFLICT);
    }

    @ExceptionHandler(DivideException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<ErrorResponse> handleDivideException(DivideException ex) {
        log.error("Division error occurred: {} (by {})", ex.getMessage(), getCurrentUsername(), ex);
        return notifyAndBuildResponse(ex, "Something went wrong", HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(AdminNotEditException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ResponseEntity<ErrorResponse> handleAdminEditException(AdminNotEditException ex) {
        log.error("Admin edit attempt denied: {} (by {})", ex.getMessage(), getCurrentUsername());
        return notifyAndBuildResponse(ex, "Admin edit not allowed.", HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(FileNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleFileNotSupportedException(FileNotSupportedException ex) {
        log.error("Unsupported file format: {} (by {})", ex.getMessage(), getCurrentUsername());
        return notifyAndBuildResponse(ex, "File format not supported.", HttpStatus.UNSUPPORTED_MEDIA_TYPE);
    }

    @ExceptionHandler(AsyncRequestNotUsableException.class)
    public ResponseEntity<Map<String, Object>> handleAsyncRequestNotUsableException(AsyncRequestNotUsableException ex) {
        log.warn("Client disconnected before response could be sent: {} (by {})", ex.getMessage(), getCurrentUsername());
        webhookNotifierService.sendExceptionNotification(ex);
        Map<String, Object> body = new HashMap<>();
        body.put("status", 499);
        body.put("error", "Client Disconnected");
        body.put("message", "The client connection was closed before the response could be sent.");
        return ResponseEntity.status(499).body(body);
    }

    @ExceptionHandler(InvalidFormulaException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ErrorResponse> handleInvalidFormulaException(InvalidFormulaException ex) {
        log.warn("Invalid clarification formula: {} (by {})", ex.getMessage(), getCurrentUsername());
        return notifyAndBuildResponse(ex, ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    // Generic handler for any other exceptions
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        log.error("Unhandled exception of type {}: {} (by {})", ex.getClass().getName(), ex.getMessage(), getCurrentUsername(), ex);
        return notifyAndBuildResponse(ex, "Something went wrong", HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Setter
    @Getter
    public static class ErrorResponse {
        private String message;
        private String exception;

        public ErrorResponse(String message, String exception) {
            this.message = message;
            this.exception = exception;
        }
    }
}
