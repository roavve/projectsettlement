package com.example.mssqll.utiles.exceptions;

public class FileNotSupportedException extends RuntimeException {
    public FileNotSupportedException(String message) {
        super(message);
    }
}