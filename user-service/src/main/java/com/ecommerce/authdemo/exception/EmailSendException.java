package com.ecommerce.authdemo.exception;

public class EmailSendException extends RuntimeException {
    public EmailSendException(String message) { super(message); }
}
