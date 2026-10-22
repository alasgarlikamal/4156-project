package com.perihelionApi.common;

/**
 * Thrown when a request conflicts with the current state of a resource.
 */
public class ConflictException extends RuntimeException {

    /**
     * Creates a new conflict exception.
     *
     * @param message a description of the conflict
     */
    public ConflictException(final String message) {
        super(message);
    }

}
