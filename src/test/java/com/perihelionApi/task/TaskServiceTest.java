/*
    AI usage: Task tests: Grok 4.7, ~238K tokens
 */
package com.perihelionApi.task;

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

import com.perihelionApi.coursework.Coursework;
import com.perihelionApi.coursework.CourseworkRepository;
import com.perihelionApi.common.ConflictException;
import com.perihelionApi.common.NotFoundException;
import com.perihelionApi.course.Course;
import com.perihelionApi.enrollment.Enrollment;
import com.perihelionApi.enrollment.EnrollmentRepository;
import com.perihelionApi.workplan.WorkPlan;
import com.perihelionApi.workplan.WorkPlanRepository;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;
    @Mock
    private WorkPlanRepository workPlanRepository;
    @Mock
    private CourseworkRepository courseworkRepository;
    @Mock
    private EnrollmentRepository enrollmentRepository;
    @InjectMocks
    private TaskService taskService;

    private Course course;
    private Enrollment enrollment;
    private WorkPlan workPlan;
    private Coursework coursework;
    private Task task;
    private final TaskRequest request = new TaskRequest(10L, 20L, 30L,
            "Read chapter 3", LocalDate.of(2026, 10, 15), new BigDecimal("2.5"));

    @BeforeEach
    void setUp() {
        course = new Course();
        course.setId(1L);
        enrollment = new Enrollment();
        enrollment.setId(30L);
        enrollment.setCourse(course);
        workPlan = new WorkPlan();
        workPlan.setId(10L);
        workPlan.setEnrollment(enrollment);
        workPlan.setRangeStart(LocalDate.of(2026, 10, 1));
        workPlan.setRangeEnd(LocalDate.of(2026, 12, 15));
        coursework = new Coursework();
        coursework.setId(20L);
        coursework.setCourse(course);
        task = new Task();
        task.setId(1L);
        task.setWorkPlan(workPlan);
        task.setCoursework(coursework);
        task.setEnrollment(enrollment);
        task.setTitle(request.title());
        task.setTargetDate(request.targetDate());
        task.setEstimatedWorkHours(request.estimatedWorkHours());
    }

    private void stubReferences() {
        when(workPlanRepository.findById(10L)).thenReturn(Optional.of(workPlan));
        when(courseworkRepository.findById(20L)).thenReturn(Optional.of(coursework));
        when(enrollmentRepository.findById(30L)).thenReturn(Optional.of(enrollment));
    }

    @Test
    void createPersistsFieldsAndMapsResponse() {
        stubReferences();
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> {
            Task saved = invocation.getArgument(0);
            assertEquals(workPlan, saved.getWorkPlan());
            assertEquals(coursework, saved.getCoursework());
            assertEquals(enrollment, saved.getEnrollment());
            assertEquals(request.title(), saved.getTitle());
            assertEquals(request.targetDate(), saved.getTargetDate());
            assertEquals(request.estimatedWorkHours(), saved.getEstimatedWorkHours());
            saved.setId(1L);
            return saved;
        });

        assertEquals(TaskResponse.from(task), taskService.create(request));
    }

    @Test
    void createRejectsMissingWorkPlan() {
        when(workPlanRepository.findById(10L)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> taskService.create(request));

        assertEquals("Work plan not found", exception.getMessage());
        verify(taskRepository, never()).save(any());
    }

    @Test
    void createRejectsMissingCoursework() {
        when(workPlanRepository.findById(10L)).thenReturn(Optional.of(workPlan));
        when(courseworkRepository.findById(20L)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> taskService.create(request));

        assertEquals("Coursework not found", exception.getMessage());
        verify(taskRepository, never()).save(any());
    }

    @Test
    void createRejectsMissingEnrollment() {
        when(workPlanRepository.findById(10L)).thenReturn(Optional.of(workPlan));
        when(courseworkRepository.findById(20L)).thenReturn(Optional.of(coursework));
        when(enrollmentRepository.findById(30L)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> taskService.create(request));

        assertEquals("Enrollment not found", exception.getMessage());
        verify(taskRepository, never()).save(any());
    }

    @Test
    void createRejectsEnrollmentThatDoesNotOwnWorkPlan() {
        Enrollment otherEnrollment = new Enrollment();
        otherEnrollment.setId(99L);
        otherEnrollment.setCourse(course);
        workPlan.setEnrollment(otherEnrollment);
        stubReferences();

        ConflictException exception = assertThrows(ConflictException.class,
                () -> taskService.create(request));

        assertEquals("Task enrollment must match the work plan's enrollment",
                exception.getMessage());
        verify(taskRepository, never()).save(any());
    }

    @Test
    void createRejectsCourseworkFromAnotherCourse() {
        Course otherCourse = new Course();
        otherCourse.setId(2L);
        coursework.setCourse(otherCourse);
        stubReferences();

        ConflictException exception = assertThrows(ConflictException.class,
                () -> taskService.create(request));

        assertEquals("Coursework must belong to the enrollment's course", exception.getMessage());
        verify(taskRepository, never()).save(any());
    }

    @Test
    void createRejectsTargetDateBeforeRange() {
        stubReferences();
        TaskRequest early = new TaskRequest(10L, 20L, 30L, "Early",
                LocalDate.of(2026, 9, 30), BigDecimal.ONE);

        ConflictException exception = assertThrows(ConflictException.class,
                () -> taskService.create(early));

        assertEquals("targetDate must fall within the work plan's range", exception.getMessage());
        verify(taskRepository, never()).save(any());
    }

    @Test
    void createRejectsTargetDateAfterRange() {
        stubReferences();
        TaskRequest late = new TaskRequest(10L, 20L, 30L, "Late",
                LocalDate.of(2026, 12, 16), BigDecimal.ONE);

        assertThrows(ConflictException.class, () -> taskService.create(late));
        verify(taskRepository, never()).save(any());
    }

    @Test
    void createAcceptsTargetDateOnRangeBoundaries() {
        stubReferences();
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        taskService.create(new TaskRequest(10L, 20L, 30L, "Start",
                workPlan.getRangeStart(), BigDecimal.ONE));
        taskService.create(new TaskRequest(10L, 20L, 30L, "End",
                workPlan.getRangeEnd(), BigDecimal.ONE));

        verify(taskRepository, org.mockito.Mockito.times(2)).save(any());
    }

    @Test
    void getReturnsMappedTask() {
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        assertEquals(TaskResponse.from(task), taskService.get(1L));
    }

    @Test
    void getRejectsMissingTask() {
        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> taskService.get(99L));
        assertEquals("Task not found", exception.getMessage());
    }

    @Test
    void listDelegatesFiltersToRepository() {
        when(taskRepository.search(30L, null, 20L)).thenReturn(List.of(task));

        assertEquals(List.of(TaskResponse.from(task)), taskService.list(30L, null, 20L));
    }

    @Test
    void updateReplacesFieldsAndPreservesId() {
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        stubReferences();
        when(taskRepository.save(task)).thenReturn(task);
        TaskRequest replacement = new TaskRequest(10L, 20L, 30L, "Rewritten",
                LocalDate.of(2026, 11, 1), BigDecimal.TEN);

        TaskResponse response = taskService.update(1L, replacement);

        assertEquals(new TaskResponse(1L, 10L, 20L, 30L, "Rewritten",
                LocalDate.of(2026, 11, 1), BigDecimal.TEN), response);
    }

    @Test
    void updateRejectsMissingTaskWithoutResolvingReferences() {
        assertThrows(NotFoundException.class, () -> taskService.update(99L, request));
        verify(workPlanRepository, never()).findById(any());
        verify(taskRepository, never()).save(any());
    }

    @Test
    void updateLeavesTaskUnchangedWhenRuleFails() {
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        stubReferences();
        TaskResponse original = TaskResponse.from(task);
        TaskRequest outOfRange = new TaskRequest(10L, 20L, 30L, "Late",
                LocalDate.of(2027, 1, 1), BigDecimal.ONE);

        assertThrows(ConflictException.class, () -> taskService.update(1L, outOfRange));

        assertEquals(original, TaskResponse.from(task));
        verify(taskRepository, never()).save(any());
    }

    @Test
    void deleteRemovesExistingTask() {
        when(taskRepository.existsById(1L)).thenReturn(true);

        taskService.delete(1L);

        verify(taskRepository).deleteById(1L);
    }

    @Test
    void deleteRejectsMissingTask() {
        when(taskRepository.existsById(99L)).thenReturn(false);

        assertThrows(NotFoundException.class, () -> taskService.delete(99L));
        verify(taskRepository, never()).deleteById(any());
    }
}
