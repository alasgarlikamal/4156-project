package com.perihelionApi.common;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Translates exceptions thrown by controllers into HTTP error responses.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles bean validation failures on request bodies.
     *
     * @param exception the validation failure
     * @return a 400 response listing the invalid fields
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            final MethodArgumentNotValidException exception) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors().forEach(fieldError ->
                fieldErrors.putIfAbsent(fieldError.getField(),
                        fieldError.getDefaultMessage()));
        exception.getBindingResult().getGlobalErrors().forEach(objectError ->
                fieldErrors.putIfAbsent(objectError.getObjectName(),
                        objectError.getDefaultMessage()));
        return ResponseEntity.badRequest()
                .body(new ErrorResponse("Validation failed", fieldErrors));
    }

    /**
     * Handles request bodies that cannot be parsed.
     *
     * @param exception the parsing failure
     * @return a 400 response
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadable(
            final HttpMessageNotReadableException exception) {
        return ResponseEntity.badRequest()
                .body(new ErrorResponse("Malformed request body", Map.of()));
    }

    /**
     * Handles path or query parameters that cannot be converted.
     *
     * @param exception the conversion failure
     * @return a 400 response naming the offending parameter
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(
            final MethodArgumentTypeMismatchException exception) {
        return ResponseEntity.badRequest()
                .body(new ErrorResponse(
                        "Invalid value for " + exception.getName(), Map.of()));
    }

    /**
     * Handles lookups for resources that do not exist.
     *
     * @param exception the lookup failure
     * @return a 404 response carrying the exception message
     */
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(
            final NotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(exception.getMessage(), Map.of()));
    }

    /**
     * Handles requests that conflict with existing resources.
     *
     * @param exception the conflict
     * @return a 409 response carrying the exception message
     */
    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflict(
            final ConflictException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(exception.getMessage(), Map.of()));
    }

    /**
     * Handles database constraint violations.
     *
     * @param exception the constraint violation
     * @return a 409 response
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(
            final DataIntegrityViolationException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(
                        "Request conflicts with existing data", Map.of()));
    }

}
