/*
    AI usage: Course and Enrollment CRUD: Grok 4.7, ~111K tokens
 */
package com.perihelionApi.enrollment;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

/**
 * Request body for creating or replacing an enrollment.
 *
 * @param courseId   the id of the course to enroll in
 * @param learnerRef the learner's identifier
 */
public record EnrollmentRequest(@NotNull Long courseId,
        @NotNull UUID learnerRef) {
}
