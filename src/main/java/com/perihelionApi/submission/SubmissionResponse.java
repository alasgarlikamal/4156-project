package com.perihelionApi.submission;

import java.time.Instant;

/**
 * Submission representation returned to clients.
 *
 * @param id           the submission id
 * @param enrollmentId the id of the submitting enrollment
 * @param courseworkId the id of the submitted coursework
 * @param submittedAt  when the work was submitted
 */
public record SubmissionResponse(Long id, Long enrollmentId,
        Long courseworkId, Instant submittedAt) {

    /**
     * Builds a response from an entity.
     *
     * @param submission the entity
     * @return the response
     */
    public static SubmissionResponse from(final Submission submission) {
        return new SubmissionResponse(submission.getId(),
                submission.getEnrollment().getId(),
                submission.getCourseworkId(),
                submission.getSubmittedAt());
    }

}
