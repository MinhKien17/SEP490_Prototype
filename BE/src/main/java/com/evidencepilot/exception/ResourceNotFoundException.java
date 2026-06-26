package com.evidencepilot.exception;

import java.util.UUID;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(UUID id, String entityName) {
        super(String.format("%s with id %s not found", entityName, id));
    }

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
