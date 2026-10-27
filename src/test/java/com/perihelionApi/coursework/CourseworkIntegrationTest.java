/*
    AI usage: Coursework tests: Grok 4.7, ~238K tokens
 */
package com.perihelionApi.coursework;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.perihelionApi.common.ConflictException;
import com.perihelionApi.common.NotFoundException;
import com.perihelionApi.course.CourseRequest;
import com.perihelionApi.course.CourseService;
import com.perihelionApi.enrollment.EnrollmentRequest;
import com.perihelionApi.enrollment.EnrollmentService;
import com.perihelionApi.submission.SubmissionRequest;
import com.perihelionApi.submission.SubmissionResponse;
import com.perihelionApi.submission.SubmissionService;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CourseworkIntegrationTest {

    private static final Instant EARLY = Instant.parse("2026-10-15T23:59:00Z");
    private static final Instant LATE = Instant.parse("2026-12-10T12:00:00Z");

    @Autowired
    private CourseService courseService;
    @Autowired
    private EnrollmentService enrollmentService;
    @Autowired
    private CourseworkService courseworkService;
    @Autowired
    private SubmissionService submissionService;

    @Test
    void courseworkAndSubmissionsRoundTripWithCrossEntityRules() {
        Long courseId = courseService.create(new CourseRequest("COMS W4156",
                "Advanced Software Engineering", LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 12, 20))).id();
        Long otherCourseId = courseService.create(new CourseRequest(null, "Databases",
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 12, 20))).id();
        Long enrollmentId = enrollmentService.create(
                new EnrollmentRequest(courseId, UUID.randomUUID())).id();

        CourseworkResponse exam = courseworkService.create(new CourseworkRequest(courseId,
                CourseworkKind.EXAM, "Final exam", LATE, new BigDecimal("3.0")));
        CourseworkResponse assignment = courseworkService.create(new CourseworkRequest(
                courseId, CourseworkKind.ASSIGNMENT, "Proposal", EARLY, new BigDecimal("4.5")));
        CourseworkResponse otherCoursework = courseworkService.create(new CourseworkRequest(
                otherCourseId, CourseworkKind.ASSIGNMENT, "SQL project", EARLY, BigDecimal.ONE));

        assertEquals(CourseworkKind.EXAM, courseworkService.get(exam.id()).kind());
        assertEquals(List.of(assignment, exam), courseworkService.list(courseId, null));
        assertEquals(List.of(exam), courseworkService.list(courseId, CourseworkKind.EXAM));
        assertEquals(List.of(assignment, otherCoursework),
                courseworkService.list(null, CourseworkKind.ASSIGNMENT));
        assertEquals(3, courseworkService.list(null, null).size());

        assertThrows(NotFoundException.class, () -> courseworkService.create(
                new CourseworkRequest(999L, CourseworkKind.EXAM, "Orphan", LATE, BigDecimal.ONE)));

        SubmissionResponse submission = submissionService.create(
                new SubmissionRequest(enrollmentId, assignment.id(), EARLY.minusSeconds(60)));
        assertEquals(assignment.id(), submission.courseworkId());
        assertEquals(List.of(submission), submissionService.list(null, assignment.id()));

        assertThrows(ConflictException.class, () -> submissionService.create(
                new SubmissionRequest(enrollmentId, assignment.id(), null)));
        assertThrows(ConflictException.class, () -> submissionService.create(
                new SubmissionRequest(enrollmentId, otherCoursework.id(), null)));
        assertThrows(NotFoundException.class, () -> submissionService.create(
                new SubmissionRequest(enrollmentId, 999L, null)));

        assertThrows(ConflictException.class, () -> courseworkService.delete(assignment.id()));
        assertThrows(ConflictException.class, () -> enrollmentService.delete(enrollmentId));

        CourseworkResponse updated = courseworkService.update(assignment.id(),
                new CourseworkRequest(courseId, CourseworkKind.ASSIGNMENT, "Proposal v2",
                        LATE.plusSeconds(3600), BigDecimal.TEN));
        assertEquals("Proposal v2", updated.title());
        assertEquals(List.of(exam, updated), courseworkService.list(courseId, null));

        submissionService.delete(submission.id());
        courseworkService.delete(assignment.id());
        courseworkService.delete(exam.id());
        assertThrows(NotFoundException.class, () -> courseworkService.get(exam.id()));
        assertEquals(List.of(), courseworkService.list(courseId, null));
    }
}
