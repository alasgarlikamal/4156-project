package com.perihelionApi.coursesession;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.perihelionApi.common.NotFoundException;
import com.perihelionApi.course.CourseRequest;
import com.perihelionApi.course.CourseService;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CourseSessionIntegrationTest {

    @Autowired
    private CourseSessionService sessionService;

    @Autowired
    private CourseService courseService;

    @Test
    void persistsListsUpdatesDeletesAndExportsCourseSessions() {
        Long courseId = courseService.create(new CourseRequest("COMS W4156",
                "Software Engineering", LocalDate.of(2026, 11, 2),
                LocalDate.of(2026, 11, 9))).id();

        CourseSessionResponse later = sessionService.create(courseId,
                new CourseSessionRequest(3, LocalTime.of(16, 0),
                        LocalTime.of(17, 0)));
        CourseSessionResponse earlier = sessionService.create(courseId,
                new CourseSessionRequest(1, LocalTime.of(14, 0),
                        LocalTime.of(15, 15)));

        assertEquals(List.of(earlier, later), sessionService.list(courseId));

        String calendar = sessionService.exportCalendar(courseId);
        assertEquals(3, count(calendar, "BEGIN:VEVENT"));
        assertTrue(calendar.contains("DTSTART:20261102T190000Z"));
        assertTrue(calendar.contains("DTSTART:20261104T210000Z"));
        assertTrue(calendar.contains("DTEND:20261109T201500Z"));

        CourseSessionResponse updated = sessionService.update(courseId,
                later.id(), new CourseSessionRequest(5, LocalTime.of(9, 0),
                        LocalTime.of(10, 0)));
        assertEquals(5, updated.weekday());

        sessionService.delete(courseId, earlier.id());
        assertEquals(List.of(updated), sessionService.list(courseId));
        assertThrows(NotFoundException.class,
                () -> sessionService.delete(courseId, earlier.id()));
    }

    @Test
    void rejectsUpdatingSessionOwnedByAnotherCourse() {
        Long ownerCourseId = courseService.create(new CourseRequest(
                "COMS W4156", "Software Engineering",
                LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 9))).id();
        Long otherCourseId = courseService.create(new CourseRequest(
                "COMS W4111", "Database Systems",
                LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 9))).id();
        CourseSessionResponse session = sessionService.create(ownerCourseId,
                new CourseSessionRequest(1, LocalTime.of(14, 0),
                        LocalTime.of(15, 0)));

        assertThrows(NotFoundException.class,
                () -> sessionService.update(otherCourseId, session.id(),
                        new CourseSessionRequest(2, LocalTime.of(14, 0),
                                LocalTime.of(15, 0))));
    }

    private static int count(final String text, final String substring) {
        return (text.length() - text.replace(substring, "").length())
                / substring.length();
    }
}
