/*
    AI usage: Work plan tests: Grok 4.7, ~238K tokens
 */
package com.perihelionApi.workplan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
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
import com.perihelionApi.enrollment.Enrollment;
import com.perihelionApi.enrollment.EnrollmentRepository;
import com.perihelionApi.task.TaskRepository;

@ExtendWith(MockitoExtension.class)
class WorkPlanServiceTest {

    private static final Instant CREATED = Instant.parse("2026-09-01T12:00:00Z");

    @Mock
    private WorkPlanRepository workPlanRepository;
    @Mock
    private EnrollmentRepository enrollmentRepository;
    @Mock
    private TaskRepository taskRepository;
    @InjectMocks
    private WorkPlanService workPlanService;

    private Enrollment enrollment;
    private WorkPlan workPlan;
    private final WorkPlanRequest request = new WorkPlanRequest(2L,
            LocalDate.of(2026, 10, 1), LocalDate.of(2026, 12, 15),
            new BigDecimal("3.5"));

    @BeforeEach
    void setUp() {
        enrollment = new Enrollment();
        enrollment.setId(2L);
        workPlan = new WorkPlan();
        workPlan.setId(1L);
        workPlan.setEnrollment(enrollment);
        workPlan.setRangeStart(request.rangeStart());
        workPlan.setRangeEnd(request.rangeEnd());
        workPlan.setAvailableHoursPerDay(request.availableHoursPerDay());
        workPlan.setCreatedAt(CREATED);
    }

    @Test
    void createPersistsFieldsAndStampsCreatedAt() {
        when(enrollmentRepository.findById(2L)).thenReturn(Optional.of(enrollment));
        when(workPlanRepository.save(any(WorkPlan.class))).thenAnswer(invocation -> {
            WorkPlan saved = invocation.getArgument(0);
            assertEquals(enrollment, saved.getEnrollment());
            assertEquals(request.rangeStart(), saved.getRangeStart());
            assertEquals(request.rangeEnd(), saved.getRangeEnd());
            assertEquals(request.availableHoursPerDay(), saved.getAvailableHoursPerDay());
            assertNotNull(saved.getCreatedAt());
            saved.setId(1L);
            return saved;
        });

        WorkPlanResponse response = workPlanService.create(request);

        assertEquals(1L, response.id());
        assertEquals(2L, response.enrollmentId());
        assertNotNull(response.createdAt());
    }

    @Test
    void createRejectsMissingEnrollmentWithoutSaving() {
        when(enrollmentRepository.findById(2L)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> workPlanService.create(request));

        assertEquals("Enrollment not found", exception.getMessage());
        verify(workPlanRepository, never()).save(any());
    }

    @Test
    void getReturnsMappedWorkPlan() {
        when(workPlanRepository.findById(1L)).thenReturn(Optional.of(workPlan));

        assertEquals(WorkPlanResponse.from(workPlan), workPlanService.get(1L));
    }

    @Test
    void getRejectsMissingWorkPlan() {
        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> workPlanService.get(99L));
        assertEquals("Work plan not found", exception.getMessage());
    }

    @Test
    void listWithoutFilterUsesAllWorkPlans() {
        when(workPlanRepository.findAllByOrderByRangeStartAscIdAsc())
                .thenReturn(List.of(workPlan));

        assertEquals(List.of(WorkPlanResponse.from(workPlan)), workPlanService.list(null));
    }

    @Test
    void listFiltersByEnrollment() {
        when(workPlanRepository.findByEnrollmentIdOrderByRangeStartAscIdAsc(2L))
                .thenReturn(List.of(workPlan));

        assertEquals(List.of(WorkPlanResponse.from(workPlan)), workPlanService.list(2L));
        verify(workPlanRepository, never()).findAllByOrderByRangeStartAscIdAsc();
    }

    @Test
    void updateReplacesFieldsAndPreservesCreatedAt() {
        Enrollment other = new Enrollment();
        other.setId(5L);
        WorkPlanRequest replacement = new WorkPlanRequest(5L,
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30), BigDecimal.ONE);
        when(workPlanRepository.findById(1L)).thenReturn(Optional.of(workPlan));
        when(enrollmentRepository.findById(5L)).thenReturn(Optional.of(other));
        when(workPlanRepository.save(workPlan)).thenReturn(workPlan);

        WorkPlanResponse response = workPlanService.update(1L, replacement);

        assertEquals(new WorkPlanResponse(1L, 5L, replacement.rangeStart(),
                replacement.rangeEnd(), BigDecimal.ONE, CREATED), response);
    }

    @Test
    void updateRejectsMissingWorkPlanWithoutTouchingEnrollment() {
        assertThrows(NotFoundException.class, () -> workPlanService.update(99L, request));
        verify(enrollmentRepository, never()).findById(any());
        verify(workPlanRepository, never()).save(any());
    }

    @Test
    void deleteRemovesWorkPlanWithoutTasks() {
        when(workPlanRepository.existsById(1L)).thenReturn(true);
        when(taskRepository.existsByWorkPlanId(1L)).thenReturn(false);

        workPlanService.delete(1L);

        verify(workPlanRepository).deleteById(1L);
    }

    @Test
    void deleteRejectsWorkPlanWithTasks() {
        when(workPlanRepository.existsById(1L)).thenReturn(true);
        when(taskRepository.existsByWorkPlanId(1L)).thenReturn(true);

        ConflictException exception = assertThrows(ConflictException.class,
                () -> workPlanService.delete(1L));

        assertEquals("Work plan cannot be deleted while tasks exist", exception.getMessage());
        verify(workPlanRepository, never()).deleteById(any());
    }

    @Test
    void deleteRejectsMissingWorkPlan() {
        when(workPlanRepository.existsById(99L)).thenReturn(false);

        assertThrows(NotFoundException.class, () -> workPlanService.delete(99L));
        verify(taskRepository, never()).existsByWorkPlanId(any());
        verify(workPlanRepository, never()).deleteById(any());
    }
}
