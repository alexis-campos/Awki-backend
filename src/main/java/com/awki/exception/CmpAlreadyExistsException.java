package com.awki.exception;

public class CmpAlreadyExistsException extends RuntimeException {
    public CmpAlreadyExistsException(String cmp) {
        super("El CMP '" + cmp + "' ya está registrado en el sistema");
    }
}
