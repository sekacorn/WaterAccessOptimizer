package com.water.auth.exception;

/**
 * Exception thrown when user provides invalid credentials during login
 */
public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException(String message) {
        super(message);
    }
}
