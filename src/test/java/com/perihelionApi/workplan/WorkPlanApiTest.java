/*
    AI usage: Work Plan API Tests: Opus 5.5, ~114K tokens
 */
package com.perihelionApi.workplan;

import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.StringJoiner;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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

    /** Error message for a body that cannot be read. */
    private static final String MALFORMED_BODY = "Malformed request body";

    /** Error message for an id path value that is not a number. */
    private static final String INVALID_ID = "Invalid value for id";

    /** The only message a {@code @NotNull} field can give. */
    private static final String MUST_NOT_BE_NULL = "must not be null";

    /** The message of the {@code rangeValid} cross-field check. */
    private static final String RANGE_INVALID =
            "rangeStart must be earlier than rangeEnd";

    /** First day of the default plan range, a Monday. */
    private static final LocalDate START = LocalDate.of(2026, 11, 2);

    /** Last day of the default plan range, the Sunday after START. */
    private static final LocalDate END = LocalDate.of(2026, 11, 8);

    /** The hours per day that {@code createWorkPlan} sends. */
    private static final double DEFAULT_HOURS = 4.0;

    /** A createdAt a client might send; the server must not keep it. */
    private static final String SENT_CREATED_AT = "2000-01-01T00:00:00Z";

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
     * Returns the fields of a valid work plan body, from START to END with
     * 4 hours per day. Each value is already written as JSON.
     *
     * @param enrollmentId the enrollment id
     * @return the fields in body order, which the caller may change
     */
    private static Map<String, String> validFields(final long enrollmentId) {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("enrollmentId", String.valueOf(enrollmentId));
        fields.put("rangeStart", "\"" + START + "\"");
        fields.put("rangeEnd", "\"" + END + "\"");
        fields.put("availableHoursPerDay", "4");
        return fields;
    }

    /**
     * Writes fields as a JSON object.
     *
     * @param fields the field names and their JSON values
     * @return the JSON body
     */
    private static String toJson(final Map<String, String> fields) {
        StringJoiner json = new StringJoiner(", ", "{", "}");
        for (Map.Entry<String, String> field : fields.entrySet()) {
            json.add("\"" + field.getKey() + "\": " + field.getValue());
        }
        return json.toString();
    }

    /**
     * Builds a valid work plan body, then sets one field to a raw JSON
     * value, adding the field if it is not already there.
     *
     * @param enrollmentId the enrollment id
     * @param field the field to set
     * @param rawValue the value written as JSON, such as {@code "\"x\""}
     * @return the JSON body
     */
    private static String workPlanJsonWith(final long enrollmentId,
            final String field, final String rawValue) {
        Map<String, String> fields = validFields(enrollmentId);
        fields.put(field, rawValue);
        return toJson(fields);
    }

    /**
     * Builds a valid work plan body that leaves out one field.
     *
     * @param field the field to leave out
     * @param enrollmentId the enrollment id to send when it is kept
     * @return the JSON body
     */
    private static String workPlanJsonWithout(final String field,
            final long enrollmentId) {
        Map<String, String> fields = validFields(enrollmentId);
        fields.remove(field);
        return toJson(fields);
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

        @ParameterizedTest(name = "availableHoursPerDay {0}")
        @ValueSource(strings = {"0", "0.0", "24", "24.0"})
        @DisplayName("availableHoursPerDay at the bounds 0 and 24 returns "
                + "201")
        void hoursAtBoundsReturn201(final String hours) throws Exception {
            long enrollmentId = createEnrollment(createCourse());

            ResultActions result = postJson(PATH,
                    workPlanJson(enrollmentId, START, END, hours));

            result.andExpect(status().isCreated())
                    .andExpect(hoursAre(Double.parseDouble(hours)));
            assertStored(idOf(result), enrollmentId, START, END,
                    Double.parseDouble(hours));
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
        @DisplayName("createdAt sent by the client is ignored")
        void clientCreatedAtIsIgnored() throws Exception {
            // Assumption: the server sets its own createdAt and does not
            // reject the request; the client value is simply dropped.
            long enrollmentId = createEnrollment(createCourse());
            String body = workPlanJsonWith(enrollmentId, "createdAt",
                    "\"" + SENT_CREATED_AT + "\"");

            ResultActions result = postJson(PATH, body);

            result.andExpect(status().isCreated())
                    .andExpect(jsonPath("$.createdAt").exists())
                    .andExpect(jsonPath("$.createdAt")
                            .value(not(SENT_CREATED_AT)));
            getWorkPlan(idOf(result))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.createdAt").exists())
                    .andExpect(jsonPath("$.createdAt")
                            .value(not(SENT_CREATED_AT)));
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

        @ParameterizedTest(name = "availableHoursPerDay {0}")
        @CsvSource({
            "-0.01, must be greater than or equal to 0.0",
            "24.01, must be less than or equal to 24.0"
        })
        @DisplayName("availableHoursPerDay just outside 0 to 24 returns 400 "
                + "naming that field")
        void hoursOutOfRangeReturns400(final String hours,
                final String message) throws Exception {
            long enrollmentId = createEnrollment(createCourse());

            ResultActions result = postJson(PATH,
                    workPlanJson(enrollmentId, START, END, hours));

            result.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(VALIDATION_FAILED))
                    .andExpect(jsonPath("$.fieldErrors.availableHoursPerDay")
                            .value(message));
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

        @ParameterizedTest(name = "without {0}")
        @ValueSource(strings = {"enrollmentId", "rangeStart", "rangeEnd",
            "availableHoursPerDay"})
        @DisplayName("missing required field returns 400 naming that field")
        void missingFieldReturns400(final String field) throws Exception {
            long enrollmentId = createEnrollment(createCourse());

            ResultActions result =
                    postJson(PATH, workPlanJsonWithout(field, enrollmentId));

            result.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(VALIDATION_FAILED))
                    .andExpect(jsonPath("$.fieldErrors." + field)
                            .value(MUST_NOT_BE_NULL));
        }

        @Test
        @DisplayName("empty object returns 400 naming all four fields")
        void emptyObjectReturns400() throws Exception {
            String body = "{}";

            ResultActions result = postJson(PATH, body);

            result.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(VALIDATION_FAILED))
                    .andExpect(jsonPath("$.fieldErrors.enrollmentId")
                            .value(MUST_NOT_BE_NULL))
                    .andExpect(jsonPath("$.fieldErrors.rangeStart")
                            .value(MUST_NOT_BE_NULL))
                    .andExpect(jsonPath("$.fieldErrors.rangeEnd")
                            .value(MUST_NOT_BE_NULL))
                    .andExpect(jsonPath("$.fieldErrors.availableHoursPerDay")
                            .value(MUST_NOT_BE_NULL));
        }

        @ParameterizedTest(name = "enrollmentId {0}")
        @ValueSource(longs = {0, -1})
        @DisplayName("enrollmentId of 0 or less returns 400 naming "
                + "enrollmentId")
        void nonPositiveEnrollmentIdReturns400(final long enrollmentId)
                throws Exception {
            String body = workPlanJson(enrollmentId, START, END, "4");

            ResultActions result = postJson(PATH, body);

            result.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(VALIDATION_FAILED))
                    .andExpect(jsonPath("$.fieldErrors.enrollmentId")
                            .value("must be greater than 0"));
        }

        @ParameterizedTest(name = "{0} \"yesterday\"")
        @ValueSource(strings = {"rangeStart", "rangeEnd"})
        @DisplayName("date that is not yyyy-MM-dd returns 400 malformed")
        void unreadableDateReturns400(final String field) throws Exception {
            long enrollmentId = createEnrollment(createCourse());
            String body =
                    workPlanJsonWith(enrollmentId, field, "\"yesterday\"");

            ResultActions result = postJson(PATH, body);

            result.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(MALFORMED_BODY));
        }

        @Test
        @DisplayName("malformed JSON returns 400")
        void malformedJsonReturns400() throws Exception {
            String body = "{\"enrollmentId\": 1, \"rangeStart\": ";

            ResultActions result = postJson(PATH, body);

            result.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(MALFORMED_BODY));
        }
    }

    /** Tests for GET /api/workplans, with and without a filter. */
    @Nested
    @DisplayName("GET /api/workplans")
    class ListWorkPlans {

        @Test
        @DisplayName("unfiltered list contains a new work plan")
        void unfilteredListContainsNewWorkPlan() throws Exception {
            long id = createWorkPlan(createEnrollment(createCourse()),
                    START, END);

            ResultActions result = mvc().perform(get(PATH));

            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$[*].id", hasItem(jsonInt(id))));
        }

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

        @Test
        @DisplayName("list is ordered by rangeStart, then by id")
        void listIsOrderedByRangeStartThenId() throws Exception {
            // Assumption: one enrollment may hold several work plans, even
            // with overlapping ranges, and ids are given out in increasing
            // order. The plans are created out of order on purpose; the two
            // that share a rangeStart are told apart only by id (the one
            // created first has the later rangeEnd).
            long enrollmentId = createEnrollment(createCourse());
            long late = createWorkPlan(enrollmentId,
                    LocalDate.of(2026, 11, 20), LocalDate.of(2026, 11, 25));
            long early = createWorkPlan(enrollmentId,
                    LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 5));
            long lateSameStart = createWorkPlan(enrollmentId,
                    LocalDate.of(2026, 11, 20), LocalDate.of(2026, 11, 22));
            long middle = createWorkPlan(enrollmentId,
                    LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 12));

            ResultActions result = mvc().perform(get(PATH)
                    .param("enrollmentId", String.valueOf(enrollmentId)));

            // The enrollment is new, so it holds only these four plans.
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$[*].id", contains(jsonInt(early),
                            jsonInt(middle), jsonInt(late),
                            jsonInt(lateSameStart))));
        }

        @Test
        @DisplayName("unknown enrollmentId returns an empty list")
        void unknownEnrollmentReturnsEmptyList() throws Exception {
            String unknownEnrollment = String.valueOf(UNKNOWN_ID);

            ResultActions result = mvc().perform(get(PATH)
                    .param("enrollmentId", unknownEnrollment));

            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$").isEmpty());
        }

        @Test
        @DisplayName("enrollment with no work plans returns an empty list")
        void enrollmentWithoutPlansReturnsEmptyList() throws Exception {
            long enrollmentId = createEnrollment(createCourse());

            ResultActions result = mvc().perform(get(PATH)
                    .param("enrollmentId", String.valueOf(enrollmentId)));

            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$").isEmpty());
        }

        @Test
        @DisplayName("enrollmentId that is not a number returns 400")
        void invalidEnrollmentIdReturns400() throws Exception {
            ResultActions result = mvc().perform(get(PATH)
                    .param("enrollmentId", "abc"));

            result.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message")
                            .value("Invalid value for enrollmentId"));
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

        @ParameterizedTest(name = "id {0}")
        @ValueSource(longs = {UNKNOWN_ID, 0, -1})
        @DisplayName("id that does not exist returns 404")
        void unknownIdReturns404(final long id) throws Exception {
            // Assumption: an id of 0 or less is simply not found, like any
            // other id that was never created.
            ResultActions result = getWorkPlan(id);

            result.andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message")
                            .value(WORK_PLAN_NOT_FOUND));
        }

        @ParameterizedTest(name = "id \"{0}\"")
        @ValueSource(strings = {"abc", "1.5", "9223372036854775808"})
        @DisplayName("id that is not a whole number in range returns 400")
        void invalidIdReturns400(final String id) throws Exception {
            // Assumption: "1.5" and a value one above Long.MAX_VALUE are path
            // values of the wrong type, like "abc".
            ResultActions result = mvc().perform(get(PATH + "/" + id));

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
        @DisplayName("createdAt sent in the body is ignored")
        void sentCreatedAtIsIgnored() throws Exception {
            // Assumption: "a value sent by the client is ignored" covers
            // PUT as well as POST.
            long enrollmentId = createEnrollment(createCourse());
            long id = createWorkPlan(enrollmentId, START, END);
            String createdAt = createdAtOf(id);
            String body = workPlanJsonWith(enrollmentId, "createdAt",
                    "\"" + SENT_CREATED_AT + "\"");

            ResultActions result = putJson(pathOf(id), body);

            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.createdAt").value(createdAt));
            getWorkPlan(id)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.createdAt").value(createdAt));
        }

        @Test
        @DisplayName("moving to another enrollment returns 200 and is stored")
        void moveToAnotherEnrollmentReturns200() throws Exception {
            // Assumption: the new enrollment may be in a different course;
            // the description puts no rule on it.
            long id = createWorkPlan(createEnrollment(createCourse()),
                    START, END);
            long newEnrollment = createEnrollment(createCourse());

            ResultActions result = putJson(pathOf(id),
                    workPlanJson(newEnrollment, START, END, "4"));

            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(id))
                    .andExpect(jsonPath("$.enrollmentId")
                            .value(newEnrollment));
            assertStored(id, newEnrollment, START, END, DEFAULT_HOURS);
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

        @Test
        @DisplayName("unknown work plan is reported before unknown "
                + "enrollment")
        void unknownWorkPlanIsCheckedBeforeEnrollment() throws Exception {
            String body = workPlanJson(UNKNOWN_ID, START, END, "4");

            ResultActions result = putJson(pathOf(UNKNOWN_ID), body);

            result.andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message")
                            .value(WORK_PLAN_NOT_FOUND));
        }

        @ParameterizedTest(name = "{0} = {1}")
        @CsvSource({
            "rangeEnd, \"2026-11-02\", rangeValid",
            "availableHoursPerDay, 24.01, availableHoursPerDay",
            "availableHoursPerDay, -0.01, availableHoursPerDay",
            "enrollmentId, 0, enrollmentId",
            "rangeStart, null, rangeStart"
        })
        @DisplayName("invalid body returns 400 naming the problem and "
                + "changes nothing")
        void invalidBodyReturns400(final String field, final String rawValue,
                final String errorKey) throws Exception {
            // The stored plan differs from the body's other fields, so a
            // partial update would show up in the GET below. The rangeEnd
            // value is START, which makes the range empty.
            long enrollmentId = createEnrollment(createCourse());
            LocalDate oldStart = START.minusDays(7);
            LocalDate oldEnd = END.minusDays(7);
            long id = createWorkPlan(enrollmentId, oldStart, oldEnd);
            String body = workPlanJsonWith(enrollmentId, field, rawValue);

            ResultActions result = putJson(pathOf(id), body);

            result.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(VALIDATION_FAILED))
                    .andExpect(jsonPath("$.fieldErrors." + errorKey)
                            .exists());
            assertStored(id, enrollmentId, oldStart, oldEnd, DEFAULT_HOURS);
        }

        @Test
        @DisplayName("id that is not a number returns 400")
        void invalidIdReturns400() throws Exception {
            long enrollmentId = createEnrollment(createCourse());
            String body = workPlanJson(enrollmentId, START, END, "4");

            ResultActions result = putJson(PATH + "/abc", body);

            result.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(INVALID_ID));
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
        @DisplayName("id that is not a number returns 400")
        void invalidIdReturns400() throws Exception {
            ResultActions result = mvc().perform(delete(PATH + "/abc"));

            result.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(INVALID_ID));
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

        @Test
        @DisplayName("work plan whose only task was deleted returns 204")
        void afterTaskIsDeletedReturns204() throws Exception {
            long courseId = createCourse();
            long enrollmentId = createEnrollment(courseId);
            long id = createWorkPlan(enrollmentId, START, END);
            long taskId = createTask(id, createCoursework(courseId),
                    enrollmentId, START.plusDays(2));
            mvc().perform(delete("/api/tasks/" + taskId))
                    .andExpect(status().isNoContent());

            ResultActions result = mvc().perform(delete(pathOf(id)));

            result.andExpect(status().isNoContent());
            getWorkPlan(id).andExpect(status().isNotFound());
        }
    }
}
