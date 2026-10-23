package com.perihelionApi.common;

import java.util.Map;

/**
 * Error payload returned to clients when a request fails.
 *
 * @param message     a human-readable summary of the failure
 * @param fieldErrors validation messages keyed by field name, if any
 */
public record ErrorResponse(String message, Map<String, String> fieldErrors) {
}
