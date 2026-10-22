/*
	AI usage: Course and Enrollment CRUD: Grok 4.7, ~111K tokens
 */
package com.perihelionApi.enrollment;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record EnrollmentRequest(@NotNull Long courseId, @NotNull UUID learnerRef) {
}
