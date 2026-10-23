/*
    AI usage:Course and Enrollment CRUD: Grok 4.7, ~111K tokens
 */
package com.perihelionApi.enrollment;

import java.util.UUID;

/**
 * Enrollment representation returned to clients.
 *
 * @param id         the enrollment id
 * @param courseId   the id of the enrolled course
 * @param learnerRef the learner's identifier
 */
public record EnrollmentResponse(Long id, Long courseId, UUID learnerRef) {

    /**
     * Builds a response from an entity.
     *
     * @param enrollment the entity
     * @return the response
     */
    public static EnrollmentResponse from(final Enrollment enrollment) {
        return new EnrollmentResponse(enrollment.getId(),
                enrollment.getCourse().getId(),
                UUID.fromString(enrollment.getLearnerRef()));
    }

}
