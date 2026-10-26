package com.perihelionApi.assignment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.LocalDate;
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
class AssignmentIntegrationTest {

    @Autowired
    private AssignmentService assignmentService;
    @Autowired
    private CourseService courseService;

    @Test
    void persistsFiltersOrdersUpdatesAndDeletesAssignments() {
        LocalDate deadline = LocalDate.of(2026, 11, 1);
        Long courseId = courseService.create(new CourseRequest(null,
                "Software Engineering", LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 12, 20))).id();
        Long otherCourseId = courseService.create(new CourseRequest(null,
                "Databases", LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 12, 20))).id();
        AssignmentResponse later = assignmentService.create(
                new AssignmentRequest(courseId, "Final", deadline.plusDays(5),
                        BigDecimal.TEN));
        AssignmentResponse earlier = assignmentService.create(
                new AssignmentRequest(courseId, "Proposal", deadline,
                        new BigDecimal("4.5")));
        assignmentService.create(new AssignmentRequest(otherCourseId,
                "SQL project", deadline, BigDecimal.ONE));

        assertEquals(earlier, assignmentService.get(earlier.id()));
        assertEquals(List.of(earlier, later), assignmentService.list(courseId));
        assertEquals(3, assignmentService.list(null).size());

        AssignmentResponse updated = assignmentService.update(later.id(),
                new AssignmentRequest(otherCourseId, "Revised final",
                        deadline.minusDays(1), BigDecimal.ZERO));
        assertEquals(otherCourseId, updated.courseId());
        assertEquals("Revised final", updated.title());
        assertEquals(deadline.minusDays(1), updated.dueDate());
        assertEquals(0, updated.estimatedWorkHours().compareTo(BigDecimal.ZERO));
        assertEquals(List.of(earlier), assignmentService.list(courseId));

        assignmentService.delete(earlier.id());
        assertEquals(List.of(), assignmentService.list(courseId));
        assertThrows(NotFoundException.class,
                () -> assignmentService.get(earlier.id()));
    }
}
