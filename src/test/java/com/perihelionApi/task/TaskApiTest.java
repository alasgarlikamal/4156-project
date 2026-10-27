/*
    AI usage: Task API Tests: Opus 5.5, ~142K tokens
 */
package com.perihelionApi.task;

import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;

import com.perihelionApi.ApiTestSupport;

/**
 * Full-application API tests for {@code /api/tasks}, written from the API
 * description, with nothing mocked.
 *
 * <p>A task needs a work plan, a piece of coursework and an enrollment that
 * fit together. Each test builds that chain of rows over HTTP with
 * {@code createChain()}, so no test depends on another test, on the order
 * the tests run in, or on the rows already in the database.
 */
@DisplayName("Tasks API")
class TaskApiTest extends ApiTestSupport {

    /** Base path of the task endpoints. */
    private static final String PATH = "/api/tasks";

    /** First day of every work plan that {@code createChain()} makes. */
    private static final LocalDate RANGE_START = LocalDate.of(2026, 11, 1);

    /** Last day of every work plan that {@code createChain()} makes. */
    private static final LocalDate RANGE_END = LocalDate.of(2026, 11, 30);

    /** A day well inside the range of the plans {@code createChain()} makes. */
    private static final LocalDate IN_RANGE = LocalDate.of(2026, 11, 15);

    /** First day of a second plan that does not overlap the first. */
    private static final LocalDate NEXT_START = LocalDate.of(2026, 12, 1);

    /** Last day of a second plan that does not overlap the first. */
    private static final LocalDate NEXT_END = LocalDate.of(2026, 12, 15);

    /** Hours that the helpers give a task, as a number. */
    private static final double DEFAULT_HOURS = 2.0;

    /** Largest difference allowed when hours are compared as numbers. */
    private static final double TOLERANCE = 1e-9;

    /** Error message when the task does not exist. */
    private static final String TASK_NOT_FOUND = "Task not found";

    /** Error message when the work plan does not exist. */
    private static final String WORK_PLAN_NOT_FOUND = "Work plan not found";

    /** Error message when the coursework does not exist. */
    private static final String COURSEWORK_NOT_FOUND = "Coursework not found";

    /** Error message when the enrollment does not exist. */
    private static final String ENROLLMENT_NOT_FOUND = "Enrollment not found";

    /** Error message when the work plan belongs to another enrollment. */
    private static final String ENROLLMENT_MISMATCH =
            "Task enrollment must match the work plan's enrollment";

    /** Error message when the coursework is from another course. */
    private static final String WRONG_COURSE =
            "Coursework must belong to the enrollment's course";

    /** Error message when targetDate is outside the work plan's range. */
    private static final String OUT_OF_RANGE =
            "targetDate must fall within the work plan's range";

    /** Error message for a bean-validation failure. */
    private static final String VALIDATION_FAILED = "Validation failed";

    /** Error message for a path id that is not a number. */
    private static final String INVALID_ID = "Invalid value for id";

    /** The message of {@code @NotBlank}. */
    private static final String MUST_NOT_BE_BLANK = "must not be blank";

    /**
     * The ids of a course, an enrollment in it, coursework in it and a work
     * plan for that enrollment: everything one valid task needs.
     *
     * @param courseId the course
     * @param enrollmentId the enrollment in the course
     * @param courseworkId the coursework in the course
     * @param workPlanId the enrollment's work plan, from RANGE_START to
     *     RANGE_END
     */
    private record Chain(long courseId, long enrollmentId, long courseworkId,
            long workPlanId) {
    }

    /**
     * Creates, over HTTP, a new course, an enrollment in it, an assignment
     * in it and a work plan for the enrollment from RANGE_START to
     * RANGE_END.
     *
     * @return the ids of the new rows
     * @throws Exception if a request fails or does not return 201
     */
    private Chain createChain() throws Exception {
        long courseId = createCourse();
        long enrollmentId = createEnrollment(courseId);
        long courseworkId = createCoursework(courseId);
        long workPlanId =
                createWorkPlan(enrollmentId, RANGE_START, RANGE_END);
        return new Chain(courseId, enrollmentId, courseworkId, workPlanId);
    }

    /**
     * Returns the path of one task.
     *
     * @param id the task id
     * @return the path, such as {@code /api/tasks/7}
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
     * Builds a task body that uses all three ids of a chain.
     *
     * @param chain the work plan, coursework and enrollment to use
     * @param title the task title
     * @param targetDate the day the task is planned for
     * @param hours the hours, written as a raw JSON number
     * @return the JSON body
     */
    private static String taskJson(final Chain chain, final String title,
            final LocalDate targetDate, final String hours) {
        return taskJson(chain.workPlanId(), chain.courseworkId(),
                chain.enrollmentId(), title, targetDate, hours);
    }

    /**
     * Returns the fields of a valid task body for a chain, each written as
     * raw JSON, in a fixed order: a unique title, IN_RANGE as the target
     * date and 2 hours.
     *
     * @param chain the work plan, coursework and enrollment to use
     * @return the field names mapped to their raw JSON values
     */
    private static Map<String, String> validFields(final Chain chain) {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("workPlanId", String.valueOf(chain.workPlanId()));
        fields.put("courseworkId", String.valueOf(chain.courseworkId()));
        fields.put("enrollmentId", String.valueOf(chain.enrollmentId()));
        fields.put("title", "\"" + unique("Task") + "\"");
        fields.put("targetDate", "\"" + IN_RANGE + "\"");
        fields.put("estimatedWorkHours", "2");
        return fields;
    }

    /**
     * Writes fields as a JSON object.
     *
     * @param fields the field names mapped to their raw JSON values
     * @return the JSON body
     */
    private static String toJson(final Map<String, String> fields) {
        return fields.entrySet().stream()
                .map(entry -> "\"" + entry.getKey() + "\": "
                        + entry.getValue())
                .collect(Collectors.joining(", ", "{", "}"));
    }

    /**
     * Builds a valid task body for a chain with one field set to the given
     * raw JSON value. A field that is not in a task body, such as "id", is
     * added.
     *
     * @param field the field to set
     * @param rawValue the value, written as raw JSON, such as {@code 7} or
     *     {@code "   "} with its quotes
     * @param chain the work plan, coursework and enrollment to use
     * @return the JSON body
     */
    private static String taskJsonWith(final String field,
            final String rawValue, final Chain chain) {
        Map<String, String> fields = validFields(chain);
        fields.put(field, rawValue);
        return toJson(fields);
    }

    /**
     * Creates a task in a chain on IN_RANGE with 2 estimated hours.
     *
     * @param chain the work plan, coursework and enrollment to use
     * @param title the task title
     * @return the new task id
     * @throws Exception if the request fails or does not return 201
     */
    private long createTask(final Chain chain, final String title)
            throws Exception {
        return idOf(postJson(PATH, taskJson(chain, title, IN_RANGE, "2")));
    }

    /**
     * Creates a task in a chain with a unique title and 2 estimated hours.
     *
     * @param chain the work plan, coursework and enrollment to use
     * @param targetDate a day inside the chain's work plan range
     * @return the new task id
     * @throws Exception if the request fails or does not return 201
     */
    private long createTask(final Chain chain, final LocalDate targetDate)
            throws Exception {
        return createTask(chain.workPlanId(), chain.courseworkId(),
                chain.enrollmentId(), targetDate);
    }

    /**
     * Sends a GET request for one task.
     *
     * @param id the task id
     * @return the result, for assertions
     * @throws Exception if the request cannot be sent
     */
    private ResultActions getTask(final long id) throws Exception {
        return mvc().perform(get(pathOf(id)));
    }

    /**
     * Sends a GET request for the task list with one filter.
     *
     * @param param the filter name, such as "workPlanId"
     * @param value the filter value
     * @return the result, for assertions
     * @throws Exception if the request cannot be sent
     */
    private ResultActions listBy(final String param, final long value)
            throws Exception {
        return mvc().perform(get(PATH).param(param, String.valueOf(value)));
    }

    /**
     * Checks estimatedWorkHours numerically, because SQLite may return
     * {@code 2.0} as {@code 2}.
     *
     * @param hours the expected hours
     * @return the matcher
     */
    private static ResultMatcher hoursAre(final double hours) {
        return jsonPath("$.estimatedWorkHours", closeTo(hours, TOLERANCE),
                Double.class);
    }

    /**
     * Checks every task field of a response except the id.
     *
     * @param chain the expected work plan, coursework and enrollment
     * @param title the expected title
     * @param targetDate the expected target date
     * @param hours the expected hours
     * @return the matcher
     */
    private static ResultMatcher fieldsAre(final Chain chain,
            final String title, final LocalDate targetDate,
            final double hours) {
        ResultMatcher[] matchers = {
            jsonPath("$.workPlanId").value(chain.workPlanId()),
            jsonPath("$.courseworkId").value(chain.courseworkId()),
            jsonPath("$.enrollmentId").value(chain.enrollmentId()),
            jsonPath("$.title").value(title),
            jsonPath("$.targetDate").value(targetDate.toString()),
            hoursAre(hours),
        };
        return result -> {
            for (ResultMatcher matcher : matchers) {
                matcher.match(result);
            }
        };
    }

    /**
     * Checks with a GET request that a task is stored with the given
     * fields.
     *
     * @param id the task id
     * @param chain the expected work plan, coursework and enrollment
     * @param title the expected title
     * @param targetDate the expected target date
     * @param hours the expected hours
     * @throws Exception if the request cannot be sent or a check fails
     */
    private void assertStored(final long id, final Chain chain,
            final String title, final LocalDate targetDate,
            final double hours) throws Exception {
        getTask(id)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(fieldsAre(chain, title, targetDate, hours));
    }

    /**
     * Checks with a GET request that a task made by
     * {@code createTask(chain, title)} still has its original fields.
     *
     * @param id the task id
     * @param chain the chain the task was created in
     * @param title the title the task was created with
     * @throws Exception if the request cannot be sent or a check fails
     */
    private void assertUnchanged(final long id, final Chain chain,
            final String title) throws Exception {
        assertStored(id, chain, title, IN_RANGE, DEFAULT_HOURS);
    }

    /** Tests for POST /api/tasks, which plans a task in a work plan. */
    @Nested
    @DisplayName("POST /api/tasks")
    class Create {

        @Test
        @DisplayName("valid request returns 201 with every field, and GET "
                + "returns the same")
        void validRequestReturns201() throws Exception {
            Chain chain = createChain();
            String title = unique("Task");

            ResultActions result =
                    postJson(PATH, taskJson(chain, title, IN_RANGE, "3.5"));

            result.andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").isNumber())
                    .andExpect(fieldsAre(chain, title, IN_RANGE, 3.5));
            assertStored(idOf(result), chain, title, IN_RANGE, 3.5);
        }

        @ParameterizedTest(name = "targetDate {0}")
        @ValueSource(strings = {"2026-11-01", "2026-11-30"})
        @DisplayName("targetDate on the first or last day of the work plan "
                + "returns 201")
        void targetDateOnRangeEndReturns201(final LocalDate targetDate)
                throws Exception {
            // The two dates are RANGE_START and RANGE_END; both ends count
            // as inside the range.
            Chain chain = createChain();
            String title = unique("Task");

            ResultActions result =
                    postJson(PATH, taskJson(chain, title, targetDate, "2"));

            result.andExpect(status().isCreated())
                    .andExpect(fieldsAre(chain, title, targetDate, 2));
        }

        @Test
        @DisplayName("id of an existing task in the body is ignored: a new "
                + "task is created and the existing one is unchanged")
        void bodyIdCreatesNewTask() throws Exception {
            Chain chain = createChain();
            String title = unique("Task");
            long existing = createTask(chain, title);
            String body =
                    taskJsonWith("id", String.valueOf(existing), chain);

            ResultActions result = postJson(PATH, body);

            result.andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id", not(is(jsonInt(existing)))));
            assertUnchanged(existing, chain, title);
        }

        @Test
        @DisplayName("blank title returns 400 naming title")
        void blankTitleReturns400() throws Exception {
            Chain chain = createChain();

            ResultActions result =
                    postJson(PATH, taskJsonWith("title", "\"   \"", chain));

            result.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(VALIDATION_FAILED))
                    .andExpect(jsonPath("$.fieldErrors.title")
                            .value(MUST_NOT_BE_BLANK));
        }

        @ParameterizedTest(name = "unknown {0}")
        @CsvSource({"workPlanId, " + WORK_PLAN_NOT_FOUND,
            "courseworkId, " + COURSEWORK_NOT_FOUND,
            "enrollmentId, " + ENROLLMENT_NOT_FOUND})
        @DisplayName("id that does not exist returns 404 naming what is "
                + "missing")
        void unknownReferenceReturns404(final String field,
                final String message) throws Exception {
            Chain chain = createChain();
            String unknown = String.valueOf(UNKNOWN_ID);

            ResultActions result =
                    postJson(PATH, taskJsonWith(field, unknown, chain));

            result.andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value(message));
        }

        @Test
        @DisplayName("enrollment other than the work plan's, in the same "
                + "course, returns 409")
        void enrollmentMismatchReturns409() throws Exception {
            Chain chain = createChain();
            long other = createEnrollment(chain.courseId());
            String body =
                    taskJsonWith("enrollmentId", String.valueOf(other), chain);

            ResultActions result = postJson(PATH, body);

            result.andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message")
                            .value(ENROLLMENT_MISMATCH));
        }

        @Test
        @DisplayName("coursework from another course returns 409")
        void courseworkFromOtherCourseReturns409() throws Exception {
            Chain chain = createChain();
            long otherWork = createCoursework(createCourse());
            String body = taskJsonWith("courseworkId",
                    String.valueOf(otherWork), chain);

            ResultActions result = postJson(PATH, body);

            result.andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value(WRONG_COURSE));
        }

        @ParameterizedTest(name = "targetDate {0}")
        @ValueSource(strings = {"2026-10-31", "2026-12-01"})
        @DisplayName("targetDate one day outside the work plan's range "
                + "returns 409")
        void targetDateOutsideRangeReturns409(final LocalDate targetDate)
                throws Exception {
            // The dates are the day before RANGE_START and the day after
            // RANGE_END.
            Chain chain = createChain();
            String body = taskJson(chain, unique("Task"), targetDate, "2");

            ResultActions result = postJson(PATH, body);

            result.andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value(OUT_OF_RANGE));
        }
    }

    /** Tests for GET /api/tasks, with and without filters. */
    @Nested
    @DisplayName("GET /api/tasks")
    class ListTasks {

        @Test
        @DisplayName("workPlanId filter returns only that work plan's tasks")
        void workPlanIdFilterReturnsOnlyThatPlan() throws Exception {
            // Both plans belong to the same enrollment, so only the work
            // plan tells the two tasks apart.
            Chain chain = createChain();
            long otherPlan =
                    createWorkPlan(chain.enrollmentId(), NEXT_START, NEXT_END);
            long match = createTask(chain, IN_RANGE);
            long other = createTask(otherPlan, chain.courseworkId(),
                    chain.enrollmentId(), NEXT_START);

            ResultActions result = listBy("workPlanId", chain.workPlanId());

            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$[*].id", hasItem(jsonInt(match))))
                    .andExpect(jsonPath("$[*].id",
                            not(hasItem(jsonInt(other)))))
                    .andExpect(jsonPath("$[*].workPlanId",
                            everyItem(is(jsonInt(chain.workPlanId())))));
        }

        @Test
        @DisplayName("enrollmentId, workPlanId and courseworkId together "
                + "must all match")
        void allFiltersAreCombinedWithAnd() throws Exception {
            Chain chain = createChain();
            long otherPlan =
                    createWorkPlan(chain.enrollmentId(), NEXT_START, NEXT_END);
            long otherWork = createCoursework(chain.courseId());
            long otherLearner = createEnrollment(chain.courseId());
            long otherLearnerPlan =
                    createWorkPlan(otherLearner, RANGE_START, RANGE_END);
            long match = createTask(chain, IN_RANGE);
            // Each decoy matches some of the three filters, but not all.
            long wrongPlan = createTask(otherPlan, chain.courseworkId(),
                    chain.enrollmentId(), NEXT_START);
            long wrongWork = createTask(chain.workPlanId(), otherWork,
                    chain.enrollmentId(), IN_RANGE);
            long wrongLearner = createTask(otherLearnerPlan,
                    chain.courseworkId(), otherLearner, IN_RANGE);

            ResultActions result = mvc().perform(get(PATH)
                    .param("enrollmentId",
                            String.valueOf(chain.enrollmentId()))
                    .param("workPlanId", String.valueOf(chain.workPlanId()))
                    .param("courseworkId",
                            String.valueOf(chain.courseworkId())));

            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$[*].id", hasItem(jsonInt(match))))
                    .andExpect(jsonPath("$[*].id",
                            not(hasItem(jsonInt(wrongPlan)))))
                    .andExpect(jsonPath("$[*].id",
                            not(hasItem(jsonInt(wrongWork)))))
                    .andExpect(jsonPath("$[*].id",
                            not(hasItem(jsonInt(wrongLearner)))));
        }
    }

    /** Tests for GET /api/tasks/{id}, which reads one task. */
    @Nested
    @DisplayName("GET /api/tasks/{id}")
    class GetById {

        @Test
        @DisplayName("existing task returns 200 with its fields")
        void existingTaskReturns200() throws Exception {
            Chain chain = createChain();
            String title = unique("Task");
            long id = idOf(postJson(PATH,
                    taskJson(chain, title, RANGE_END, "1.25")));

            ResultActions result = getTask(id);

            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(id))
                    .andExpect(fieldsAre(chain, title, RANGE_END, 1.25));
        }

        @Test
        @DisplayName("id that does not exist returns 404 with the standard "
                + "JSON error body")
        void unknownIdReturns404() throws Exception {
            ResultActions result = getTask(UNKNOWN_ID);

            result.andExpect(status().isNotFound())
                    .andExpect(content().contentTypeCompatibleWith(
                            MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.message").value(TASK_NOT_FOUND))
                    .andExpect(jsonPath("$.fieldErrors").isEmpty());
        }

        @Test
        @DisplayName("id that is not a number returns 400")
        void invalidIdReturns400() throws Exception {
            ResultActions result = mvc().perform(get(PATH + "/abc"));

            result.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(INVALID_ID));
        }
    }

    /** Tests for PUT /api/tasks/{id}, which replaces every field. */
    @Nested
    @DisplayName("PUT /api/tasks/{id}")
    class Update {

        @Test
        @DisplayName("valid request returns 200, replaces every field and "
                + "keeps the id")
        void validRequestReplacesEveryField() throws Exception {
            Chain chain = createChain();
            long id = createTask(chain, unique("Task"));
            Chain newChain = createChain();
            String title = unique("Task");

            ResultActions result = putJson(pathOf(id),
                    taskJson(newChain, title, RANGE_END, "5.25"));

            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(id))
                    .andExpect(fieldsAre(newChain, title, RANGE_END, 5.25));
            assertStored(id, newChain, title, RANGE_END, 5.25);
        }

        @Test
        @DisplayName("unknown task returns 404")
        void unknownTaskReturns404() throws Exception {
            String body = taskJson(createChain(), unique("Task"), IN_RANGE,
                    "2");

            ResultActions result = putJson(pathOf(UNKNOWN_ID), body);

            result.andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value(TASK_NOT_FOUND));
        }

        @ParameterizedTest(name = "unknown {0}")
        @CsvSource({"workPlanId, " + WORK_PLAN_NOT_FOUND,
            "courseworkId, " + COURSEWORK_NOT_FOUND,
            "enrollmentId, " + ENROLLMENT_NOT_FOUND})
        @DisplayName("id that does not exist returns 404 naming what is "
                + "missing and changes nothing")
        void unknownReferenceReturns404(final String field,
                final String message) throws Exception {
            Chain chain = createChain();
            String title = unique("Task");
            long id = createTask(chain, title);
            String unknown = String.valueOf(UNKNOWN_ID);

            ResultActions result =
                    putJson(pathOf(id), taskJsonWith(field, unknown, chain));

            result.andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value(message));
            assertUnchanged(id, chain, title);
        }

        @Test
        @DisplayName("enrollment other than the work plan's returns 409 and "
                + "changes nothing")
        void enrollmentMismatchReturns409() throws Exception {
            Chain chain = createChain();
            String title = unique("Task");
            long id = createTask(chain, title);
            long other = createEnrollment(chain.courseId());
            String body =
                    taskJsonWith("enrollmentId", String.valueOf(other), chain);

            ResultActions result = putJson(pathOf(id), body);

            result.andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message")
                            .value(ENROLLMENT_MISMATCH));
            assertUnchanged(id, chain, title);
        }

        @Test
        @DisplayName("coursework from another course returns 409 and "
                + "changes nothing")
        void courseworkFromOtherCourseReturns409() throws Exception {
            Chain chain = createChain();
            String title = unique("Task");
            long id = createTask(chain, title);
            long otherWork = createCoursework(createCourse());
            String body = taskJsonWith("courseworkId",
                    String.valueOf(otherWork), chain);

            ResultActions result = putJson(pathOf(id), body);

            result.andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value(WRONG_COURSE));
            assertUnchanged(id, chain, title);
        }

        @Test
        @DisplayName("targetDate one day after the work plan's range "
                + "returns 409 and changes nothing")
        void targetDateOutsideRangeReturns409() throws Exception {
            Chain chain = createChain();
            String title = unique("Task");
            long id = createTask(chain, title);
            String body = taskJson(chain, unique("Task"),
                    RANGE_END.plusDays(1), "2");

            ResultActions result = putJson(pathOf(id), body);

            result.andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value(OUT_OF_RANGE));
            assertUnchanged(id, chain, title);
        }
    }

    /** Tests for DELETE /api/tasks/{id}. */
    @Nested
    @DisplayName("DELETE /api/tasks/{id}")
    class Delete {

        @Test
        @DisplayName("existing task returns 204, then GET returns 404")
        void existingTaskReturns204() throws Exception {
            long id = createTask(createChain(), IN_RANGE);

            ResultActions result = mvc().perform(delete(pathOf(id)));

            result.andExpect(status().isNoContent());
            getTask(id).andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value(TASK_NOT_FOUND));
        }

        @Test
        @DisplayName("unknown task returns 404")
        void unknownTaskReturns404() throws Exception {
            ResultActions result = mvc().perform(delete(pathOf(UNKNOWN_ID)));

            result.andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value(TASK_NOT_FOUND));
        }
    }
}
