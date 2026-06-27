package com.ecommerce.authdemo.exception;

public class SmsSendException extends RuntimeException {
    public SmsSendException(String message) {
        super(message);
    }
}
