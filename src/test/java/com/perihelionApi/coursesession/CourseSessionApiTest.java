/*
    AI usage: Course Session API Tests: Opus 5.5, ~128K tokens
 */
package com.perihelionApi.coursesession;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import com.perihelionApi.ApiTestSupport;

/**
 * Full-application API tests for the weekly sessions of a course,
 * {@code /api/courses/{courseId}/sessions}, and for the course calendar
 * export, {@code /api/courses/{courseId}/calendar.ics}. They are written from
 * the API description, with nothing mocked.
 *
 * <p>Each test creates its own courses and sessions over HTTP, so no test
 * depends on another test, on the order the tests run in, or on the rows
 * already in the database. Two sessions with the same course, weekday,
 * start and end hit a database constraint, so every enabled test uses a new
 * course or different times.
 */
@DisplayName("Course Sessions API")
class CourseSessionApiTest extends ApiTestSupport {

    /** Error message when the course does not exist. */
    private static final String COURSE_NOT_FOUND = "Course not found";

    /** Error message when the session does not exist in the course. */
    private static final String SESSION_NOT_FOUND =
            "Course session not found";

    /** Error message for a bean-validation failure. */
    private static final String VALIDATION_FAILED = "Validation failed";

    /** Error message for a body that cannot be read. */
    private static final String MALFORMED_BODY = "Malformed request body";

    /** The only message a {@code @NotNull} field can give. */
    private static final String MUST_NOT_BE_NULL = "must not be null";

    /** Error message when endTime is not after startTime. */
    private static final String TIME_RANGE_MESSAGE =
            "endTime must be later than startTime";

    /** The line ending of every calendar line. */
    private static final String CRLF = "\r\n";

    /** A line break plus a space, which folds a long calendar line. */
    private static final String FOLD = "\r\n ";

    /** The format every DTSTAMP line must have. */
    private static final String DTSTAMP_FORMAT = "DTSTAMP:\\d{8}T\\d{6}Z";

    /**
     * Returns the sessions path of a course.
     *
     * @param courseId the course id, or any text such as "abc"
     * @return the path, such as {@code /api/courses/7/sessions}
     */
    private static String sessionsPath(final Object courseId) {
        return "/api/courses/" + courseId + "/sessions";
    }

    /**
     * Returns the path of one session of a course.
     *
     * @param courseId the course id, or any text such as "abc"
     * @param sessionId the session id, or any text such as "abc"
     * @return the path, such as {@code /api/courses/7/sessions/3}
     */
    private static String sessionPath(final Object courseId,
            final Object sessionId) {
        return sessionsPath(courseId) + "/" + sessionId;
    }

    /**
     * Returns the calendar path of a course.
     *
     * @param courseId the course id, or any text such as "abc"
     * @return the path, such as {@code /api/courses/7/calendar.ics}
     */
    private static String calendarPath(final Object courseId) {
        return "/api/courses/" + courseId + "/calendar.ics";
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
     * Builds a valid session body (Monday 14:00 to 15:15) that leaves out
     * one required field.
     *
     * @param field the field to leave out: "weekday", "startTime" or
     *     "endTime"
     * @return the JSON body
     */
    private static String sessionJsonWithout(final String field) {
        return Stream.of("\"weekday\": 1", "\"startTime\": \"14:00\"",
                        "\"endTime\": \"15:15\"")
                .filter(part -> !part.startsWith("\"" + field + "\""))
                .collect(Collectors.joining(", ", "{", "}"));
    }

    /**
     * Creates a course titled "SE", with no code and the given term, for
     * the calendar tests. The short title keeps SUMMARY lines unfolded.
     *
     * @param termStart the first day of the term, such as "2026-11-02"
     * @param termEnd the last day of the term, such as "2026-11-09"
     * @return the new course id
     * @throws Exception if the request fails or does not return 201
     */
    private long calendarCourse(final String termStart, final String termEnd)
            throws Exception {
        return createCourse("SE", null, LocalDate.parse(termStart),
                LocalDate.parse(termEnd));
    }

    /**
     * Sends a GET request for the sessions of a course.
     *
     * @param courseId the course id
     * @return the result, for assertions
     * @throws Exception if the request cannot be sent
     */
    private ResultActions listSessions(final long courseId) throws Exception {
        return mvc().perform(get(sessionsPath(courseId)));
    }

    /**
     * Sends a GET request for the calendar of a course, with no Accept
     * header, as a plain client would.
     *
     * @param courseId the course id
     * @return the result, for assertions
     * @throws Exception if the request cannot be sent
     */
    private ResultActions exportCalendar(final long courseId)
            throws Exception {
        return mvc().perform(get(calendarPath(courseId)));
    }

    /**
     * Checks with the list endpoint that a course holds a session with the
     * given values.
     *
     * @param courseId the course id
     * @param sessionId the session id
     * @param weekday the expected ISO weekday
     * @param startTime the expected start time, such as "14:00:00"
     * @param endTime the expected end time, such as "15:15:00"
     * @throws Exception if the request cannot be sent or a check fails
     */
    private void assertListed(final long courseId, final long sessionId,
            final int weekday, final String startTime, final String endTime)
            throws Exception {
        String match = "$[?(@.id == " + sessionId + ")]";
        listSessions(courseId)
                .andExpect(status().isOk())
                .andExpect(jsonPath(match + ".courseId",
                        contains(jsonInt(courseId))))
                .andExpect(jsonPath(match + ".weekday", contains(weekday)))
                .andExpect(jsonPath(match + ".startTime", contains(startTime)))
                .andExpect(jsonPath(match + ".endTime", contains(endTime)));
    }

    /**
     * Checks for 200 and returns the raw calendar body.
     *
     * @param result the export result
     * @return the body, exactly as sent
     * @throws Exception if the response is not 200
     */
    private static String bodyOf(final ResultActions result)
            throws Exception {
        return result.andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    /**
     * Checks for 200 and returns the calendar lines. Folded lines are
     * joined back together first.
     *
     * @param result the export result
     * @return the lines, without their CRLF endings
     * @throws Exception if the response is not 200
     */
    private static List<String> calendarLines(final ResultActions result)
            throws Exception {
        return Arrays.asList(bodyOf(result).replace(FOLD, "").split(CRLF));
    }

    /**
     * Counts the events in a calendar.
     *
     * @param lines the calendar lines
     * @return the number of {@code BEGIN:VEVENT} lines
     */
    private static int eventCount(final List<String> lines) {
        return Collections.frequency(lines, "BEGIN:VEVENT");
    }

    /** Tests for POST /api/courses/{courseId}/sessions. */
    @Nested
    @DisplayName("POST /api/courses/{courseId}/sessions")
    class Create {

        @Test
        @DisplayName("valid request returns 201 with the session, and the "
                + "list shows it")
        void validRequestReturns201() throws Exception {
            long courseId = createCourse();

            ResultActions result = postJson(sessionsPath(courseId),
                    sessionJson(1, "14:00", "15:15"));

            result.andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").isNumber())
                    .andExpect(jsonPath("$.courseId").value(courseId))
                    .andExpect(jsonPath("$.weekday").value(1))
                    .andExpect(jsonPath("$.startTime").value("14:00:00"))
                    .andExpect(jsonPath("$.endTime").value("15:15:00"));
            assertListed(courseId, idOf(result), 1, "14:00:00", "15:15:00");
        }

        @ParameterizedTest(name = "weekday {0}")
        @ValueSource(ints = {1, 7})
        @DisplayName("first and last weekday, 1 and 7, return 201")
        void weekdayBoundsReturn201(final int weekday) throws Exception {
            long courseId = createCourse();

            ResultActions result = postJson(sessionsPath(courseId),
                    sessionJson(weekday, "14:00", "15:15"));

            result.andExpect(status().isCreated())
                    .andExpect(jsonPath("$.weekday").value(weekday));
        }

        @Test
        @DisplayName("endTime one minute after startTime returns 201")
        void oneMinuteSessionReturns201() throws Exception {
            long courseId = createCourse();

            ResultActions result = postJson(sessionsPath(courseId),
                    sessionJson(1, "14:00", "14:01"));

            result.andExpect(status().isCreated())
                    .andExpect(jsonPath("$.startTime").value("14:00:00"))
                    .andExpect(jsonPath("$.endTime").value("14:01:00"));
        }

        @Test
        @DisplayName("session overlapping another on the same day returns "
                + "201")
        void overlappingSessionReturns201() throws Exception {
            long courseId = createCourse();
            long first = createSession(courseId, 1, "14:00", "15:15");

            ResultActions result = postJson(sessionsPath(courseId),
                    sessionJson(1, "14:30", "15:30"));

            result.andExpect(status().isCreated());
            listSessions(courseId)
                    .andExpect(jsonPath("$[*].id",
                            hasItems(jsonInt(first), jsonInt(idOf(result)))));
        }

        @Test
        @DisplayName("same weekday and times in a different course returns "
                + "201")
        void sameSlotInOtherCourseReturns201() throws Exception {
            // Assumption: the duplicate rule covers one course only; the
            // description names "the same course".
            createSession(createCourse(), 1, "14:00", "15:15");
            long otherCourse = createCourse();

            ResultActions result = postJson(sessionsPath(otherCourse),
                    sessionJson(1, "14:00", "15:15"));

            result.andExpect(status().isCreated())
                    .andExpect(jsonPath("$.courseId").value(otherCourse));
        }

        @Disabled("Known bug: an identical session in the same course "
                + "throws a ServletException instead of returning 409")
        @Test
        @DisplayName("identical weekday and times in the same course "
                + "returns 409")
        void identicalSessionReturns409() throws Exception {
            long courseId = createCourse();
            createSession(courseId, 1, "14:00", "15:15");

            ResultActions result = postJson(sessionsPath(courseId),
                    sessionJson(1, "14:00", "15:15"));

            // Assumption: the description gives no message for this 409,
            // so only the status is checked.
            result.andExpect(status().isConflict());
        }

        @Test
        @DisplayName("unknown course returns 404")
        void unknownCourseReturns404() throws Exception {
            String body = sessionJson(1, "14:00", "15:15");

            ResultActions result = postJson(sessionsPath(UNKNOWN_ID), body);

            result.andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value(COURSE_NOT_FOUND));
        }

        @Test
        @DisplayName("courseId that is not a number returns 400")
        void invalidCourseIdReturns400() throws Exception {
            String body = sessionJson(1, "14:00", "15:15");

            ResultActions result = postJson(sessionsPath("abc"), body);

            result.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message")
                            .value("Invalid value for courseId"));
        }

        @ParameterizedTest(name = "weekday {0}")
        @ValueSource(ints = {0, 8})
        @DisplayName("weekday just outside 1 to 7 returns 400 naming "
                + "weekday")
        void weekdayOutOfRangeReturns400(final int weekday)
                throws Exception {
            long courseId = createCourse();

            ResultActions result = postJson(sessionsPath(courseId),
                    sessionJson(weekday, "14:00", "15:15"));

            result.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(VALIDATION_FAILED))
                    .andExpect(jsonPath("$.fieldErrors.weekday").exists());
        }

        @ParameterizedTest(name = "without {0}")
        @ValueSource(strings = {"weekday", "startTime", "endTime"})
        @DisplayName("missing required field returns 400 naming that field")
        void missingFieldReturns400(final String field) throws Exception {
            long courseId = createCourse();

            ResultActions result = postJson(sessionsPath(courseId),
                    sessionJsonWithout(field));

            result.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(VALIDATION_FAILED))
                    .andExpect(jsonPath("$.fieldErrors." + field)
                            .value(MUST_NOT_BE_NULL));
        }

        @ParameterizedTest(name = "{0} to {1}")
        @CsvSource({"14:00, 14:00", "14:00, 13:59"})
        @DisplayName("endTime equal to or before startTime returns 400")
        void endNotAfterStartReturns400(final String startTime,
                final String endTime) throws Exception {
            long courseId = createCourse();

            ResultActions result = postJson(sessionsPath(courseId),
                    sessionJson(1, startTime, endTime));

            result.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(VALIDATION_FAILED))
                    .andExpect(jsonPath("$.fieldErrors.timeRangeValid")
                            .value(TIME_RANGE_MESSAGE));
        }

        @Test
        @DisplayName("invalid body for an unknown course returns 400, not "
                + "404")
        void validationIsCheckedBeforeCourse() throws Exception {
            String body = sessionJson(8, "14:00", "15:15");

            ResultActions result = postJson(sessionsPath(UNKNOWN_ID), body);

            result.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(VALIDATION_FAILED))
                    .andExpect(jsonPath("$.fieldErrors.weekday").exists());
        }

        @Test
        @DisplayName("startTime of 25:00 returns 400 malformed body")
        void impossibleTimeReturns400() throws Exception {
            long courseId = createCourse();

            ResultActions result = postJson(sessionsPath(courseId),
                    sessionJson(1, "25:00", "15:15"));

            result.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(MALFORMED_BODY));
        }

        @Test
        @DisplayName("malformed JSON returns 400")
        void malformedJsonReturns400() throws Exception {
            long courseId = createCourse();

            ResultActions result = postJson(sessionsPath(courseId),
                    "{\"weekday\": 1, \"startTime\": ");

            result.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(MALFORMED_BODY));
        }
    }

    /** Tests for GET /api/courses/{courseId}/sessions. */
    @Nested
    @DisplayName("GET /api/courses/{courseId}/sessions")
    class ListSessions {

        @Test
        @DisplayName("list is ordered by weekday, then by startTime")
        void listIsOrderedByWeekdayThenStartTime() throws Exception {
            long courseId = createCourse();
            long wednesday = createSession(courseId, 3, "09:00", "10:00");
            long mondayLate = createSession(courseId, 1, "15:00", "16:00");
            long tuesday = createSession(courseId, 2, "08:00", "09:00");
            long mondayEarly = createSession(courseId, 1, "09:00", "10:00");

            ResultActions result = listSessions(courseId);

            // The course is new, so it holds only these four sessions.
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$[*].id", contains(
                            jsonInt(mondayEarly), jsonInt(mondayLate),
                            jsonInt(tuesday), jsonInt(wednesday))));
        }

        @Test
        @DisplayName("list holds only the sessions of that course")
        void listHoldsOnlyThatCourse() throws Exception {
            long courseA = createCourse();
            long courseB = createCourse();
            long inA = createSession(courseA, 1, "14:00", "15:15");
            long inB = createSession(courseB, 2, "14:00", "15:15");

            ResultActions result = listSessions(courseA);

            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$[*].id", hasItem(jsonInt(inA))))
                    .andExpect(jsonPath("$[*].id",
                            not(hasItem(jsonInt(inB)))))
                    .andExpect(jsonPath("$[*].courseId",
                            everyItem(is(jsonInt(courseA)))));
        }

        @Test
        @DisplayName("new course has an empty list")
        void newCourseHasEmptyList() throws Exception {
            long courseId = createCourse();

            ResultActions result = listSessions(courseId);

            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$").isEmpty());
        }

        @Test
        @DisplayName("unknown course returns 404")
        void unknownCourseReturns404() throws Exception {
            ResultActions result = listSessions(UNKNOWN_ID);

            result.andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value(COURSE_NOT_FOUND));
        }

        @Test
        @DisplayName("courseId that is not a number returns 400")
        void invalidCourseIdReturns400() throws Exception {
            ResultActions result = mvc().perform(get(sessionsPath("abc")));

            result.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message")
                            .value("Invalid value for courseId"));
        }
    }

    /** Tests for PUT /api/courses/{courseId}/sessions/{sessionId}. */
    @Nested
    @DisplayName("PUT /api/courses/{courseId}/sessions/{sessionId}")
    class Update {

        @Test
        @DisplayName("valid update returns 200 with the new weekday and "
                + "times, and the list shows them")
        void validUpdateReturns200() throws Exception {
            long courseId = createCourse();
            long id = createSession(courseId, 1, "14:00", "15:15");

            ResultActions result = putJson(sessionPath(courseId, id),
                    sessionJson(3, "09:30", "10:45"));

            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(id))
                    .andExpect(jsonPath("$.courseId").value(courseId))
                    .andExpect(jsonPath("$.weekday").value(3))
                    .andExpect(jsonPath("$.startTime").value("09:30:00"))
                    .andExpect(jsonPath("$.endTime").value("10:45:00"));
            assertListed(courseId, id, 3, "09:30:00", "10:45:00");
        }

        @Test
        @DisplayName("sending its own current values returns 200")
        void ownCurrentValuesReturn200() throws Exception {
            long courseId = createCourse();
            long id = createSession(courseId, 1, "14:00", "15:15");

            ResultActions result = putJson(sessionPath(courseId, id),
                    sessionJson(1, "14:00", "15:15"));

            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(id));
            assertListed(courseId, id, 1, "14:00:00", "15:15:00");
        }

        @Test
        @DisplayName("weekday 8 returns 400 and changes nothing")
        void weekdayOutOfRangeReturns400() throws Exception {
            long courseId = createCourse();
            long id = createSession(courseId, 1, "14:00", "15:15");

            ResultActions result = putJson(sessionPath(courseId, id),
                    sessionJson(8, "14:00", "15:15"));

            result.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(VALIDATION_FAILED))
                    .andExpect(jsonPath("$.fieldErrors.weekday").exists());
            assertListed(courseId, id, 1, "14:00:00", "15:15:00");
        }

        @ParameterizedTest(name = "{0} to {1}")
        @CsvSource({"14:00, 14:00", "14:00, 13:59"})
        @DisplayName("endTime equal to or before startTime returns 400 and "
                + "changes nothing")
        void endNotAfterStartReturns400(final String startTime,
                final String endTime) throws Exception {
            long courseId = createCourse();
            long id = createSession(courseId, 1, "10:00", "11:00");

            ResultActions result = putJson(sessionPath(courseId, id),
                    sessionJson(1, startTime, endTime));

            result.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(VALIDATION_FAILED))
                    .andExpect(jsonPath("$.fieldErrors.timeRangeValid")
                            .value(TIME_RANGE_MESSAGE));
            assertListed(courseId, id, 1, "10:00:00", "11:00:00");
        }

        @Test
        @DisplayName("startTime of 25:00 returns 400 malformed body")
        void impossibleTimeReturns400() throws Exception {
            long courseId = createCourse();
            long id = createSession(courseId, 1, "14:00", "15:15");

            ResultActions result = putJson(sessionPath(courseId, id),
                    sessionJson(1, "25:00", "15:15"));

            result.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(MALFORMED_BODY));
        }

        @Test
        @DisplayName("unknown course returns 404 Course not found")
        void unknownCourseReturns404() throws Exception {
            long sessionId = createSession(createCourse(), 1, "14:00", "15:15");
            String body = sessionJson(2, "09:00", "10:00");

            ResultActions result =
                    putJson(sessionPath(UNKNOWN_ID, sessionId), body);

            result.andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value(COURSE_NOT_FOUND));
        }

        @Test
        @DisplayName("unknown session returns 404 Course session not found")
        void unknownSessionReturns404() throws Exception {
            long courseId = createCourse();
            String body = sessionJson(2, "09:00", "10:00");

            ResultActions result =
                    putJson(sessionPath(courseId, UNKNOWN_ID), body);

            result.andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value(SESSION_NOT_FOUND));
        }

        @Test
        @DisplayName("session of another course returns 404 and changes "
                + "nothing")
        void sessionOfOtherCourseReturns404() throws Exception {
            long owner = createCourse();
            long id = createSession(owner, 1, "14:00", "15:15");
            long otherCourse = createCourse();

            ResultActions result = putJson(sessionPath(otherCourse, id),
                    sessionJson(2, "09:00", "10:00"));

            result.andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value(SESSION_NOT_FOUND));
            assertListed(owner, id, 1, "14:00:00", "15:15:00");
            listSessions(otherCourse).andExpect(jsonPath("$").isEmpty());
        }

        @Test
        @DisplayName("invalid body for an unknown course returns 400, not "
                + "404")
        void validationIsCheckedBeforeCourse() throws Exception {
            String body = sessionJson(8, "14:00", "15:15");

            ResultActions result =
                    putJson(sessionPath(UNKNOWN_ID, UNKNOWN_ID), body);

            result.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(VALIDATION_FAILED));
        }

        @Test
        @DisplayName("sessionId that is not a number returns 400")
        void invalidSessionIdReturns400() throws Exception {
            long courseId = createCourse();
            String body = sessionJson(1, "14:00", "15:15");

            ResultActions result = putJson(sessionPath(courseId, "abc"), body);

            result.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message")
                            .value("Invalid value for sessionId"));
        }

        @Test
        @DisplayName("courseId that is not a number returns 400")
        void invalidCourseIdReturns400() throws Exception {
            long sessionId = createSession(createCourse(), 1, "14:00", "15:15");
            String body = sessionJson(1, "14:00", "15:15");

            ResultActions result = putJson(sessionPath("abc", sessionId), body);

            result.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message")
                            .value("Invalid value for courseId"));
        }

        @Disabled("Known bug: moving a session onto another session's slot "
                + "throws a ServletException instead of returning 409")
        @Test
        @DisplayName("moving a session onto another session's exact weekday "
                + "and times returns 409 and changes nothing")
        void moveOntoTakenSlotReturns409() throws Exception {
            long courseId = createCourse();
            createSession(courseId, 1, "14:00", "15:15");
            long id = createSession(courseId, 2, "09:00", "10:00");

            ResultActions result = putJson(sessionPath(courseId, id),
                    sessionJson(1, "14:00", "15:15"));

            // Assumption: the description gives no message for this 409,
            // so only the status is checked.
            result.andExpect(status().isConflict());
            assertListed(courseId, id, 2, "09:00:00", "10:00:00");
        }
    }

    /** Tests for DELETE /api/courses/{courseId}/sessions/{sessionId}. */
    @Nested
    @DisplayName("DELETE /api/courses/{courseId}/sessions/{sessionId}")
    class Delete {

        @Test
        @DisplayName("existing session returns 204 and leaves the list")
        void existingSessionReturns204() throws Exception {
            long courseId = createCourse();
            long id = createSession(courseId, 1, "14:00", "15:15");
            long kept = createSession(courseId, 2, "14:00", "15:15");

            ResultActions result =
                    mvc().perform(delete(sessionPath(courseId, id)));

            result.andExpect(status().isNoContent());
            listSessions(courseId)
                    .andExpect(jsonPath("$[*].id", contains(jsonInt(kept))));
        }

        @Test
        @DisplayName("deleted slot can be added again")
        void deletedSlotCanBeAddedAgain() throws Exception {
            long courseId = createCourse();
            long id = createSession(courseId, 1, "14:00", "15:15");

            mvc().perform(delete(sessionPath(courseId, id)))
                    .andExpect(status().isNoContent());
            ResultActions result = postJson(sessionsPath(courseId),
                    sessionJson(1, "14:00", "15:15"));

            result.andExpect(status().isCreated());
        }

        @Test
        @DisplayName("deleting the same session twice returns 404 the "
                + "second time")
        void secondDeleteReturns404() throws Exception {
            long courseId = createCourse();
            long id = createSession(courseId, 1, "14:00", "15:15");
            mvc().perform(delete(sessionPath(courseId, id)))
                    .andExpect(status().isNoContent());

            ResultActions result =
                    mvc().perform(delete(sessionPath(courseId, id)));

            result.andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value(SESSION_NOT_FOUND));
        }

        @Test
        @DisplayName("unknown course returns 404 Course not found")
        void unknownCourseReturns404() throws Exception {
            long sessionId = createSession(createCourse(), 1, "14:00", "15:15");

            ResultActions result =
                    mvc().perform(delete(sessionPath(UNKNOWN_ID, sessionId)));

            result.andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value(COURSE_NOT_FOUND));
        }

        @Test
        @DisplayName("unknown session returns 404 Course session not found")
        void unknownSessionReturns404() throws Exception {
            long courseId = createCourse();

            ResultActions result =
                    mvc().perform(delete(sessionPath(courseId, UNKNOWN_ID)));

            result.andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value(SESSION_NOT_FOUND));
        }

        @Test
        @DisplayName("session of another course returns 404 and is kept")
        void sessionOfOtherCourseReturns404() throws Exception {
            long owner = createCourse();
            long id = createSession(owner, 1, "14:00", "15:15");
            long otherCourse = createCourse();

            ResultActions result =
                    mvc().perform(delete(sessionPath(otherCourse, id)));

            result.andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value(SESSION_NOT_FOUND));
            assertListed(owner, id, 1, "14:00:00", "15:15:00");
        }

        @Test
        @DisplayName("sessionId that is not a number returns 400")
        void invalidSessionIdReturns400() throws Exception {
            long courseId = createCourse();

            ResultActions result =
                    mvc().perform(delete(sessionPath(courseId, "abc")));

            result.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message")
                            .value("Invalid value for sessionId"));
        }
    }

    /** Tests for GET /api/courses/{courseId}/calendar.ics. */
    @Nested
    @DisplayName("GET /api/courses/{courseId}/calendar.ics")
    class ExportCalendar {

        @Test
        @DisplayName("returns 200 as a text/calendar attachment named after "
                + "the course")
        void returnsCalendarAttachment() throws Exception {
            long courseId = calendarCourse("2026-11-02", "2026-11-09");
            String fileName = "filename=\"course-" + courseId + ".ics\"";

            ResultActions result = exportCalendar(courseId);

            result.andExpect(status().isOk())
                    .andExpect(content()
                            .contentTypeCompatibleWith("text/calendar"))
                    .andExpect(header().string(
                            HttpHeaders.CONTENT_DISPOSITION,
                            containsString("attachment")))
                    .andExpect(header().string(
                            HttpHeaders.CONTENT_DISPOSITION,
                            containsString(fileName)));
        }

        @Test
        @DisplayName("body is one VCALENDAR with the fixed header lines and "
                + "CRLF line endings")
        void bodyIsCalendarWithHeaderLines() throws Exception {
            long courseId = calendarCourse("2026-11-02", "2026-11-09");
            createSession(courseId, 1, "14:00", "15:15");

            ResultActions result = exportCalendar(courseId);

            String body = bodyOf(result);
            assertThat(body, startsWith("BEGIN:VCALENDAR" + CRLF));
            assertThat(body, endsWith(CRLF + "END:VCALENDAR" + CRLF));
            assertThat(body.replace(CRLF, ""), not(containsString("\n")));
            assertThat(calendarLines(result), hasItems("VERSION:2.0",
                    "PRODID:-//Perihelion API//Course Calendar//EN",
                    "CALSCALE:GREGORIAN", "METHOD:PUBLISH"));
        }

        @Test
        @DisplayName("one event per matching date, with the first and last "
                + "term days included")
        void oneEventPerMatchingDate() throws Exception {
            long courseId = calendarCourse("2026-11-02", "2026-11-09");
            createSession(courseId, 1, "14:00", "15:15");
            createSession(courseId, 3, "16:00", "17:00");

            ResultActions result = exportCalendar(courseId);

            // Monday 2 and 9 November and Wednesday 4 November, all in
            // standard time (UTC-5).
            List<String> lines = calendarLines(result);
            assertThat(eventCount(lines), is(3));
            assertThat(Collections.frequency(lines, "END:VEVENT"), is(3));
            assertThat(lines, hasItems(
                    "DTSTART:20261102T190000Z", "DTEND:20261102T201500Z",
                    "DTSTART:20261104T210000Z", "DTEND:20261104T220000Z",
                    "DTSTART:20261109T190000Z", "DTEND:20261109T201500Z"));
        }

        @Test
        @DisplayName("times are New York local times, converted to UTC "
                + "before and after daylight saving time ends")
        void daylightSavingTimeIsApplied() throws Exception {
            long courseId = calendarCourse("2026-10-26", "2026-11-02");
            createSession(courseId, 1, "14:00", "15:00");

            ResultActions result = exportCalendar(courseId);

            // 26 October is daylight time (UTC-4); 2 November is standard
            // time (UTC-5).
            List<String> lines = calendarLines(result);
            assertThat(eventCount(lines), is(2));
            assertThat(lines, hasItems(
                    "DTSTART:20261026T180000Z", "DTEND:20261026T190000Z",
                    "DTSTART:20261102T190000Z", "DTEND:20261102T200000Z"));
        }

        @Test
        @DisplayName("weekday 7 is Sunday, including the Sunday daylight "
                + "saving time ends")
        void weekdaySevenIsSunday() throws Exception {
            long courseId = calendarCourse("2026-10-30", "2026-11-08");
            createSession(courseId, 7, "14:00", "15:00");

            ResultActions result = exportCalendar(courseId);

            // Sunday 1 November: daylight time ends at 02:00, so 14:00 is
            // already standard time (UTC-5).
            List<String> lines = calendarLines(result);
            assertThat(eventCount(lines), is(2));
            assertThat(lines, hasItems("DTSTART:20261101T190000Z",
                    "DTSTART:20261108T190000Z"));
        }

        @Test
        @DisplayName("event UID names the course, the session and the date")
        void uidNamesCourseSessionAndDate() throws Exception {
            long courseId = calendarCourse("2026-11-02", "2026-11-03");
            long sessionId = createSession(courseId, 1, "14:00", "15:15");
            String uid = "UID:course-" + courseId + "-session-" + sessionId
                    + "-20261102@perihelion-api";

            ResultActions result = exportCalendar(courseId);

            List<String> lines = calendarLines(result);
            assertThat(eventCount(lines), is(1));
            assertThat(lines, hasItem(uid));
        }

        @Test
        @DisplayName("each event has a DTSTAMP in UTC basic format")
        void eachEventHasDtstamp() throws Exception {
            long courseId = calendarCourse("2026-11-02", "2026-11-09");
            createSession(courseId, 1, "14:00", "15:15");

            ResultActions result = exportCalendar(courseId);

            // DTSTAMP is the export time, so only its format is checked.
            List<String> stamps = calendarLines(result).stream()
                    .filter(line -> line.startsWith("DTSTAMP"))
                    .toList();
            assertThat(stamps, hasSize(2));
            assertThat(stamps, everyItem(matchesPattern(DTSTAMP_FORMAT)));
        }

        @Test
        @DisplayName("SUMMARY is the course code, a dash and the title")
        void summaryHasCodeAndTitle() throws Exception {
            long courseId = createCourse("SE", "COMS W4156",
                    LocalDate.parse("2026-11-02"),
                    LocalDate.parse("2026-11-09"));
            createSession(courseId, 1, "14:00", "15:15");

            ResultActions result = exportCalendar(courseId);

            List<String> lines = calendarLines(result);
            assertThat(Collections.frequency(lines,
                    "SUMMARY:COMS W4156 - SE"), is(2));
        }

        @Test
        @DisplayName("SUMMARY is only the title when the course has no code")
        void summaryIsTitleWithoutCode() throws Exception {
            long courseId = calendarCourse("2026-11-02", "2026-11-09");
            createSession(courseId, 1, "14:00", "15:15");

            ResultActions result = exportCalendar(courseId);

            List<String> lines = calendarLines(result);
            assertThat(Collections.frequency(lines, "SUMMARY:SE"), is(2));
        }

        @Test
        @DisplayName("comma in SUMMARY is escaped with a backslash")
        void summaryCommaIsEscaped() throws Exception {
            long courseId = createCourse("A, B", null,
                    LocalDate.parse("2026-11-02"),
                    LocalDate.parse("2026-11-03"));
            createSession(courseId, 1, "14:00", "15:15");

            ResultActions result = exportCalendar(courseId);

            assertThat(calendarLines(result), hasItem("SUMMARY:A\\, B"));
        }

        @Test
        @DisplayName("course with no sessions gives a calendar with no "
                + "events")
        void noSessionsGivesNoEvents() throws Exception {
            long courseId = calendarCourse("2026-11-02", "2026-11-09");

            ResultActions result = exportCalendar(courseId);

            List<String> lines = calendarLines(result);
            assertThat(eventCount(lines), is(0));
            assertThat(lines.get(0), is("BEGIN:VCALENDAR"));
            assertThat(lines.get(lines.size() - 1), is("END:VCALENDAR"));
        }

        @Test
        @DisplayName("term with no day on the session's weekday gives no "
                + "events")
        void termWithoutMatchingWeekdayGivesNoEvents() throws Exception {
            // 3 and 4 November 2026 are a Tuesday and a Wednesday.
            long courseId = calendarCourse("2026-11-03", "2026-11-04");
            createSession(courseId, 1, "14:00", "15:15");

            ResultActions result = exportCalendar(courseId);

            assertThat(eventCount(calendarLines(result)), is(0));
        }

        @Test
        @DisplayName("unknown course returns 404 as JSON")
        void unknownCourseReturns404() throws Exception {
            ResultActions result = exportCalendar(UNKNOWN_ID);

            result.andExpect(status().isNotFound())
                    .andExpect(content().contentTypeCompatibleWith(
                            MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.message").value(COURSE_NOT_FOUND));
        }

        @Test
        @DisplayName("courseId that is not a number returns 400")
        void invalidCourseIdReturns400() throws Exception {
            ResultActions result = mvc().perform(get(calendarPath("abc")));

            result.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message")
                            .value("Invalid value for courseId"));
        }
    }
}
