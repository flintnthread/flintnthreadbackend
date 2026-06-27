package com.ecommerce.authdemo.exception;

public class OtpNotFoundException extends RuntimeException {
    public OtpNotFoundException(String message) {
        super(message);
    }
}
