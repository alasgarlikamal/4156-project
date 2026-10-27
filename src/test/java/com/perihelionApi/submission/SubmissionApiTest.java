/*
    AI usage: Submission API Tests: Opus 5.5, ~113K tokens
 */
package com.perihelionApi.submission;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import com.perihelionApi.ApiTestSupport;

/**
 * Full-application API tests for {@code /api/submissions}, written from the
 * API description, with nothing mocked.
 *
 * <p>Each test creates its own courses, enrollments, coursework and
 * submissions over HTTP, so no test depends on another test, on the order
 * the tests run in, or on the rows already in the database.
 */
@DisplayName("Submissions API")
class SubmissionApiTest extends ApiTestSupport {

    /** Base path of the submission endpoints. */
    private static final String PATH = "/api/submissions";

    /** Error message when the submission does not exist. */
    private static final String SUBMISSION_NOT_FOUND = "Submission not found";

    /** Error message when the enrollment does not exist. */
    private static final String ENROLLMENT_NOT_FOUND = "Enrollment not found";

    /** Error message when the coursework does not exist. */
    private static final String COURSEWORK_NOT_FOUND = "Coursework not found";

    /** Error message when the coursework is from another course. */
    private static final String WRONG_COURSE =
            "Coursework must belong to the enrollment's course";

    /** Error message when the enrollment already submitted the coursework. */
    private static final String DUPLICATE =
            "A submission already exists for this enrollment and coursework";

    /** Error message for a bean-validation failure. */
    private static final String VALIDATION_FAILED = "Validation failed";

    /** The only message a {@code @NotNull} field can give. */
    private static final String MUST_NOT_BE_NULL = "must not be null";

    /** The submission time that {@code createSubmission} sends. */
    private static final String SUBMITTED_AT = "2026-11-10T12:00:00Z";

    /** A later whole-second submission time, for updates. */
    private static final String LATER = "2026-11-12T08:30:00Z";

    /**
     * Returns the path of one submission.
     *
     * @param id the submission id
     * @return the path, such as {@code /api/submissions/7}
     */
    private static String pathOf(final long id) {
        return PATH + "/" + id;
    }

    /**
     * Returns an id as an int. JsonPath reads a number that fits in an int
     * as an Integer, so Hamcrest matchers such as {@code hasItem} must be
     * given an int to find it.
     *
     * @param id the id
     * @return the same id as an int
     */
    private static int jsonInt(final long id) {
        return Math.toIntExact(id);
    }

    /**
     * Sends a GET request for one submission.
     *
     * @param id the submission id
     * @return the result, for assertions
     * @throws Exception if the request cannot be sent
     */
    private ResultActions getSubmission(final long id) throws Exception {
        return mvc().perform(get(pathOf(id)));
    }

    /**
     * Checks with a GET request that a submission is stored with the given
     * enrollment, coursework and submission time.
     *
     * @param id the submission id
     * @param enrollmentId the expected enrollment id
     * @param courseworkId the expected coursework id
     * @param submittedAt the expected submission time, such as
     *     "2026-11-10T12:00:00Z"
     * @throws Exception if the request cannot be sent or a check fails
     */
    private void assertStored(final long id, final long enrollmentId,
            final long courseworkId, final String submittedAt)
            throws Exception {
        getSubmission(id)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.enrollmentId").value(enrollmentId))
                .andExpect(jsonPath("$.courseworkId").value(courseworkId))
                .andExpect(jsonPath("$.submittedAt").value(submittedAt));
    }

    /** Tests for POST /api/submissions, which records a submission. */
    @Nested
    @DisplayName("POST /api/submissions")
    class Create {

        @Test
        @DisplayName("valid request with submittedAt returns 201 with all "
                + "fields")
        void validRequestReturns201() throws Exception {
            long courseId = createCourse();
            long enrollmentId = createEnrollment(courseId);
            long courseworkId = createCoursework(courseId);
            String body =
                    submissionJson(enrollmentId, courseworkId, SUBMITTED_AT);

            ResultActions result = postJson(PATH, body);

            result.andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").isNumber())
                    .andExpect(jsonPath("$.enrollmentId").value(enrollmentId))
                    .andExpect(jsonPath("$.courseworkId").value(courseworkId))
                    .andExpect(jsonPath("$.submittedAt").value(SUBMITTED_AT));
            assertStored(idOf(result), enrollmentId, courseworkId,
                    SUBMITTED_AT);
        }

        @Test
        @DisplayName("omitted submittedAt is filled in by the server")
        void omittedSubmittedAtIsFilledIn() throws Exception {
            long courseId = createCourse();
            long enrollmentId = createEnrollment(courseId);
            long courseworkId = createCoursework(courseId);
            String body = submissionJson(enrollmentId, courseworkId, null);

            ResultActions result = postJson(PATH, body);

            // The POST response carries a finer value than the one stored,
            // so only the presence of the field is checked.
            result.andExpect(status().isCreated())
                    .andExpect(jsonPath("$.submittedAt").exists());
            getSubmission(idOf(result))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.submittedAt").exists());
        }

        @Test
        @DisplayName("unknown enrollment returns 404")
        void unknownEnrollmentReturns404() throws Exception {
            long courseworkId = createCoursework(createCourse());
            String body =
                    submissionJson(UNKNOWN_ID, courseworkId, SUBMITTED_AT);

            ResultActions result = postJson(PATH, body);

            result.andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message")
                            .value(ENROLLMENT_NOT_FOUND));
        }

        @Test
        @DisplayName("courseworkId that does not exist returns 404")
        void unknownCourseworkReturns404() throws Exception {
            long enrollmentId = createEnrollment(createCourse());
            String body =
                    submissionJson(enrollmentId, UNKNOWN_ID, SUBMITTED_AT);

            ResultActions result = postJson(PATH, body);

            result.andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message")
                            .value(COURSEWORK_NOT_FOUND));
        }

        @Test
        @DisplayName("coursework from another course returns 409")
        void courseworkFromOtherCourseReturns409() throws Exception {
            long enrollmentId = createEnrollment(createCourse());
            long otherWork = createCoursework(createCourse());
            String body =
                    submissionJson(enrollmentId, otherWork, SUBMITTED_AT);

            ResultActions result = postJson(PATH, body);

            result.andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value(WRONG_COURSE));
        }

        @Test
        @DisplayName("same enrollment and coursework twice returns 409, even "
                + "with another submittedAt")
        void duplicateReturns409() throws Exception {
            long courseId = createCourse();
            long enrollmentId = createEnrollment(courseId);
            long courseworkId = createCoursework(courseId);
            createSubmission(enrollmentId, courseworkId);
            String body = submissionJson(enrollmentId, courseworkId, LATER);

            ResultActions result = postJson(PATH, body);

            result.andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value(DUPLICATE));
        }

        @Test
        @DisplayName("missing courseworkId returns 400 naming that field")
        void missingCourseworkIdReturns400() throws Exception {
            long enrollmentId = createEnrollment(createCourse());
            String body = "{\"enrollmentId\": " + enrollmentId
                    + ", \"submittedAt\": \"" + SUBMITTED_AT + "\"}";

            ResultActions result = postJson(PATH, body);

            result.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(VALIDATION_FAILED))
                    .andExpect(jsonPath("$.fieldErrors.courseworkId")
                            .value(MUST_NOT_BE_NULL));
        }
    }

    /** Tests for GET /api/submissions, with and without filters. */
    @Nested
    @DisplayName("GET /api/submissions")
    class ListSubmissions {

        @Test
        @DisplayName("enrollmentId filter returns only that enrollment's "
                + "submissions")
        void enrollmentIdFilterReturnsOnlyThatEnrollment() throws Exception {
            long courseId = createCourse();
            long enrollmentA = createEnrollment(courseId);
            long enrollmentB = createEnrollment(courseId);
            long courseworkId = createCoursework(courseId);
            long byA = createSubmission(enrollmentA, courseworkId);
            long byB = createSubmission(enrollmentB, courseworkId);

            ResultActions result = mvc().perform(get(PATH)
                    .param("enrollmentId", String.valueOf(enrollmentA)));

            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$[*].id", hasItem(jsonInt(byA))))
                    .andExpect(jsonPath("$[*].id",
                            not(hasItem(jsonInt(byB)))))
                    .andExpect(jsonPath("$[*].enrollmentId",
                            everyItem(is(jsonInt(enrollmentA)))));
        }

        @Test
        @DisplayName("enrollmentId and courseworkId together must both "
                + "match")
        void bothFiltersAreCombinedWithAnd() throws Exception {
            long courseId = createCourse();
            long enrollmentId = createEnrollment(courseId);
            long courseworkId = createCoursework(courseId);
            long match = createSubmission(enrollmentId, courseworkId);
            long otherWork = createSubmission(enrollmentId,
                    createCoursework(courseId));
            long otherEnrollment = createSubmission(
                    createEnrollment(courseId), courseworkId);

            ResultActions result = mvc().perform(get(PATH)
                    .param("enrollmentId", String.valueOf(enrollmentId))
                    .param("courseworkId", String.valueOf(courseworkId)));

            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$[*].id", hasItem(jsonInt(match))))
                    .andExpect(jsonPath("$[*].id",
                            not(hasItem(jsonInt(otherWork)))))
                    .andExpect(jsonPath("$[*].id",
                            not(hasItem(jsonInt(otherEnrollment)))));
        }
    }

    /** Tests for GET /api/submissions/{id}, which reads one submission. */
    @Nested
    @DisplayName("GET /api/submissions/{id}")
    class GetById {

        @Test
        @DisplayName("existing submission returns 200 with its fields")
        void existingSubmissionReturns200() throws Exception {
            long courseId = createCourse();
            long enrollmentId = createEnrollment(courseId);
            long courseworkId = createCoursework(courseId);
            long id = createSubmission(enrollmentId, courseworkId);

            ResultActions result = getSubmission(id);

            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(id))
                    .andExpect(jsonPath("$.enrollmentId").value(enrollmentId))
                    .andExpect(jsonPath("$.courseworkId").value(courseworkId))
                    .andExpect(jsonPath("$.submittedAt").value(SUBMITTED_AT));
        }

        @Test
        @DisplayName("id that does not exist returns 404")
        void unknownIdReturns404() throws Exception {
            ResultActions result = getSubmission(UNKNOWN_ID);

            result.andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message")
                            .value(SUBMISSION_NOT_FOUND));
        }

        @Test
        @DisplayName("id that is not a number returns 400")
        void invalidIdReturns400() throws Exception {
            ResultActions result = mvc().perform(get(PATH + "/abc"));

            result.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message")
                            .value("Invalid value for id"));
        }
    }

    /** Tests for PUT /api/submissions/{id}, which replaces the fields. */
    @Nested
    @DisplayName("PUT /api/submissions/{id}")
    class Update {

        @Test
        @DisplayName("changing coursework and submittedAt returns 200 and is "
                + "stored")
        void changeCourseworkAndTimeReturns200() throws Exception {
            long courseId = createCourse();
            long enrollmentId = createEnrollment(courseId);
            long id = createSubmission(enrollmentId,
                    createCoursework(courseId));
            long newWork = createCoursework(courseId);

            ResultActions result = putJson(pathOf(id),
                    submissionJson(enrollmentId, newWork, LATER));

            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(id))
                    .andExpect(jsonPath("$.enrollmentId").value(enrollmentId))
                    .andExpect(jsonPath("$.courseworkId").value(newWork))
                    .andExpect(jsonPath("$.submittedAt").value(LATER));
            assertStored(id, enrollmentId, newWork, LATER);
        }

        @Test
        @DisplayName("omitted submittedAt keeps the stored value")
        void omittedSubmittedAtKeepsStoredValue() throws Exception {
            long courseId = createCourse();
            long enrollmentId = createEnrollment(courseId);
            long id = createSubmission(enrollmentId,
                    createCoursework(courseId));
            long newWork = createCoursework(courseId);

            ResultActions result = putJson(pathOf(id),
                    submissionJson(enrollmentId, newWork, null));

            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.courseworkId").value(newWork))
                    .andExpect(jsonPath("$.submittedAt").value(SUBMITTED_AT));
            assertStored(id, enrollmentId, newWork, SUBMITTED_AT);
        }

        @Test
        @DisplayName("unknown submission returns 404")
        void unknownSubmissionReturns404() throws Exception {
            long courseId = createCourse();
            String body = submissionJson(createEnrollment(courseId),
                    createCoursework(courseId), SUBMITTED_AT);

            ResultActions result = putJson(pathOf(UNKNOWN_ID), body);

            result.andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message")
                            .value(SUBMISSION_NOT_FOUND));
        }

        @Test
        @DisplayName("unknown enrollment returns 404 and changes nothing")
        void unknownEnrollmentReturns404() throws Exception {
            long courseId = createCourse();
            long enrollmentId = createEnrollment(courseId);
            long courseworkId = createCoursework(courseId);
            long id = createSubmission(enrollmentId, courseworkId);

            ResultActions result = putJson(pathOf(id),
                    submissionJson(UNKNOWN_ID, courseworkId, LATER));

            result.andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message")
                            .value(ENROLLMENT_NOT_FOUND));
            assertStored(id, enrollmentId, courseworkId, SUBMITTED_AT);
        }

        @Test
        @DisplayName("courseworkId that does not exist returns 404 and "
                + "changes nothing")
        void unknownCourseworkReturns404() throws Exception {
            long courseId = createCourse();
            long enrollmentId = createEnrollment(courseId);
            long courseworkId = createCoursework(courseId);
            long id = createSubmission(enrollmentId, courseworkId);

            ResultActions result = putJson(pathOf(id),
                    submissionJson(enrollmentId, UNKNOWN_ID, LATER));

            result.andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message")
                            .value(COURSEWORK_NOT_FOUND));
            assertStored(id, enrollmentId, courseworkId, SUBMITTED_AT);
        }

        @Test
        @DisplayName("coursework from another course returns 409 and "
                + "changes nothing")
        void courseworkFromOtherCourseReturns409() throws Exception {
            long courseId = createCourse();
            long enrollmentId = createEnrollment(courseId);
            long courseworkId = createCoursework(courseId);
            long id = createSubmission(enrollmentId, courseworkId);
            long otherWork = createCoursework(createCourse());

            ResultActions result = putJson(pathOf(id),
                    submissionJson(enrollmentId, otherWork, LATER));

            result.andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value(WRONG_COURSE));
            assertStored(id, enrollmentId, courseworkId, SUBMITTED_AT);
        }

        @Test
        @DisplayName("changing to another submission's enrollment and "
                + "coursework returns 409 and changes nothing")
        void duplicateReturns409() throws Exception {
            long courseId = createCourse();
            long enrollmentId = createEnrollment(courseId);
            long takenWork = createCoursework(courseId);
            createSubmission(enrollmentId, takenWork);
            long courseworkId = createCoursework(courseId);
            long id = createSubmission(enrollmentId, courseworkId);

            ResultActions result = putJson(pathOf(id),
                    submissionJson(enrollmentId, takenWork, LATER));

            result.andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value(DUPLICATE));
            assertStored(id, enrollmentId, courseworkId, SUBMITTED_AT);
        }
    }

    /** Tests for DELETE /api/submissions/{id}. */
    @Nested
    @DisplayName("DELETE /api/submissions/{id}")
    class Delete {

        @Test
        @DisplayName("existing submission returns 204, then GET returns 404")
        void existingSubmissionReturns204() throws Exception {
            long courseId = createCourse();
            long id = createSubmission(createEnrollment(courseId),
                    createCoursework(courseId));

            ResultActions result = mvc().perform(delete(pathOf(id)));

            result.andExpect(status().isNoContent());
            getSubmission(id)
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message")
                            .value(SUBMISSION_NOT_FOUND));
        }

        @Test
        @DisplayName("unknown submission returns 404")
        void unknownSubmissionReturns404() throws Exception {
            ResultActions result = mvc().perform(delete(pathOf(UNKNOWN_ID)));

            result.andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message")
                            .value(SUBMISSION_NOT_FOUND));
        }
    }
}
