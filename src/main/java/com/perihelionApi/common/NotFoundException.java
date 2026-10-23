package com.perihelionApi.common;

/**
 * Thrown when a requested resource does not exist.
 */
public class NotFoundException extends RuntimeException {

    /**
     * Creates a new not-found exception.
     *
     * @param message a description of what was not found
     */
    public NotFoundException(final String message) {
        super(message);
    }

}
