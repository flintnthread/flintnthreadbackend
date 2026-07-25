package com.ecommerce.authdemo.exception;

/** Raised during registration when the email or mobile is already registered. */
public class DuplicateAccountException extends RuntimeException {
    public DuplicateAccountException(String message) {
        super(message);
    }
}
