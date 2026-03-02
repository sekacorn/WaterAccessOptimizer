package com.water.auth.exception;

/**
 * Exception thrown when attempting to login to a locked account
 * (after exceeding failed login attempts threshold)
 */
public class AccountLockedException extends RuntimeException {
    public AccountLockedException(String message) {
        super(message);
    }
}
