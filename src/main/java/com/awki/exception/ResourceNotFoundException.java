package com.awki.exception;

import lombok.Getter;

@Getter
public class ResourceNotFoundException extends RuntimeException {

    private final String resource;
    private final String identifier;

    public ResourceNotFoundException(String resource, String identifier) {
        super(resource + " no encontrado con identificador: " + identifier);
        this.resource = resource;
        this.identifier = identifier;
    }
}
