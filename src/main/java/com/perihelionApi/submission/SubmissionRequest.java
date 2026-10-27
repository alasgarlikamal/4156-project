/*
    AI usage: Submission coursework link: Grok 4.7, ~238K tokens
 */
package com.perihelionApi.submission;

import java.time.Instant;

import jakarta.validation.constraints.NotNull;

/**
 * Request body for creating or replacing a submission.
 *
 * @param enrollmentId the id of the submitting enrollment
 * @param courseworkId the id of the submitted coursework
 * @param submittedAt  when the work was submitted; on create, null means
 *                     now, and on update, null keeps the stored time
 */
public record SubmissionRequest(@NotNull Long enrollmentId,
        @NotNull Long courseworkId, Instant submittedAt) {
}
