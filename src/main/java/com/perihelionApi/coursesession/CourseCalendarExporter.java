// Wrote file with github copilot used 6 credits
package com.perihelionApi.coursesession;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import com.perihelionApi.course.Course;

/**
 * Creates an iCalendar file with one event per course meeting date.
 */
final class CourseCalendarExporter {

    /** Timezone used for all stored meeting times. */
    private static final ZoneId CALENDAR_ZONE =
            ZoneId.of("America/New_York");

    /** UTC format required for iCalendar date-time properties. */
    private static final DateTimeFormatter UTC_DATE_TIME =
            DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");

    private CourseCalendarExporter() {
    }

    /**
     * Exports every matching weekday from term start through term end.
     *
     * @param course   the course
     * @param sessions its weekly meeting times
     * @return RFC 5545 calendar text using CRLF line endings
     */
    static String export(final Course course,
            final List<CourseSession> sessions) {
        StringBuilder calendar = new StringBuilder();
        appendLine(calendar, "BEGIN:VCALENDAR");
        appendLine(calendar, "VERSION:2.0");
        appendLine(calendar, "PRODID:-//Perihelion API//Course Calendar//EN");
        appendLine(calendar, "CALSCALE:GREGORIAN");
        appendLine(calendar, "METHOD:PUBLISH");

        String summary = course.getTitle();
        if (course.getCode() != null && !course.getCode().isBlank()) {
            summary = course.getCode() + " - " + summary;
        }
        String stamp = UTC_DATE_TIME.format(
                ZonedDateTime.ofInstant(Instant.now(), ZoneId.of("UTC")));

        for (CourseSession session : sessions) {
            appendSessionEvents(calendar, course, session, summary, stamp);
        }

        appendLine(calendar, "END:VCALENDAR");
        return calendar.toString();
    }

    private static void appendSessionEvents(final StringBuilder calendar,
            final Course course, final CourseSession session,
            final String summary, final String stamp) {
        LocalDate date = course.getTermStart();
        while (!date.isAfter(course.getTermEnd())) {
            if (date.getDayOfWeek().getValue() == session.getWeekday()) {
                appendEvent(calendar, course, session, date, summary, stamp);
            }
            date = date.plusDays(1);
        }
    }

    private static void appendEvent(final StringBuilder calendar,
            final Course course, final CourseSession session,
            final LocalDate date, final String summary, final String stamp) {
        ZonedDateTime start = ZonedDateTime.of(date,
                session.getStartTime(), CALENDAR_ZONE);
        ZonedDateTime end = ZonedDateTime.of(date,
                session.getEndTime(), CALENDAR_ZONE);
        appendLine(calendar, "BEGIN:VEVENT");
        appendLine(calendar, "UID:course-" + course.getId()
                + "-session-" + session.getId() + "-"
                + date.toString().replace("-", "") + "@perihelion-api");
        appendLine(calendar, "DTSTAMP:" + stamp);
        appendLine(calendar, "DTSTART:"
                + UTC_DATE_TIME.format(start.withZoneSameInstant(
                        ZoneId.of("UTC"))));
        appendLine(calendar, "DTEND:"
                + UTC_DATE_TIME.format(end.withZoneSameInstant(
                        ZoneId.of("UTC"))));
        appendLine(calendar, "SUMMARY:" + escapeText(summary));
        appendLine(calendar, "END:VEVENT");
    }

    private static String escapeText(final String value) {
        return value.replace("\\", "\\\\")
                .replace(";", "\\;")
                .replace(",", "\\,")
                .replace("\r\n", "\\n")
                .replace("\r", "\\n")
                .replace("\n", "\\n");
    }

    private static void appendLine(final StringBuilder calendar,
            final String line) {
        int octets = 0;
        for (int offset = 0; offset < line.length();) {
            int codePoint = line.codePointAt(offset);
            String character = new String(Character.toChars(codePoint));
            int characterOctets =
                    character.getBytes(StandardCharsets.UTF_8).length;
            if (octets + characterOctets > 75) {
                calendar.append("\r\n ");
                octets = 1;
            }
            calendar.append(character);
            octets += characterOctets;
            offset += Character.charCount(codePoint);
        }
        calendar.append("\r\n");
    }
}
