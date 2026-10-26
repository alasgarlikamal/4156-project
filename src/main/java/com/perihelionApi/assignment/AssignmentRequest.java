package com.perihelionApi.assignment;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Request body for creating or replacing an assignment.
 *
 * @param courseId the course id
 * @param title the assignment title
 * @param dueDate the assignment deadline
 * @param estimatedWorkHours estimated effort in hours, including zero
 */
public record AssignmentRequest(
        @NotNull @Positive Long courseId,
        @NotBlank @Size(max = 255) String title,
        @NotNull LocalDate dueDate,
        @NotNull @DecimalMin("0.0") BigDecimal estimatedWorkHours) {
}
