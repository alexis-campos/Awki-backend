package com.awki.exception;

public class TenantViolationException extends RuntimeException {

    public TenantViolationException(String message) {
        super(message);
    }
}
