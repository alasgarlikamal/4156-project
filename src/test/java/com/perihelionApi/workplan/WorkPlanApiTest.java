/*
    AI usage: Work Plan API Tests: Opus 5.5, ~114K tokens
 */
package com.perihelionApi.workplan;

import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;

import com.jayway.jsonpath.JsonPath;
import com.perihelionApi.ApiTestSupport;

/**
 * Full-application API tests for {@code /api/workplans}, written from the
 * API description, with nothing mocked.
 *
 * <p>Each test creates its own courses, enrollments and work plans over
 * HTTP, so no test depends on another test, on the order the tests run in,
 * or on the rows already in the database.
 */
@DisplayName("Work Plans API")
class WorkPlanApiTest extends ApiTestSupport {

    /** Base path of the work plan endpoints. */
    private static final String PATH = "/api/workplans";

    /** Error message when the work plan does not exist. */
    private static final String WORK_PLAN_NOT_FOUND = "Work plan not found";

    /** Error message when the enrollment does not exist. */
    private static final String ENROLLMENT_NOT_FOUND = "Enrollment not found";

    /** Error message when a delete is blocked by a task. */
    private static final String HAS_TASKS =
            "Work plan cannot be deleted while tasks exist";

    /** Error message for a bean-validation failure. */
    private static final String VALIDATION_FAILED = "Validation failed";

    /** Error message for an id path value that is not a number. */
    private static final String INVALID_ID = "Invalid value for id";

    /** The message of the {@code rangeValid} cross-field check. */
    private static final String RANGE_INVALID =
            "rangeStart must be earlier than rangeEnd";

    /** First day of the default plan range, a Monday. */
    private static final LocalDate START = LocalDate.of(2026, 11, 2);

    /** Last day of the default plan range, the Sunday after START. */
    private static final LocalDate END = LocalDate.of(2026, 11, 8);

    /** The hours per day that {@code createWorkPlan} sends. */
    private static final double DEFAULT_HOURS = 4.0;

    /** Allowed difference when comparing hours as numbers. */
    private static final double TOLERANCE = 1e-9;

    /**
     * Returns the path of one work plan.
     *
     * @param id the work plan id
     * @return the path, such as {@code /api/workplans/7}
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
     * Returns a matcher that compares availableHoursPerDay as a number, so
     * that {@code 4}, {@code 4.0} and {@code 4.00} all match 4.
     *
     * @param expected the expected hours
     * @return the matcher
     */
    private static ResultMatcher hoursAre(final double expected) {
        return jsonPath("$.availableHoursPerDay")
                .value(closeTo(expected, TOLERANCE), Double.class);
    }

    /**
     * Sends a GET request for one work plan.
     *
     * @param id the work plan id
     * @return the result, for assertions
     * @throws Exception if the request cannot be sent
     */
    private ResultActions getWorkPlan(final long id) throws Exception {
        return mvc().perform(get(pathOf(id)));
    }

    /**
     * Reads a work plan's createdAt with a GET request, so that the value
     * comes from the database.
     *
     * @param id the work plan id
     * @return the createdAt text
     * @throws Exception if the request cannot be sent or does not return 200
     */
    private String createdAtOf(final long id) throws Exception {
        String body = getWorkPlan(id).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.createdAt");
    }

    /**
     * Checks with a GET request that a work plan is stored with the given
     * fields and has a createdAt.
     *
     * @param id the work plan id
     * @param enrollmentId the expected enrollment id
     * @param rangeStart the expected first day
     * @param rangeEnd the expected last day
     * @param hours the expected hours per day
     * @throws Exception if the request cannot be sent or a check fails
     */
    private void assertStored(final long id, final long enrollmentId,
            final LocalDate rangeStart, final LocalDate rangeEnd,
            final double hours) throws Exception {
        getWorkPlan(id)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.enrollmentId").value(enrollmentId))
                .andExpect(jsonPath("$.rangeStart")
                        .value(rangeStart.toString()))
                .andExpect(jsonPath("$.rangeEnd").value(rangeEnd.toString()))
                .andExpect(hoursAre(hours))
                .andExpect(jsonPath("$.createdAt").exists());
    }

    /** Tests for POST /api/workplans, which creates a work plan. */
    @Nested
    @DisplayName("POST /api/workplans")
    class Create {

        @Test
        @DisplayName("valid request returns 201 with all fields and a "
                + "createdAt")
        void validRequestReturns201() throws Exception {
            long enrollmentId = createEnrollment(createCourse());

            ResultActions result = postJson(PATH,
                    workPlanJson(enrollmentId, START, END, "4"));

            // createdAt is stored at millisecond precision but returned
            // here with microseconds, so only its presence is checked.
            result.andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").isNumber())
                    .andExpect(jsonPath("$.enrollmentId").value(enrollmentId))
                    .andExpect(jsonPath("$.rangeStart").value(START.toString()))
                    .andExpect(jsonPath("$.rangeEnd").value(END.toString()))
                    .andExpect(hoursAre(DEFAULT_HOURS))
                    .andExpect(jsonPath("$.createdAt").exists());
            assertStored(idOf(result), enrollmentId, START, END,
                    DEFAULT_HOURS);
        }

        @Test
        @DisplayName("one-day range (rangeEnd the day after rangeStart) "
                + "returns 201")
        void oneDayRangeReturns201() throws Exception {
            long enrollmentId = createEnrollment(createCourse());
            LocalDate nextDay = START.plusDays(1);

            ResultActions result = postJson(PATH,
                    workPlanJson(enrollmentId, START, nextDay, "4"));

            result.andExpect(status().isCreated())
                    .andExpect(jsonPath("$.rangeStart").value(START.toString()))
                    .andExpect(jsonPath("$.rangeEnd")
                            .value(nextDay.toString()));
            assertStored(idOf(result), enrollmentId, START, nextDay,
                    DEFAULT_HOURS);
        }

        @Test
        @DisplayName("unknown enrollment returns 404")
        void unknownEnrollmentReturns404() throws Exception {
            String body = workPlanJson(UNKNOWN_ID, START, END, "4");

            ResultActions result = postJson(PATH, body);

            result.andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message")
                            .value(ENROLLMENT_NOT_FOUND));
        }

        @ParameterizedTest(name = "rangeEnd = rangeStart + {0} days")
        @ValueSource(longs = {0, -1})
        @DisplayName("rangeStart equal to or after rangeEnd returns 400 "
                + "under rangeValid")
        void rangeNotIncreasingReturns400(final long days) throws Exception {
            long enrollmentId = createEnrollment(createCourse());
            LocalDate rangeEnd = START.plusDays(days);

            ResultActions result = postJson(PATH,
                    workPlanJson(enrollmentId, START, rangeEnd, "4"));

            result.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(VALIDATION_FAILED))
                    .andExpect(jsonPath("$.fieldErrors.rangeValid")
                            .value(RANGE_INVALID));
        }
    }

    /** Tests for GET /api/workplans with the enrollmentId filter. */
    @Nested
    @DisplayName("GET /api/workplans")
    class ListWorkPlans {

        @Test
        @DisplayName("enrollmentId filter returns only that enrollment's "
                + "work plans")
        void enrollmentIdFilterReturnsOnlyThatEnrollment() throws Exception {
            long courseId = createCourse();
            long enrollmentA = createEnrollment(courseId);
            long enrollmentB = createEnrollment(courseId);
            long inA = createWorkPlan(enrollmentA, START, END);
            long inB = createWorkPlan(enrollmentB, START, END);

            ResultActions result = mvc().perform(get(PATH)
                    .param("enrollmentId", String.valueOf(enrollmentA)));

            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$[*].id", hasItem(jsonInt(inA))))
                    .andExpect(jsonPath("$[*].id",
                            not(hasItem(jsonInt(inB)))))
                    .andExpect(jsonPath("$[*].enrollmentId",
                            everyItem(is(jsonInt(enrollmentA)))));
        }
    }

    /** Tests for GET /api/workplans/{id}, which reads one work plan. */
    @Nested
    @DisplayName("GET /api/workplans/{id}")
    class GetById {

        @Test
        @DisplayName("existing work plan returns 200 with its fields")
        void existingWorkPlanReturns200() throws Exception {
            long enrollmentId = createEnrollment(createCourse());
            long id = createWorkPlan(enrollmentId, START, END);

            ResultActions result = getWorkPlan(id);

            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(id))
                    .andExpect(jsonPath("$.enrollmentId").value(enrollmentId))
                    .andExpect(jsonPath("$.rangeStart").value(START.toString()))
                    .andExpect(jsonPath("$.rangeEnd").value(END.toString()))
                    .andExpect(hoursAre(DEFAULT_HOURS))
                    .andExpect(jsonPath("$.createdAt").exists());
        }

        @Test
        @DisplayName("id that does not exist returns 404")
        void unknownIdReturns404() throws Exception {
            ResultActions result = getWorkPlan(UNKNOWN_ID);

            result.andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message")
                            .value(WORK_PLAN_NOT_FOUND));
        }

        @Test
        @DisplayName("id that is not a number returns 400")
        void invalidIdReturns400() throws Exception {
            ResultActions result = mvc().perform(get(PATH + "/abc"));

            result.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(INVALID_ID));
        }
    }

    /** Tests for PUT /api/workplans/{id}, which replaces every field. */
    @Nested
    @DisplayName("PUT /api/workplans/{id}")
    class Update {

        @Test
        @DisplayName("new range and hours return 200 and are stored")
        void newRangeAndHoursReturn200() throws Exception {
            long enrollmentId = createEnrollment(createCourse());
            long id = createWorkPlan(enrollmentId, START, END);
            LocalDate newStart = START.plusDays(7);
            LocalDate newEnd = END.plusDays(14);

            ResultActions result = putJson(pathOf(id),
                    workPlanJson(enrollmentId, newStart, newEnd, "6.5"));

            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(id))
                    .andExpect(jsonPath("$.enrollmentId").value(enrollmentId))
                    .andExpect(jsonPath("$.rangeStart")
                            .value(newStart.toString()))
                    .andExpect(jsonPath("$.rangeEnd").value(newEnd.toString()))
                    .andExpect(hoursAre(6.5));
            assertStored(id, enrollmentId, newStart, newEnd, 6.5);
        }

        @Test
        @DisplayName("createdAt stays the same after an update")
        void createdAtIsKept() throws Exception {
            long enrollmentId = createEnrollment(createCourse());
            long id = createWorkPlan(enrollmentId, START, END);
            String createdAt = createdAtOf(id);
            String body = workPlanJson(enrollmentId, START.plusDays(7),
                    END.plusDays(7), "6");

            ResultActions result = putJson(pathOf(id), body);

            // Both values come from the database, never from the POST.
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.createdAt").value(createdAt));
            getWorkPlan(id)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.createdAt").value(createdAt));
        }

        @Test
        @DisplayName("unknown work plan returns 404")
        void unknownWorkPlanReturns404() throws Exception {
            long enrollmentId = createEnrollment(createCourse());
            String body = workPlanJson(enrollmentId, START, END, "4");

            ResultActions result = putJson(pathOf(UNKNOWN_ID), body);

            result.andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message")
                            .value(WORK_PLAN_NOT_FOUND));
        }

        @Test
        @DisplayName("unknown enrollment returns 404 and changes nothing")
        void unknownEnrollmentReturns404() throws Exception {
            long enrollmentId = createEnrollment(createCourse());
            long id = createWorkPlan(enrollmentId, START, END);
            String body = workPlanJson(UNKNOWN_ID, START.plusDays(7),
                    END.plusDays(7), "6");

            ResultActions result = putJson(pathOf(id), body);

            result.andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message")
                            .value(ENROLLMENT_NOT_FOUND));
            assertStored(id, enrollmentId, START, END, DEFAULT_HOURS);
        }
    }

    /** Tests for DELETE /api/workplans/{id}. */
    @Nested
    @DisplayName("DELETE /api/workplans/{id}")
    class Delete {

        @Test
        @DisplayName("existing work plan returns 204, then GET returns 404")
        void existingWorkPlanReturns204() throws Exception {
            long id = createWorkPlan(createEnrollment(createCourse()),
                    START, END);

            ResultActions result = mvc().perform(delete(pathOf(id)));

            result.andExpect(status().isNoContent());
            getWorkPlan(id)
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message")
                            .value(WORK_PLAN_NOT_FOUND));
        }

        @Test
        @DisplayName("unknown work plan returns 404")
        void unknownWorkPlanReturns404() throws Exception {
            ResultActions result = mvc().perform(delete(pathOf(UNKNOWN_ID)));

            result.andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message")
                            .value(WORK_PLAN_NOT_FOUND));
        }

        @Test
        @DisplayName("work plan with a task returns 409 and is kept")
        void withTaskReturns409() throws Exception {
            long courseId = createCourse();
            long enrollmentId = createEnrollment(courseId);
            long id = createWorkPlan(enrollmentId, START, END);
            createTask(id, createCoursework(courseId), enrollmentId,
                    START.plusDays(2));

            ResultActions result = mvc().perform(delete(pathOf(id)));

            result.andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value(HAS_TASKS));
            assertStored(id, enrollmentId, START, END, DEFAULT_HOURS);
        }
    }
}
