package com.example.mssqll.dto.response;


public class TokenValidationResult {
    private boolean valid;
    private String message;

    public TokenValidationResult(boolean valid, String message) {
        this.valid = valid;
        this.message = message;
    }

    public boolean isValid() {
        return valid;
    }

    public String getMessage() {
        return message;
    }
}
