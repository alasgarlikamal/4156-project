/*
    AI usage: Work plan tests: Grok 4.7, ~238K tokens
 */
package com.perihelionApi.workplan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
import com.perihelionApi.coursework.CourseworkKind;
import com.perihelionApi.coursework.CourseworkRequest;
import com.perihelionApi.coursework.CourseworkService;
import com.perihelionApi.enrollment.EnrollmentRequest;
import com.perihelionApi.enrollment.EnrollmentService;
import com.perihelionApi.task.TaskRequest;
import com.perihelionApi.task.TaskResponse;
import com.perihelionApi.task.TaskService;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class WorkPlanIntegrationTest {

    private static final Instant DEADLINE = Instant.parse("2026-11-01T23:59:00Z");

    @Autowired
    private CourseService courseService;
    @Autowired
    private EnrollmentService enrollmentService;
    @Autowired
    private CourseworkService courseworkService;
    @Autowired
    private WorkPlanService workPlanService;
    @Autowired
    private TaskService taskService;

    @Test
    void plansAndTasksRoundTripWithCrossEntityRules() {
        Long courseId = courseService.create(new CourseRequest(null, "Software Engineering",
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 12, 20))).id();
        Long otherCourseId = courseService.create(new CourseRequest(null, "Databases",
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 12, 20))).id();
        Long enrollmentId = enrollmentService.create(
                new EnrollmentRequest(courseId, UUID.randomUUID())).id();
        Long otherEnrollmentId = enrollmentService.create(
                new EnrollmentRequest(otherCourseId, UUID.randomUUID())).id();
        Long courseworkId = courseworkService.create(new CourseworkRequest(courseId,
                CourseworkKind.ASSIGNMENT, "Proposal", DEADLINE, BigDecimal.TEN)).id();
        Long otherCourseworkId = courseworkService.create(new CourseworkRequest(otherCourseId,
                CourseworkKind.ASSIGNMENT, "SQL project", DEADLINE, BigDecimal.ONE)).id();

        WorkPlanResponse later = workPlanService.create(new WorkPlanRequest(enrollmentId,
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 12, 15), new BigDecimal("2.0")));
        WorkPlanResponse earlier = workPlanService.create(new WorkPlanRequest(enrollmentId,
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31), new BigDecimal("3.5")));
        workPlanService.create(new WorkPlanRequest(otherEnrollmentId,
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31), BigDecimal.ONE));

        assertNotNull(earlier.createdAt());
        assertEquals(earlier, workPlanService.get(earlier.id()));
        assertEquals(List.of(earlier, later), workPlanService.list(enrollmentId));
        assertEquals(3, workPlanService.list(null).size());

        TaskResponse task = taskService.create(new TaskRequest(earlier.id(), courseworkId,
                enrollmentId, "Outline", LocalDate.of(2026, 10, 20), new BigDecimal("1.5")));
        TaskResponse firstTask = taskService.create(new TaskRequest(earlier.id(), courseworkId,
                enrollmentId, "Research", LocalDate.of(2026, 10, 5), BigDecimal.ONE));

        assertEquals(List.of(firstTask, task), taskService.list(null, earlier.id(), null));
        assertEquals(List.of(firstTask, task), taskService.list(enrollmentId, null, courseworkId));
        assertEquals(List.of(), taskService.list(otherEnrollmentId, null, null));

        assertThrows(ConflictException.class, () -> taskService.create(new TaskRequest(
                earlier.id(), courseworkId, otherEnrollmentId, "Wrong owner",
                LocalDate.of(2026, 10, 20), BigDecimal.ONE)));
        assertThrows(ConflictException.class, () -> taskService.create(new TaskRequest(
                earlier.id(), otherCourseworkId, enrollmentId, "Wrong course",
                LocalDate.of(2026, 10, 20), BigDecimal.ONE)));
        assertThrows(ConflictException.class, () -> taskService.create(new TaskRequest(
                earlier.id(), courseworkId, enrollmentId, "Out of range",
                LocalDate.of(2026, 11, 2), BigDecimal.ONE)));

        assertThrows(ConflictException.class, () -> workPlanService.delete(earlier.id()));
        assertThrows(ConflictException.class, () -> courseworkService.delete(courseworkId));
        assertThrows(ConflictException.class, () -> enrollmentService.delete(enrollmentId));

        WorkPlanResponse updated = workPlanService.update(earlier.id(), new WorkPlanRequest(
                enrollmentId, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 11, 30),
                new BigDecimal("4.0")));
        assertEquals(earlier.createdAt(), updated.createdAt());
        assertEquals(LocalDate.of(2026, 11, 30), updated.rangeEnd());

        taskService.delete(task.id());
        taskService.delete(firstTask.id());
        assertEquals(List.of(), taskService.list(null, earlier.id(), null));
        workPlanService.delete(earlier.id());
        assertThrows(NotFoundException.class, () -> workPlanService.get(earlier.id()));
        assertEquals(List.of(later), workPlanService.list(enrollmentId));
    }
}
