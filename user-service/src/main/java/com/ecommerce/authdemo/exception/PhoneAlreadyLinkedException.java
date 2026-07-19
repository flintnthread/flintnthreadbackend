package com.ecommerce.authdemo.exception;

public class PhoneAlreadyLinkedException extends RuntimeException {
    public PhoneAlreadyLinkedException(String message) {
        super(message);
    }
}
