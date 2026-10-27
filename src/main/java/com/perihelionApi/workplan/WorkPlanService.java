/*
    AI usage: Work plan CRUD: Grok 4.7, ~238K tokens
 */
package com.perihelionApi.workplan;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.perihelionApi.common.ConflictException;
import com.perihelionApi.common.NotFoundException;
import com.perihelionApi.enrollment.Enrollment;
import com.perihelionApi.enrollment.EnrollmentRepository;
import com.perihelionApi.task.TaskRepository;

/**
 * Business logic for work plans.
 */
@Service
@Transactional
public class WorkPlanService {

    /** Persistence for work plans. */
    private final WorkPlanRepository workPlanRepository;

    /** Persistence for enrollments, used to resolve the owner. */
    private final EnrollmentRepository enrollmentRepository;

    /** Persistence for tasks, used to guard deletes. */
    private final TaskRepository taskRepository;

    /**
     * Creates the service.
     *
     * @param workPlanRepository   the work plan repository
     * @param enrollmentRepository the enrollment repository
     * @param taskRepository       the task repository
     */
    public WorkPlanService(final WorkPlanRepository workPlanRepository,
            final EnrollmentRepository enrollmentRepository,
            final TaskRepository taskRepository) {
        this.workPlanRepository = workPlanRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.taskRepository = taskRepository;
    }

    /**
     * Creates and persists a work plan.
     *
     * @param request the work plan values
     * @return the created work plan
     * @throws NotFoundException if the enrollment does not exist
     */
    public WorkPlanResponse create(final WorkPlanRequest request) {
        WorkPlan workPlan = new WorkPlan();
        apply(workPlan, request);
        workPlan.setCreatedAt(Instant.now());
        return WorkPlanResponse.from(workPlanRepository.save(workPlan));
    }

    /**
     * Returns a single work plan.
     *
     * @param id the work plan id
     * @return the work plan
     * @throws NotFoundException if no work plan has the given id
     */
    @Transactional(readOnly = true)
    public WorkPlanResponse get(final Long id) {
        return WorkPlanResponse.from(findWorkPlan(id));
    }

    /**
     * Lists work plans, optionally filtered by enrollment.
     *
     * @param enrollmentId an enrollment id, or null
     * @return the matching work plans ordered by range start and id
     */
    @Transactional(readOnly = true)
    public List<WorkPlanResponse> list(final Long enrollmentId) {
        List<WorkPlan> workPlans = enrollmentId == null
                ? workPlanRepository.findAllByOrderByRangeStartAscIdAsc()
                : workPlanRepository
                        .findByEnrollmentIdOrderByRangeStartAscIdAsc(
                                enrollmentId);
        return workPlans.stream().map(WorkPlanResponse::from).toList();
    }

    /**
     * Replaces a work plan. The creation time is preserved.
     *
     * @param id      the work plan id
     * @param request the new work plan values
     * @return the updated work plan
     * @throws NotFoundException if the work plan or enrollment does not
     *                           exist
     */
    public WorkPlanResponse update(final Long id,
            final WorkPlanRequest request) {
        WorkPlan workPlan = findWorkPlan(id);
        apply(workPlan, request);
        return WorkPlanResponse.from(workPlanRepository.save(workPlan));
    }

    /**
     * Deletes a work plan.
     *
     * @param id the work plan id
     * @throws NotFoundException if no work plan has the given id
     * @throws ConflictException if the work plan still has tasks
     */
    public void delete(final Long id) {
        if (!workPlanRepository.existsById(id)) {
            throw new NotFoundException("Work plan not found");
        }
        if (taskRepository.existsByWorkPlanId(id)) {
            throw new ConflictException(
                    "Work plan cannot be deleted while tasks exist");
        }
        workPlanRepository.deleteById(id);
    }

    private WorkPlan findWorkPlan(final Long id) {
        return workPlanRepository.findById(id).orElseThrow(
                () -> new NotFoundException("Work plan not found"));
    }

    private void apply(final WorkPlan workPlan,
            final WorkPlanRequest request) {
        Enrollment enrollment = enrollmentRepository
                .findById(request.enrollmentId())
                .orElseThrow(
                        () -> new NotFoundException("Enrollment not found"));
        workPlan.setEnrollment(enrollment);
        workPlan.setRangeStart(request.rangeStart());
        workPlan.setRangeEnd(request.rangeEnd());
        workPlan.setAvailableHoursPerDay(request.availableHoursPerDay());
    }

}
