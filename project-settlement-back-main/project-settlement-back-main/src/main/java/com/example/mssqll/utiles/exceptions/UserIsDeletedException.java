package com.example.mssqll.utiles.exceptions;

public class UserIsDeletedException extends RuntimeException {
    public UserIsDeletedException(String message) {
        super(message);
    }
}
