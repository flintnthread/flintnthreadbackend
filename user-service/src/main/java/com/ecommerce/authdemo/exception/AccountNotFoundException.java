package com.ecommerce.authdemo.exception;

/** Raised during login when no account matches the supplied email/mobile. */
public class AccountNotFoundException extends RuntimeException {
    public AccountNotFoundException(String message) {
        super(message);
    }
}
