/*
    AI usage: Enrollment API Tests: Opus 5.5, ~117K tokens
 */
package com.perihelionApi.enrollment;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import com.perihelionApi.ApiTestSupport;

/**
 * Full-application API tests for {@code /api/enrollments}, written from the
 * API description, with nothing mocked.
 *
 * <p>Each test creates its own courses, learners and enrollments over HTTP,
 * so no test depends on another test, on the order the tests run in, or on
 * the rows already in the database.
 */
@DisplayName("Enrollments API")
class EnrollmentApiTest extends ApiTestSupport {

    /** Base path of the enrollment endpoints. */
    private static final String PATH = "/api/enrollments";

    /** Error message when the course does not exist. */
    private static final String COURSE_NOT_FOUND = "Course not found";

    /** Error message when the enrollment does not exist. */
    private static final String ENROLLMENT_NOT_FOUND = "Enrollment not found";

    /** Error message when the learner is already enrolled in the course. */
    private static final String DUPLICATE =
            "An enrollment already exists for this course and learner";

    /** Error message when a delete is blocked by a submission. */
    private static final String HAS_SUBMISSIONS =
            "Enrollment cannot be deleted while submissions exist";

    /** Error message when a delete is blocked by a work plan. */
    private static final String HAS_WORK_PLANS =
            "Enrollment cannot be deleted while work plans exist";

    /** Error message for a bean-validation failure. */
    private static final String VALIDATION_FAILED = "Validation failed";

    /** The only message a {@code @NotNull} field can give. */
    private static final String MUST_NOT_BE_NULL = "must not be null";

    /**
     * Returns the path of one enrollment.
     *
     * @param id the enrollment id
     * @return the path, such as {@code /api/enrollments/7}
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
     * Sends a GET request for one enrollment.
     *
     * @param id the enrollment id
     * @return the result, for assertions
     * @throws Exception if the request cannot be sent
     */
    private ResultActions getEnrollment(final long id) throws Exception {
        return mvc().perform(get(pathOf(id)));
    }

    /**
     * Checks with a GET request that an enrollment is stored with the given
     * course and learner.
     *
     * @param id the enrollment id
     * @param courseId the expected course id
     * @param learner the expected learner reference
     * @throws Exception if the request cannot be sent or a check fails
     */
    private void assertStored(final long id, final long courseId,
            final UUID learner) throws Exception {
        getEnrollment(id)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.courseId").value(courseId))
                .andExpect(jsonPath("$.learnerRef")
                        .value(learner.toString()));
    }

    /** Tests for POST /api/enrollments, which enrolls a learner. */
    @Nested
    @DisplayName("POST /api/enrollments")
    class Create {

        @Test
        @DisplayName("valid request returns 201 with id, courseId and "
                + "learnerRef")
        void validRequestReturns201() throws Exception {
            long courseId = createCourse();
            UUID learner = UUID.randomUUID();

            ResultActions result =
                    postJson(PATH, enrollmentJson(courseId, learner));

            result.andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").isNumber())
                    .andExpect(jsonPath("$.courseId").value(courseId))
                    .andExpect(jsonPath("$.learnerRef")
                            .value(learner.toString()));
            assertStored(idOf(result), courseId, learner);
        }

        @Test
        @DisplayName("missing learnerRef returns 400 naming that field")
        void missingLearnerRefReturns400() throws Exception {
            String body = "{\"courseId\": " + createCourse() + "}";

            ResultActions result = postJson(PATH, body);

            result.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(VALIDATION_FAILED))
                    .andExpect(jsonPath("$.fieldErrors.learnerRef")
                            .value(MUST_NOT_BE_NULL));
        }

        @Test
        @DisplayName("unknown course returns 404")
        void unknownCourseReturns404() throws Exception {
            String body = enrollmentJson(UNKNOWN_ID, UUID.randomUUID());

            ResultActions result = postJson(PATH, body);

            result.andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value(COURSE_NOT_FOUND));
        }

        @Test
        @DisplayName("same course and learner twice returns 409")
        void duplicateReturns409() throws Exception {
            long courseId = createCourse();
            UUID learner = UUID.randomUUID();
            createEnrollment(courseId, learner);

            ResultActions result =
                    postJson(PATH, enrollmentJson(courseId, learner));

            result.andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value(DUPLICATE));
        }
    }

    /** Tests for GET /api/enrollments, with and without filters. */
    @Nested
    @DisplayName("GET /api/enrollments")
    class ListEnrollments {

        @Test
        @DisplayName("courseId filter returns only that course's enrollments")
        void courseIdFilterReturnsOnlyThatCourse() throws Exception {
            long courseA = createCourse();
            long courseB = createCourse();
            long inA = createEnrollment(courseA);
            long inB = createEnrollment(courseB);

            ResultActions result = mvc().perform(get(PATH)
                    .param("courseId", String.valueOf(courseA)));

            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$[*].id", hasItem(jsonInt(inA))))
                    .andExpect(jsonPath("$[*].id",
                            not(hasItem(jsonInt(inB)))))
                    .andExpect(jsonPath("$[*].courseId",
                            everyItem(is(jsonInt(courseA)))));
        }

        @Test
        @DisplayName("courseId and learnerRef together must both match")
        void bothFiltersAreCombinedWithAnd() throws Exception {
            UUID learner = UUID.randomUUID();
            long course = createCourse();
            long match = createEnrollment(course, learner);
            long otherCourse = createEnrollment(createCourse(), learner);
            long otherLearner = createEnrollment(course);

            ResultActions result = mvc().perform(get(PATH)
                    .param("courseId", String.valueOf(course))
                    .param("learnerRef", learner.toString()));

            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$[*].id", hasItem(jsonInt(match))))
                    .andExpect(jsonPath("$[*].id",
                            not(hasItem(jsonInt(otherCourse)))))
                    .andExpect(jsonPath("$[*].id",
                            not(hasItem(jsonInt(otherLearner)))));
        }
    }

    /** Tests for GET /api/enrollments/{id}, which reads one enrollment. */
    @Nested
    @DisplayName("GET /api/enrollments/{id}")
    class GetById {

        @Test
        @DisplayName("existing enrollment returns 200 with its fields")
        void existingEnrollmentReturns200() throws Exception {
            long courseId = createCourse();
            UUID learner = UUID.randomUUID();
            long id = createEnrollment(courseId, learner);

            ResultActions result = getEnrollment(id);

            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(id))
                    .andExpect(jsonPath("$.courseId").value(courseId))
                    .andExpect(jsonPath("$.learnerRef")
                            .value(learner.toString()));
        }

        @Test
        @DisplayName("id that does not exist returns 404")
        void unknownIdReturns404() throws Exception {
            ResultActions result = getEnrollment(UNKNOWN_ID);

            result.andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message")
                            .value(ENROLLMENT_NOT_FOUND));
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

    /** Tests for PUT /api/enrollments/{id}, which replaces both fields. */
    @Nested
    @DisplayName("PUT /api/enrollments/{id}")
    class Update {

        @Test
        @DisplayName("moving to another course returns 200 and is stored")
        void moveToAnotherCourseReturns200() throws Exception {
            UUID learner = UUID.randomUUID();
            long id = createEnrollment(createCourse(), learner);
            long newCourse = createCourse();

            ResultActions result =
                    putJson(pathOf(id), enrollmentJson(newCourse, learner));

            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(id))
                    .andExpect(jsonPath("$.courseId").value(newCourse))
                    .andExpect(jsonPath("$.learnerRef")
                            .value(learner.toString()));
            assertStored(id, newCourse, learner);
        }

        @Test
        @DisplayName("unknown enrollment returns 404")
        void unknownEnrollmentReturns404() throws Exception {
            String body = enrollmentJson(createCourse(), UUID.randomUUID());

            ResultActions result = putJson(pathOf(UNKNOWN_ID), body);

            result.andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message")
                            .value(ENROLLMENT_NOT_FOUND));
        }

        @Test
        @DisplayName("unknown course returns 404 and changes nothing")
        void unknownCourseReturns404() throws Exception {
            long courseId = createCourse();
            UUID learner = UUID.randomUUID();
            long id = createEnrollment(courseId, learner);

            ResultActions result =
                    putJson(pathOf(id), enrollmentJson(UNKNOWN_ID, learner));

            result.andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value(COURSE_NOT_FOUND));
            assertStored(id, courseId, learner);
        }

        @Test
        @DisplayName("moving to a course the learner is already in returns "
                + "409 and changes nothing")
        void duplicateReturns409() throws Exception {
            UUID learner = UUID.randomUUID();
            long takenCourse = createCourse();
            createEnrollment(takenCourse, learner);
            long courseId = createCourse();
            long id = createEnrollment(courseId, learner);

            ResultActions result =
                    putJson(pathOf(id), enrollmentJson(takenCourse, learner));

            result.andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value(DUPLICATE));
            assertStored(id, courseId, learner);
        }
    }

    /** Tests for DELETE /api/enrollments/{id}. */
    @Nested
    @DisplayName("DELETE /api/enrollments/{id}")
    class Delete {

        @Test
        @DisplayName("existing enrollment returns 204, then GET returns 404")
        void existingEnrollmentReturns204() throws Exception {
            long id = createEnrollment(createCourse());

            ResultActions result = mvc().perform(delete(pathOf(id)));

            result.andExpect(status().isNoContent());
            getEnrollment(id).andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("unknown enrollment returns 404")
        void unknownEnrollmentReturns404() throws Exception {
            ResultActions result = mvc().perform(delete(pathOf(UNKNOWN_ID)));

            result.andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message")
                            .value(ENROLLMENT_NOT_FOUND));
        }

        @Test
        @DisplayName("enrollment with a submission returns 409 and is kept")
        void withSubmissionReturns409() throws Exception {
            long courseId = createCourse();
            UUID learner = UUID.randomUUID();
            long id = createEnrollment(courseId, learner);
            createSubmission(id, createCoursework(courseId));

            ResultActions result = mvc().perform(delete(pathOf(id)));

            result.andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value(HAS_SUBMISSIONS));
            assertStored(id, courseId, learner);
        }

        @Test
        @DisplayName("enrollment with a work plan returns 409 and is kept")
        void withWorkPlanReturns409() throws Exception {
            long courseId = createCourse();
            UUID learner = UUID.randomUUID();
            long id = createEnrollment(courseId, learner);
            createWorkPlan(id, LocalDate.of(2026, 11, 2),
                    LocalDate.of(2026, 11, 8));

            ResultActions result = mvc().perform(delete(pathOf(id)));

            result.andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value(HAS_WORK_PLANS));
            assertStored(id, courseId, learner);
        }
    }
}
