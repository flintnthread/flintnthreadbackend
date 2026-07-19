package com.ecommerce.authdemo.exception;

public class SignupException extends RuntimeException {
    private final String code;

    public SignupException(String message) {
        this(message, null);
    }

    public SignupException(String message, String code) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
