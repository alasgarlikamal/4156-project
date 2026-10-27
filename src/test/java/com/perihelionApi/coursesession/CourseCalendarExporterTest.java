// Wrote file with github copilot used 7.5 credits
package com.perihelionApi.coursesession;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.perihelionApi.course.Course;

class CourseCalendarExporterTest {

    @Test
    void exportsEachMatchingDateWithNewYorkDaylightSavingApplied()
            throws Exception {
        Course course = course("COMS W4156", "Software, Design");
        CourseSession session = session(1L, 1, "14:00", "15:15");

        String calendar = CourseCalendarExporter.export(course,
                List.of(session));

        assertEquals(3, count(calendar, "BEGIN:VEVENT"));
        assertTrue(calendar.contains("DTSTART:20260302T190000Z"));
        assertTrue(calendar.contains("DTEND:20260302T201500Z"));
        assertTrue(calendar.contains("DTSTART:20260309T180000Z"));
        assertTrue(calendar.contains("DTEND:20260316T191500Z"));
        assertTrue(calendar.contains("SUMMARY:COMS W4156 - "
                + "Software\\, Design"));
        assertTrue(calendar.endsWith("END:VCALENDAR\r\n"));
        for (String line : calendar.split("\r\n")) {
            assertTrue(line.getBytes(java.nio.charset.StandardCharsets.UTF_8)
                    .length <= 75);
        }
    }

    @Test
    void omitsNullAndBlankCourseCodes() {
        Course withoutCode = course(null, "Software Design");
        Course withBlankCode = course("  ", "Software Design");
        CourseSession session = session(1L, 1, "14:00", "15:15");

        String nullCodeCalendar = CourseCalendarExporter.export(withoutCode,
                List.of(session));
        String blankCodeCalendar = CourseCalendarExporter.export(withBlankCode,
                List.of(session));

        assertTrue(nullCodeCalendar.contains("SUMMARY:Software Design"));
        assertTrue(blankCodeCalendar.contains("SUMMARY:Software Design"));
    }

    @Test
    void foldsLongCalendarLines() {
        String longTitle = "A".repeat(80);
        String calendar = CourseCalendarExporter.export(
                course(null, longTitle),
                List.of(session(1L, 1, "14:00", "15:15")));

        assertTrue(calendar.contains("\r\n "));
        assertTrue(calendar.replace("\r\n ", "")
                .contains("SUMMARY:" + longTitle));
        for (String line : calendar.split("\r\n")) {
            assertTrue(line.getBytes(java.nio.charset.StandardCharsets.UTF_8)
                    .length <= 75);
        }
    }

    private static Course course(final String code, final String title) {
        Course course = new Course();
        course.setId(1L);
        course.setCode(code);
        course.setTitle(title);
        course.setTermStart(LocalDate.of(2026, 3, 2));
        course.setTermEnd(LocalDate.of(2026, 3, 16));
        return course;
    }

    private static CourseSession session(final Long id, final int weekday,
            final String start, final String end) {
        CourseSession session = new CourseSession();
        session.setId(id);
        session.setWeekday(weekday);
        session.setStartTime(LocalTime.parse(start));
        session.setEndTime(LocalTime.parse(end));
        return session;
    }

    private static int count(final String text, final String substring) {
        return (text.length() - text.replace(substring, "").length())
                / substring.length();
    }
}
