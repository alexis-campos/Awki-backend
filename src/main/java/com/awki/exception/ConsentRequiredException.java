package com.awki.exception;

import lombok.Getter;

@Getter
public class ConsentRequiredException extends RuntimeException {

    private final String consentType;

    public ConsentRequiredException(String consentType, String message) {
        super(message);
        this.consentType = consentType;
    }
}
