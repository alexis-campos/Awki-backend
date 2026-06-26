package com.awki.exception;

public class LinkCodeExpiredException extends RuntimeException {

    public LinkCodeExpiredException(String message) {
        super(message);
    }
}
