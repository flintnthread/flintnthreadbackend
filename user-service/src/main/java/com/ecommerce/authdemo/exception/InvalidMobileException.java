package com.ecommerce.authdemo.exception;

public class InvalidMobileException extends RuntimeException {
    public InvalidMobileException(String message) {
        super(message);
    }
}