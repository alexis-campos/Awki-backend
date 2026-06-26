package com.awki.exception;

public class EmailAlreadyExistsException extends RuntimeException {
    public EmailAlreadyExistsException(String email) {
        super("El email '" + email + "' ya está registrado en el sistema");
    }
}
