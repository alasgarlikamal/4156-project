package com.perihelionApi.assignment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.perihelionApi.common.NotFoundException;
import com.perihelionApi.course.Course;
import com.perihelionApi.course.CourseRepository;

@ExtendWith(MockitoExtension.class)
class AssignmentServiceTest {

    @Mock
    private AssignmentRepository assignmentRepository;
    @Mock
    private CourseRepository courseRepository;
    @InjectMocks
    private AssignmentService assignmentService;

    private Course course;
    private Assignment assignment;
    private final AssignmentRequest request = new AssignmentRequest(
            2L, "Project proposal", LocalDate.of(2026, 11, 1),
            new BigDecimal("4.5"));

    @BeforeEach
    void setUp() {
        course = new Course();
        course.setId(2L);
        assignment = new Assignment();
        assignment.setId(1L);
        assignment.setCourse(course);
        assignment.setTitle(request.title());
        assignment.setDueDate(request.dueDate());
        assignment.setEstimatedWorkHours(request.estimatedWorkHours());
    }

    @Test
    void createPersistsFieldsAndMapsResponse() {
        when(courseRepository.findById(2L)).thenReturn(Optional.of(course));
        when(assignmentRepository.save(any(Assignment.class)))
                .thenAnswer(invocation -> {
                    Assignment saved = invocation.getArgument(0);
                    assertEquals(course, saved.getCourse());
                    assertEquals(request.title(), saved.getTitle());
                    assertEquals(request.dueDate(), saved.getDueDate());
                    assertEquals(request.estimatedWorkHours(),
                            saved.getEstimatedWorkHours());
                    saved.setId(1L);
                    return saved;
                });
        assertEquals(AssignmentResponse.from(assignment),
                assignmentService.create(request));
    }

    @Test
    void createRejectsMissingCourseWithoutSaving() {
        assertThrows(NotFoundException.class,
                () -> assignmentService.create(request));
        verify(assignmentRepository, never()).save(any());
    }

    @Test
    void getReturnsMappedAssignment() {
        when(assignmentRepository.findById(1L))
                .thenReturn(Optional.of(assignment));
        assertEquals(AssignmentResponse.from(assignment),
                assignmentService.get(1L));
    }

    @Test
    void getRejectsMissingAssignment() {
        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> assignmentService.get(99L));
        assertEquals("Assignment not found", exception.getMessage());
    }

    @Test
    void listWithoutFilterUsesAllAssignments() {
        when(assignmentRepository.findAllByOrderByDueDateAscIdAsc())
                .thenReturn(List.of(assignment));
        assertEquals(List.of(AssignmentResponse.from(assignment)),
                assignmentService.list(null));
    }

    @Test
    void listFiltersByCourse() {
        when(assignmentRepository.findByCourseIdOrderByDueDateAscIdAsc(2L))
                .thenReturn(List.of(assignment));
        assertEquals(List.of(AssignmentResponse.from(assignment)),
                assignmentService.list(2L));
        verify(assignmentRepository, never()).findAllByOrderByDueDateAscIdAsc();
    }

    @Test
    void updateReplacesAllFieldsAndPreservesId() {
        Course replacementCourse = new Course();
        replacementCourse.setId(3L);
        AssignmentRequest replacement = new AssignmentRequest(3L, "Final",
                LocalDate.of(2026, 12, 1), BigDecimal.TEN);
        when(assignmentRepository.findById(1L))
                .thenReturn(Optional.of(assignment));
        when(courseRepository.findById(3L))
                .thenReturn(Optional.of(replacementCourse));
        when(assignmentRepository.save(assignment)).thenReturn(assignment);
        assertEquals(new AssignmentResponse(1L, 3L, "Final",
                replacement.dueDate(), BigDecimal.TEN),
                assignmentService.update(1L, replacement));
        verify(assignmentRepository).save(assignment);
    }

    @Test
    void updateRejectsMissingAssignmentWithoutSaving() {
        assertThrows(NotFoundException.class,
                () -> assignmentService.update(99L, request));
        verify(courseRepository, never()).findById(any());
        verify(assignmentRepository, never()).save(any());
    }

    @Test
    void updateRejectsMissingCourseWithoutChangingAssignment() {
        when(assignmentRepository.findById(1L))
                .thenReturn(Optional.of(assignment));
        AssignmentResponse original = AssignmentResponse.from(assignment);
        assertThrows(NotFoundException.class,
                () -> assignmentService.update(1L, request));
        assertEquals(original, AssignmentResponse.from(assignment));
        verify(assignmentRepository, never()).save(any());
    }

    @Test
    void deleteRemovesExistingAssignment() {
        when(assignmentRepository.existsById(1L)).thenReturn(true);
        assignmentService.delete(1L);
        verify(assignmentRepository).deleteById(1L);
    }

    @Test
    void deleteRejectsMissingAssignment() {
        assertThrows(NotFoundException.class,
                () -> assignmentService.delete(99L));
        verify(assignmentRepository, never()).deleteById(any());
    }
}
