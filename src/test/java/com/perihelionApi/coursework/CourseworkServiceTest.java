/*
    AI usage: Coursework tests: Grok 4.7, ~238K tokens
 */
package com.perihelionApi.coursework;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.perihelionApi.common.ConflictException;
import com.perihelionApi.common.NotFoundException;
import com.perihelionApi.course.Course;
import com.perihelionApi.course.CourseRepository;
import com.perihelionApi.submission.SubmissionRepository;
import com.perihelionApi.task.TaskRepository;

@ExtendWith(MockitoExtension.class)
class CourseworkServiceTest {

    private static final Instant DEADLINE = Instant.parse("2026-11-01T23:59:00Z");

    @Mock
    private CourseworkRepository courseworkRepository;
    @Mock
    private CourseRepository courseRepository;
    @Mock
    private SubmissionRepository submissionRepository;
    @Mock
    private TaskRepository taskRepository;
    @InjectMocks
    private CourseworkService courseworkService;

    private Course course;
    private Coursework coursework;
    private final CourseworkRequest request = new CourseworkRequest(2L,
            CourseworkKind.ASSIGNMENT, "Project proposal", DEADLINE, new BigDecimal("4.5"));

    @BeforeEach
    void setUp() {
        course = new Course();
        course.setId(2L);
        coursework = new Coursework();
        coursework.setId(1L);
        coursework.setCourse(course);
        coursework.setKind(request.kind());
        coursework.setTitle(request.title());
        coursework.setDeadline(request.deadline());
        coursework.setEstimatedEffortHours(request.estimatedEffortHours());
    }

    @Test
    void createPersistsFieldsAndMapsResponse() {
        when(courseRepository.findById(2L)).thenReturn(Optional.of(course));
        when(courseworkRepository.save(any(Coursework.class))).thenAnswer(invocation -> {
            Coursework saved = invocation.getArgument(0);
            assertEquals(course, saved.getCourse());
            assertEquals(CourseworkKind.ASSIGNMENT, saved.getKind());
            assertEquals(request.title(), saved.getTitle());
            assertEquals(DEADLINE, saved.getDeadline());
            assertEquals(request.estimatedEffortHours(), saved.getEstimatedEffortHours());
            saved.setId(1L);
            return saved;
        });

        assertEquals(CourseworkResponse.from(coursework), courseworkService.create(request));
    }

    @Test
    void createRejectsMissingCourseWithoutSaving() {
        assertThrows(NotFoundException.class, () -> courseworkService.create(request));
        verify(courseworkRepository, never()).save(any());
    }

    @Test
    void getReturnsMappedCoursework() {
        when(courseworkRepository.findById(1L)).thenReturn(Optional.of(coursework));
        assertEquals(CourseworkResponse.from(coursework), courseworkService.get(1L));
    }

    @Test
    void getRejectsMissingCoursework() {
        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> courseworkService.get(99L));
        assertEquals("Coursework not found", exception.getMessage());
    }

    @Test
    void listDelegatesFiltersToRepository() {
        when(courseworkRepository.search(2L, CourseworkKind.EXAM)).thenReturn(List.of(coursework));
        assertEquals(List.of(CourseworkResponse.from(coursework)),
                courseworkService.list(2L, CourseworkKind.EXAM));
    }

    @Test
    void updateReplacesAllFieldsAndPreservesId() {
        Course replacementCourse = new Course();
        replacementCourse.setId(3L);
        CourseworkRequest replacement = new CourseworkRequest(3L, CourseworkKind.EXAM,
                "Final", DEADLINE.plusSeconds(86400), BigDecimal.TEN);
        when(courseworkRepository.findById(1L)).thenReturn(Optional.of(coursework));
        when(courseRepository.findById(3L)).thenReturn(Optional.of(replacementCourse));
        when(courseworkRepository.save(coursework)).thenReturn(coursework);

        assertEquals(new CourseworkResponse(1L, 3L, CourseworkKind.EXAM, "Final",
                replacement.deadline(), BigDecimal.TEN),
                courseworkService.update(1L, replacement));
    }

    @Test
    void updateRejectsMissingCourseworkWithoutSaving() {
        assertThrows(NotFoundException.class, () -> courseworkService.update(99L, request));
        verify(courseRepository, never()).findById(any());
        verify(courseworkRepository, never()).save(any());
    }

    @Test
    void deleteRemovesCourseworkWithoutDependents() {
        when(courseworkRepository.existsById(1L)).thenReturn(true);
        when(submissionRepository.existsByCourseworkId(1L)).thenReturn(false);
        when(taskRepository.existsByCourseworkId(1L)).thenReturn(false);

        courseworkService.delete(1L);

        verify(courseworkRepository).deleteById(1L);
    }

    @Test
    void deleteRejectsCourseworkWithSubmissions() {
        when(courseworkRepository.existsById(1L)).thenReturn(true);
        when(submissionRepository.existsByCourseworkId(1L)).thenReturn(true);

        ConflictException exception = assertThrows(ConflictException.class,
                () -> courseworkService.delete(1L));

        assertEquals("Coursework cannot be deleted while submissions exist", exception.getMessage());
        verify(courseworkRepository, never()).deleteById(any());
    }

    @Test
    void deleteRejectsCourseworkWithTasks() {
        when(courseworkRepository.existsById(1L)).thenReturn(true);
        when(submissionRepository.existsByCourseworkId(1L)).thenReturn(false);
        when(taskRepository.existsByCourseworkId(1L)).thenReturn(true);

        ConflictException exception = assertThrows(ConflictException.class,
                () -> courseworkService.delete(1L));

        assertEquals("Coursework cannot be deleted while tasks exist", exception.getMessage());
        verify(courseworkRepository, never()).deleteById(any());
    }

    @Test
    void deleteRejectsMissingCoursework() {
        assertThrows(NotFoundException.class, () -> courseworkService.delete(99L));
        verify(courseworkRepository, never()).deleteById(any());
    }
}
