/*
    AI usage: Coursework API Tests: Opus 5.5, ~126K tokens
 */
package com.perihelionApi.coursework;

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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.StringJoiner;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import com.perihelionApi.ApiTestSupport;

/**
 * Full-application API tests for {@code /api/coursework}, written from the
 * API description, with nothing mocked.
 *
 * <p>Each test creates its own courses and coursework over HTTP, plus any
 * enrollment, submission, work plan or task it needs, so no test depends on
 * another test, on the order the tests run in, or on the rows already in
 * the database.
 */
@DisplayName("Coursework API")
class CourseworkApiTest extends ApiTestSupport {

    /** Base path of the coursework endpoints. */
    private static final String PATH = "/api/coursework";

    /** Error message when the course does not exist. */
    private static final String COURSE_NOT_FOUND = "Course not found";

    /** Error message when the coursework does not exist. */
    private static final String COURSEWORK_NOT_FOUND = "Coursework not found";

    /** Error message when a delete is blocked by a submission. */
    private static final String HAS_SUBMISSIONS =
            "Coursework cannot be deleted while submissions exist";

    /** Error message when a delete is blocked by a task. */
    private static final String HAS_TASKS =
            "Coursework cannot be deleted while tasks exist";

    /** Error message for a bean-validation failure. */
    private static final String VALIDATION_FAILED = "Validation failed";

    /** Error message for an id path value of the wrong type. */
    private static final String INVALID_ID = "Invalid value for id";

    /** The exam kind, as the API writes it. */
    private static final String EXAM = "EXAM";

    /** The assignment kind, as the API writes it. */
    private static final String ASSIGNMENT = "ASSIGNMENT";

    /** A deadline with whole seconds and a zone, as the API expects. */
    private static final String DEADLINE = "2026-11-15T23:59:00Z";

    /** A deadline later than {@link #DEADLINE}. */
    private static final String LATER_DEADLINE = "2026-12-10T12:00:00Z";

    /** How far apart two hour values may be and still count as equal. */
    private static final double TOLERANCE = 1e-9;

    /**
     * Returns the path of one coursework item.
     *
     * @param id the coursework id
     * @return the path, such as {@code /api/coursework/7}
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
     * Builds a valid assignment body, then leaves out one field.
     *
     * @param field the field to leave out, such as "kind"
     * @param courseId the course id to send when courseId is kept
     * @return the JSON body
     */
    private static String courseworkJsonWithout(final String field,
            final long courseId) {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("courseId", String.valueOf(courseId));
        fields.put("kind", "\"" + ASSIGNMENT + "\"");
        fields.put("title", "\"" + unique("Work") + "\"");
        fields.put("deadline", "\"" + DEADLINE + "\"");
        fields.put("estimatedEffortHours", "3");
        fields.remove(field);
        StringJoiner body = new StringJoiner(", ", "{", "}");
        fields.forEach((name, value) -> body.add("\"" + name + "\": "
                + value));
        return body.toString();
    }

    /**
     * Sends a GET request for one coursework item.
     *
     * @param id the coursework id
     * @return the result, for assertions
     * @throws Exception if the request cannot be sent
     */
    private ResultActions getCoursework(final long id) throws Exception {
        return mvc().perform(get(pathOf(id)));
    }

    /**
     * Creates an assignment with the given title, the deadline
     * {@link #DEADLINE} and 3 estimated hours, so that a test knows every
     * field it should find stored later.
     *
     * @param courseId the course the assignment belongs to
     * @param title the assignment title
     * @return the new coursework id
     * @throws Exception if the request fails or does not return 201
     */
    private long createAssignment(final long courseId, final String title)
            throws Exception {
        return idOf(postJson(PATH, courseworkJson(courseId, ASSIGNMENT,
                title, DEADLINE, "3")));
    }

    /**
     * Checks that a response body holds the given coursework fields. The
     * hours are compared as numbers, since the database may send back
     * {@code 2} for {@code 2.0}.
     *
     * @param result the response to check
     * @param courseId the expected course id
     * @param kind the expected kind
     * @param title the expected title
     * @param deadline the expected deadline, as an ISO instant
     * @param hours the expected estimated effort, in hours
     * @throws Exception if a check fails
     */
    private static void expectFields(final ResultActions result,
            final long courseId, final String kind, final String title,
            final String deadline, final double hours) throws Exception {
        result.andExpect(jsonPath("$.courseId").value(courseId))
                .andExpect(jsonPath("$.kind").value(kind))
                .andExpect(jsonPath("$.title").value(title))
                .andExpect(jsonPath("$.deadline").value(deadline))
                .andExpect(jsonPath("$.estimatedEffortHours",
                        closeTo(hours, TOLERANCE), Double.class));
    }

    /**
     * Checks with a GET request that a coursework item is stored with the
     * given fields.
     *
     * @param id the coursework id
     * @param courseId the expected course id
     * @param kind the expected kind
     * @param title the expected title
     * @param deadline the expected deadline, as an ISO instant
     * @param hours the expected estimated effort, in hours
     * @throws Exception if the request cannot be sent or a check fails
     */
    private void assertStored(final long id, final long courseId,
            final String kind, final String title, final String deadline,
            final double hours) throws Exception {
        ResultActions stored = getCoursework(id)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id));
        expectFields(stored, courseId, kind, title, deadline, hours);
    }

    /** Tests for POST /api/coursework, which creates coursework. */
    @Nested
    @DisplayName("POST /api/coursework")
    class Create {

        @Test
        @DisplayName("valid request returns 201 with every field, and a GET "
                + "returns the same")
        void validRequestReturns201() throws Exception {
            // Assumption: the deadline comes back exactly as sent, since it
            // is sent in UTC with whole seconds.
            long courseId = createCourse();
            String title = unique("Midterm");

            ResultActions result = postJson(PATH,
                    courseworkJson(courseId, EXAM, title, DEADLINE, "4.5"));

            result.andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").isNumber());
            expectFields(result, courseId, EXAM, title, DEADLINE, 4.5);
            assertStored(idOf(result), courseId, EXAM, title, DEADLINE,
                    4.5);
        }

        @Test
        @DisplayName("unknown course returns 404")
        void unknownCourseReturns404() throws Exception {
            String body = courseworkJson(UNKNOWN_ID, EXAM, unique("Work"),
                    DEADLINE, "3");

            ResultActions result = postJson(PATH, body);

            result.andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value(COURSE_NOT_FOUND));
        }

        @Test
        @DisplayName("missing kind returns 400 naming kind")
        void missingKindReturns400() throws Exception {
            String body = courseworkJsonWithout("kind", createCourse());

            ResultActions result = postJson(PATH, body);

            result.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(VALIDATION_FAILED))
                    .andExpect(jsonPath("$.fieldErrors.kind")
                            .value("must not be null"));
        }
    }

    /** Tests for GET /api/coursework, with its filters. */
    @Nested
    @DisplayName("GET /api/coursework")
    class ListCoursework {

        @Test
        @DisplayName("courseId filter returns only that course's coursework")
        void courseIdFilterReturnsOnlyThatCourse() throws Exception {
            long courseA = createCourse();
            long courseB = createCourse();
            long inA = createCoursework(courseA);
            long inB = createCoursework(courseB);

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
        @DisplayName("courseId and kind together must both match")
        void bothFiltersAreCombinedWithAnd() throws Exception {
            long course = createCourse();
            long match = createCoursework(course, EXAM, DEADLINE);
            long otherKind = createCoursework(course, ASSIGNMENT, DEADLINE);
            long otherCourse =
                    createCoursework(createCourse(), EXAM, DEADLINE);

            ResultActions result = mvc().perform(get(PATH)
                    .param("courseId", String.valueOf(course))
                    .param("kind", EXAM));

            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$[*].id", hasItem(jsonInt(match))))
                    .andExpect(jsonPath("$[*].id",
                            not(hasItem(jsonInt(otherKind)))))
                    .andExpect(jsonPath("$[*].id",
                            not(hasItem(jsonInt(otherCourse)))));
        }
    }

    /** Tests for GET /api/coursework/{id}, which reads one item. */
    @Nested
    @DisplayName("GET /api/coursework/{id}")
    class GetById {

        @Test
        @DisplayName("existing coursework returns 200 with its fields")
        void existingCourseworkReturns200() throws Exception {
            long courseId = createCourse();
            String title = unique("Final");
            long id = idOf(postJson(PATH,
                    courseworkJson(courseId, EXAM, title, DEADLINE, "2.5")));

            ResultActions result = getCoursework(id);

            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(id));
            expectFields(result, courseId, EXAM, title, DEADLINE, 2.5);
        }

        @Test
        @DisplayName("id that does not exist returns 404")
        void unknownIdReturns404() throws Exception {
            ResultActions result = getCoursework(UNKNOWN_ID);

            result.andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message")
                            .value(COURSEWORK_NOT_FOUND));
        }

        @Test
        @DisplayName("id that is not a number returns 400")
        void invalidIdReturns400() throws Exception {
            ResultActions result = mvc().perform(get(PATH + "/abc"));

            result.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(INVALID_ID));
        }
    }

    /** Tests for PUT /api/coursework/{id}, which replaces every field. */
    @Nested
    @DisplayName("PUT /api/coursework/{id}")
    class Update {

        @Test
        @DisplayName("replacing every field, including the course, returns "
                + "200 and is stored")
        void replaceEveryFieldReturns200() throws Exception {
            long id = createAssignment(createCourse(), unique("Work"));
            long newCourse = createCourse();
            String newTitle = unique("Exam");
            String body = courseworkJson(newCourse, EXAM, newTitle,
                    LATER_DEADLINE, "7.25");

            ResultActions result = putJson(pathOf(id), body);

            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(id));
            expectFields(result, newCourse, EXAM, newTitle, LATER_DEADLINE,
                    7.25);
            assertStored(id, newCourse, EXAM, newTitle, LATER_DEADLINE,
                    7.25);
        }

        @Test
        @DisplayName("unknown coursework returns 404")
        void unknownCourseworkReturns404() throws Exception {
            String body = courseworkJson(createCourse(), EXAM,
                    unique("Work"), DEADLINE, "3");

            ResultActions result = putJson(pathOf(UNKNOWN_ID), body);

            result.andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message")
                            .value(COURSEWORK_NOT_FOUND));
        }

        @Test
        @DisplayName("unknown course returns 404 and changes nothing")
        void unknownCourseReturns404() throws Exception {
            long courseId = createCourse();
            String title = unique("Work");
            long id = createAssignment(courseId, title);
            String body = courseworkJson(UNKNOWN_ID, EXAM, unique("New"),
                    LATER_DEADLINE, "5");

            ResultActions result = putJson(pathOf(id), body);

            result.andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value(COURSE_NOT_FOUND));
            assertStored(id, courseId, ASSIGNMENT, title, DEADLINE, 3);
        }
    }

    /** Tests for DELETE /api/coursework/{id}. */
    @Nested
    @DisplayName("DELETE /api/coursework/{id}")
    class Delete {

        @Test
        @DisplayName("existing coursework returns 204, then GET returns 404")
        void existingCourseworkReturns204() throws Exception {
            long id = createCoursework(createCourse());

            ResultActions result = mvc().perform(delete(pathOf(id)));

            result.andExpect(status().isNoContent());
            getCoursework(id).andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message")
                            .value(COURSEWORK_NOT_FOUND));
        }

        @Test
        @DisplayName("unknown coursework returns 404")
        void unknownCourseworkReturns404() throws Exception {
            ResultActions result = mvc().perform(delete(pathOf(UNKNOWN_ID)));

            result.andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message")
                            .value(COURSEWORK_NOT_FOUND));
        }

        @Test
        @DisplayName("coursework with a submission returns 409 and is kept")
        void withSubmissionReturns409() throws Exception {
            long courseId = createCourse();
            String title = unique("Work");
            long id = createAssignment(courseId, title);
            createSubmission(createEnrollment(courseId), id);

            ResultActions result = mvc().perform(delete(pathOf(id)));

            result.andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value(HAS_SUBMISSIONS));
            assertStored(id, courseId, ASSIGNMENT, title, DEADLINE, 3);
        }

        @Test
        @DisplayName("coursework with a task returns 409 and is kept")
        void withTaskReturns409() throws Exception {
            long courseId = createCourse();
            String title = unique("Work");
            long id = createAssignment(courseId, title);
            long enrollmentId = createEnrollment(courseId);
            long workPlanId = createWorkPlan(enrollmentId,
                    LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 8));
            createTask(workPlanId, id, enrollmentId,
                    LocalDate.of(2026, 11, 4));

            ResultActions result = mvc().perform(delete(pathOf(id)));

            result.andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value(HAS_TASKS));
            assertStored(id, courseId, ASSIGNMENT, title, DEADLINE, 3);
        }
    }
}
