package com.awki.exception;

import lombok.Getter;

@Getter
public class OfflineConflictException extends RuntimeException {

    private final String conflictDetails;

    public OfflineConflictException(String conflictDetails, String message) {
        super(message);
        this.conflictDetails = conflictDetails;
    }
}
