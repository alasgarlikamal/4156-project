/*
    AI usage: API Test Support: Opus 5.5, ~95K tokens
 */
package com.perihelionApi;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.jayway.jsonpath.JsonPath;

/**
 * Shared setup for the API tests.
 *
 * <p>Boots the whole application (the real controllers, services and SQLite
 * database) and sends HTTP requests to it through MockMvc. Nothing is
 * mocked. API tests use their own in-memory database, separate from the one
 * the other tests share, so the rows they create never change the counts
 * another test class checks. The helpers below create test data over HTTP,
 * the same way a client would.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.datasource.url="
        + "jdbc:sqlite:file:perihelion-api-test?mode=memory&cache=shared")
public abstract class ApiTestSupport {

    /** An id that no test creates, for "not found" cases. */
    protected static final long UNKNOWN_ID = 999_999_999L;

    /** Sends HTTP requests to the running application. */
    @Autowired
    private MockMvc mockMvc;

    /**
     * Returns the MockMvc client.
     *
     * @return the client
     */
    protected MockMvc mvc() {
        return mockMvc;
    }

    /**
     * Sends a POST request with a JSON body.
     *
     * @param path the request path
     * @param json the request body
     * @return the result, for assertions
     * @throws Exception if the request cannot be sent
     */
    protected ResultActions postJson(final String path, final String json)
            throws Exception {
        return mockMvc.perform(post(path)
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    /**
     * Sends a PUT request with a JSON body.
     *
     * @param path the request path
     * @param json the request body
     * @return the result, for assertions
     * @throws Exception if the request cannot be sent
     */
    protected ResultActions putJson(final String path, final String json)
            throws Exception {
        return mockMvc.perform(put(path)
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    /**
     * Checks for 201 Created and returns the id from the response.
     *
     * @param result the create request result
     * @return the id field of the response body
     * @throws Exception if the response is not 201 or has no id
     */
    protected static long idOf(final ResultActions result) throws Exception {
        String body = result.andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    /**
     * Returns the prefix followed by a short random suffix, so that names
     * from different tests never clash. The suffix is a hyphen and 8 hex
     * characters, which keeps calendar SUMMARY lines short.
     *
     * @param prefix the start of the name
     * @return the prefix plus a random suffix
     */
    protected static String unique(final String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * Writes a value as a JSON string, escaping quotes and backslashes.
     *
     * @param value the value, written with its toString(); may be null
     * @return the quoted string, or the JSON literal null
     */
    private static String text(final Object value) {
        if (value == null) {
            return "null";
        }
        String escaped = value.toString()
                .replace("\\", "\\\\").replace("\"", "\\\"");
        return "\"" + escaped + "\"";
    }

    /**
     * Builds a course request body.
     *
     * @param title the course title
     * @param code the course code, or null to leave the field out
     * @param termStart the first day of the term
     * @param termEnd the last day of the term
     * @return the JSON body
     */
    protected static String courseJson(final String title, final String code,
            final LocalDate termStart, final LocalDate termEnd) {
        String codeField = "";
        if (code != null) {
            codeField = "\"code\": " + text(code) + ", ";
        }
        return "{" + codeField + "\"title\": " + text(title)
                + ", \"termStart\": " + text(termStart)
                + ", \"termEnd\": " + text(termEnd) + "}";
    }

    /**
     * Creates a course with a unique title, no code and the term
     * 2026-09-01 to 2026-12-20.
     *
     * @return the new course id
     * @throws Exception if the request fails or does not return 201
     */
    protected long createCourse() throws Exception {
        return createCourse(unique("Course"), null,
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 12, 20));
    }

    /**
     * Creates a course with the given fields.
     *
     * @param title the course title
     * @param code the course code, or null to leave the field out
     * @param termStart the first day of the term
     * @param termEnd the last day of the term
     * @return the new course id
     * @throws Exception if the request fails or does not return 201
     */
    protected long createCourse(final String title, final String code,
            final LocalDate termStart, final LocalDate termEnd)
            throws Exception {
        return idOf(postJson("/api/courses",
                courseJson(title, code, termStart, termEnd)));
    }

    /**
     * Builds an enrollment request body.
     *
     * @param courseId the course id
     * @param learnerRef the learner reference
     * @return the JSON body
     */
    protected static String enrollmentJson(final long courseId,
            final UUID learnerRef) {
        return "{\"courseId\": " + courseId
                + ", \"learnerRef\": " + text(learnerRef) + "}";
    }

    /**
     * Creates an enrollment for a new random learner.
     *
     * @param courseId the course to enroll in
     * @return the new enrollment id
     * @throws Exception if the request fails or does not return 201
     */
    protected long createEnrollment(final long courseId) throws Exception {
        return createEnrollment(courseId, UUID.randomUUID());
    }

    /**
     * Creates an enrollment for the given learner.
     *
     * @param courseId the course to enroll in
     * @param learnerRef the learner reference
     * @return the new enrollment id
     * @throws Exception if the request fails or does not return 201
     */
    protected long createEnrollment(final long courseId,
            final UUID learnerRef) throws Exception {
        return idOf(postJson("/api/enrollments",
                enrollmentJson(courseId, learnerRef)));
    }

    /**
     * Builds a coursework request body.
     *
     * @param courseId the course id
     * @param kind the kind, such as "ASSIGNMENT" or "EXAM"
     * @param title the coursework title
     * @param deadline the deadline as an ISO instant, such as
     *     "2026-11-15T23:59:00Z"
     * @param estimatedEffortHours the hours, written as a raw JSON number
     * @return the JSON body
     */
    protected static String courseworkJson(final long courseId,
            final String kind, final String title, final String deadline,
            final String estimatedEffortHours) {
        return "{\"courseId\": " + courseId
                + ", \"kind\": " + text(kind)
                + ", \"title\": " + text(title)
                + ", \"deadline\": " + text(deadline)
                + ", \"estimatedEffortHours\": " + estimatedEffortHours + "}";
    }

    /**
     * Creates an assignment with a unique title, the deadline
     * 2026-11-15T23:59:00Z and 3 estimated hours.
     *
     * @param courseId the course the coursework belongs to
     * @return the new coursework id
     * @throws Exception if the request fails or does not return 201
     */
    protected long createCoursework(final long courseId) throws Exception {
        return createCoursework(courseId, "ASSIGNMENT",
                "2026-11-15T23:59:00Z");
    }

    /**
     * Creates coursework of the given kind and deadline, with a unique
     * title and 3 estimated hours.
     *
     * @param courseId the course the coursework belongs to
     * @param kind the kind, such as "ASSIGNMENT" or "EXAM"
     * @param deadline the deadline as an ISO instant
     * @return the new coursework id
     * @throws Exception if the request fails or does not return 201
     */
    protected long createCoursework(final long courseId, final String kind,
            final String deadline) throws Exception {
        return idOf(postJson("/api/coursework",
                courseworkJson(courseId, kind, unique("Work"), deadline,
                        "3")));
    }

    /**
     * Builds a submission request body.
     *
     * @param enrollmentId the enrollment id
     * @param courseworkId the coursework id
     * @param submittedAt the submission time as an ISO instant, or null to
     *     leave the field out
     * @return the JSON body
     */
    protected static String submissionJson(final long enrollmentId,
            final long courseworkId, final String submittedAt) {
        String submittedAtField = "";
        if (submittedAt != null) {
            submittedAtField = ", \"submittedAt\": " + text(submittedAt);
        }
        return "{\"enrollmentId\": " + enrollmentId
                + ", \"courseworkId\": " + courseworkId
                + submittedAtField + "}";
    }

    /**
     * Creates a submission made at 2026-11-10T12:00:00Z.
     *
     * @param enrollmentId the enrollment that submits
     * @param courseworkId the coursework submitted, from the same course
     * @return the new submission id
     * @throws Exception if the request fails or does not return 201
     */
    protected long createSubmission(final long enrollmentId,
            final long courseworkId) throws Exception {
        return idOf(postJson("/api/submissions",
                submissionJson(enrollmentId, courseworkId,
                        "2026-11-10T12:00:00Z")));
    }

    /**
     * Builds a work plan request body.
     *
     * @param enrollmentId the enrollment id
     * @param rangeStart the first day of the plan
     * @param rangeEnd the last day of the plan
     * @param availableHoursPerDay the hours, written as a raw JSON number
     * @return the JSON body
     */
    protected static String workPlanJson(final long enrollmentId,
            final LocalDate rangeStart, final LocalDate rangeEnd,
            final String availableHoursPerDay) {
        return "{\"enrollmentId\": " + enrollmentId
                + ", \"rangeStart\": " + text(rangeStart)
                + ", \"rangeEnd\": " + text(rangeEnd)
                + ", \"availableHoursPerDay\": " + availableHoursPerDay + "}";
    }

    /**
     * Creates a work plan with 4 available hours per day.
     *
     * @param enrollmentId the enrollment the plan belongs to
     * @param rangeStart the first day of the plan
     * @param rangeEnd the last day of the plan
     * @return the new work plan id
     * @throws Exception if the request fails or does not return 201
     */
    protected long createWorkPlan(final long enrollmentId,
            final LocalDate rangeStart, final LocalDate rangeEnd)
            throws Exception {
        return idOf(postJson("/api/workplans",
                workPlanJson(enrollmentId, rangeStart, rangeEnd, "4")));
    }

    /**
     * Builds a task request body.
     *
     * @param workPlanId the work plan id
     * @param courseworkId the coursework id
     * @param enrollmentId the enrollment id
     * @param title the task title
     * @param targetDate the day the task is planned for
     * @param estimatedWorkHours the hours, written as a raw JSON number
     * @return the JSON body
     */
    protected static String taskJson(final long workPlanId,
            final long courseworkId, final long enrollmentId,
            final String title, final LocalDate targetDate,
            final String estimatedWorkHours) {
        return "{\"workPlanId\": " + workPlanId
                + ", \"courseworkId\": " + courseworkId
                + ", \"enrollmentId\": " + enrollmentId
                + ", \"title\": " + text(title)
                + ", \"targetDate\": " + text(targetDate)
                + ", \"estimatedWorkHours\": " + estimatedWorkHours + "}";
    }

    /**
     * Creates a task with a unique title and 2 estimated hours.
     *
     * @param workPlanId the work plan, owned by the enrollment
     * @param courseworkId the coursework, from the enrollment's course
     * @param enrollmentId the enrollment
     * @param targetDate a day inside the work plan's range
     * @return the new task id
     * @throws Exception if the request fails or does not return 201
     */
    protected long createTask(final long workPlanId, final long courseworkId,
            final long enrollmentId, final LocalDate targetDate)
            throws Exception {
        return idOf(postJson("/api/tasks",
                taskJson(workPlanId, courseworkId, enrollmentId,
                        unique("Task"), targetDate, "2")));
    }

    /**
     * Builds a course session request body.
     *
     * @param weekday the ISO weekday, Monday = 1 to Sunday = 7
     * @param startTime the start time, such as "14:00"
     * @param endTime the end time, such as "15:15"
     * @return the JSON body
     */
    protected static String sessionJson(final int weekday,
            final String startTime, final String endTime) {
        return "{\"weekday\": " + weekday
                + ", \"startTime\": " + text(startTime)
                + ", \"endTime\": " + text(endTime) + "}";
    }

    /**
     * Creates a weekly session for a course.
     *
     * @param courseId the course the session belongs to
     * @param weekday the ISO weekday, Monday = 1 to Sunday = 7
     * @param startTime the start time, such as "14:00"
     * @param endTime the end time, such as "15:15"
     * @return the new session id
     * @throws Exception if the request fails or does not return 201
     */
    protected long createSession(final long courseId, final int weekday,
            final String startTime, final String endTime) throws Exception {
        return idOf(postJson("/api/courses/" + courseId + "/sessions",
                sessionJson(weekday, startTime, endTime)));
    }
}
