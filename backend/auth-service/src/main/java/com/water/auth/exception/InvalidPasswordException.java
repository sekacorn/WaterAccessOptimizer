package com.water.auth.exception;

/**
 * Exception thrown when password does not meet strength requirements
 * (min 8 chars, 1 uppercase, 1 lowercase, 1 number)
 */
public class InvalidPasswordException extends RuntimeException {
    public InvalidPasswordException(String message) {
        super(message);
    }
}
