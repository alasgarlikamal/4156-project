/*
    AI usage: Coursework CRUD: Grok 4.7, ~238K tokens
 */
package com.perihelionApi.coursework;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Request body for creating or replacing coursework.
 *
 * @param courseId             the owning course id
 * @param kind                 EXAM or ASSIGNMENT
 * @param title                the coursework title
 * @param deadline             when the work is due
 * @param estimatedEffortHours estimated effort in hours, including zero
 */
public record CourseworkRequest(
        @NotNull @Positive Long courseId,
        @NotNull CourseworkKind kind,
        @NotBlank @Size(max = 255) String title,
        @NotNull Instant deadline,
        @NotNull @DecimalMin("0.0") BigDecimal estimatedEffortHours) {
}
