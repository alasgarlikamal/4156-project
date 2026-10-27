/*
    AI usage: Task CRUD: Grok 4.7, ~238K tokens
 */
package com.perihelionApi.task;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Request body for creating or replacing a task.
 *
 * @param workPlanId         the work plan the task belongs to
 * @param courseworkId       the coursework the task works toward
 * @param enrollmentId       the enrollment that owns the task
 * @param title              the task title
 * @param targetDate         the day the task should be finished
 * @param estimatedWorkHours estimated effort in hours, including zero
 */
public record TaskRequest(
        @NotNull @Positive Long workPlanId,
        @NotNull @Positive Long courseworkId,
        @NotNull @Positive Long enrollmentId,
        @NotBlank @Size(max = 255) String title,
        @NotNull LocalDate targetDate,
        @NotNull @DecimalMin("0.0") BigDecimal estimatedWorkHours) {
}
