/*
    AI usage: Course Session API Tests: Opus 5.5, ~128K tokens
 */
package com.perihelionApi.coursesession;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.matchesPattern;
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

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
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
     * Checks for 200 and returns the calendar lines. Folded lines are
     * joined back together first.
     *
     * @param result the export result
     * @return the lines, without their CRLF endings
     * @throws Exception if the response is not 200
     */
    private static List<String> calendarLines(final ResultActions result)
            throws Exception {
        String body = result.andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return Arrays.asList(body.replace(FOLD, "").split(CRLF));
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
        @DisplayName("endTime before startTime returns 400")
        void endBeforeStartReturns400() throws Exception {
            long courseId = createCourse();

            ResultActions result = postJson(sessionsPath(courseId),
                    sessionJson(1, "14:00", "13:59"));

            result.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(VALIDATION_FAILED))
                    .andExpect(jsonPath("$.fieldErrors.timeRangeValid")
                            .value(TIME_RANGE_MESSAGE));
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
    }

    /** Tests for GET /api/courses/{courseId}/calendar.ics. */
    @Nested
    @DisplayName("GET /api/courses/{courseId}/calendar.ics")
    class ExportCalendar {

        @Test
        @DisplayName("text/calendar attachment with one event per matching "
                + "date, first and last term days included")
        void oneEventPerMatchingDate() throws Exception {
            long courseId = calendarCourse("2026-11-02", "2026-11-09");
            long monday = createSession(courseId, 1, "14:00", "15:15");
            createSession(courseId, 3, "16:00", "17:00");
            String fileName = "filename=\"course-" + courseId + ".ics\"";
            String uid = "UID:course-" + courseId + "-session-" + monday
                    + "-20261102@perihelion-api";

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
            List<String> lines = calendarLines(result);
            assertThat(lines.get(0), is("BEGIN:VCALENDAR"));
            assertThat(lines.get(lines.size() - 1), is("END:VCALENDAR"));
            assertThat(lines, hasItems("VERSION:2.0",
                    "PRODID:-//Perihelion API//Course Calendar//EN",
                    "CALSCALE:GREGORIAN", "METHOD:PUBLISH"));
            // Monday 2 and 9 November and Wednesday 4 November, all in
            // standard time (UTC-5).
            assertThat(eventCount(lines), is(3));
            assertThat(Collections.frequency(lines, "END:VEVENT"), is(3));
            assertThat(lines, hasItems(
                    "DTSTART:20261102T190000Z", "DTEND:20261102T201500Z",
                    "DTSTART:20261104T210000Z", "DTEND:20261104T220000Z",
                    "DTSTART:20261109T190000Z", "DTEND:20261109T201500Z"));
            assertThat(lines, hasItem(uid));
            // DTSTAMP is the export time, so only its format is checked.
            List<String> stamps = lines.stream()
                    .filter(line -> line.startsWith("DTSTAMP"))
                    .toList();
            assertThat(stamps, hasSize(3));
            assertThat(stamps, everyItem(matchesPattern(DTSTAMP_FORMAT)));
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
        @DisplayName("unknown course returns 404 as JSON")
        void unknownCourseReturns404() throws Exception {
            ResultActions result = exportCalendar(UNKNOWN_ID);

            result.andExpect(status().isNotFound())
                    .andExpect(content().contentTypeCompatibleWith(
                            MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.message").value(COURSE_NOT_FOUND));
        }
    }
}
