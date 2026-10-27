/*
    AI usage: Task CRUD: Grok 4.7, ~238K tokens
 */
package com.perihelionApi.task;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.perihelionApi.common.ConflictException;
import com.perihelionApi.common.NotFoundException;
import com.perihelionApi.coursework.Coursework;
import com.perihelionApi.coursework.CourseworkRepository;
import com.perihelionApi.enrollment.Enrollment;
import com.perihelionApi.enrollment.EnrollmentRepository;
import com.perihelionApi.workplan.WorkPlan;
import com.perihelionApi.workplan.WorkPlanRepository;

/**
 * Business logic for tasks.
 */
@Service
@Transactional
public class TaskService {

    /** Persistence for tasks. */
    private final TaskRepository taskRepository;

    /** Persistence for work plans, used to resolve the owning plan. */
    private final WorkPlanRepository workPlanRepository;

    /** Persistence for coursework, used to resolve the target work. */
    private final CourseworkRepository courseworkRepository;

    /** Persistence for enrollments, used to resolve the owner. */
    private final EnrollmentRepository enrollmentRepository;

    /**
     * Creates the service.
     *
     * @param taskRepository       the task repository
     * @param workPlanRepository   the work plan repository
     * @param courseworkRepository the coursework repository
     * @param enrollmentRepository the enrollment repository
     */
    public TaskService(final TaskRepository taskRepository,
            final WorkPlanRepository workPlanRepository,
            final CourseworkRepository courseworkRepository,
            final EnrollmentRepository enrollmentRepository) {
        this.taskRepository = taskRepository;
        this.workPlanRepository = workPlanRepository;
        this.courseworkRepository = courseworkRepository;
        this.enrollmentRepository = enrollmentRepository;
    }

    /**
     * Creates and persists a task.
     *
     * @param request the task values
     * @return the created task
     * @throws NotFoundException if the work plan, coursework, or enrollment
     *                           does not exist
     * @throws ConflictException if the task is inconsistent with its work
     *                           plan or enrollment
     */
    public TaskResponse create(final TaskRequest request) {
        Task task = new Task();
        apply(task, request);
        return TaskResponse.from(taskRepository.save(task));
    }

    /**
     * Returns a single task.
     *
     * @param id the task id
     * @return the task
     * @throws NotFoundException if no task has the given id
     */
    @Transactional(readOnly = true)
    public TaskResponse get(final Long id) {
        return TaskResponse.from(findTask(id));
    }

    /**
     * Lists tasks matching every supplied filter.
     *
     * @param enrollmentId an enrollment id, or null
     * @param workPlanId   a work plan id, or null
     * @param courseworkId a coursework id, or null
     * @return the matching tasks ordered by target date and id
     */
    @Transactional(readOnly = true)
    public List<TaskResponse> list(final Long enrollmentId,
            final Long workPlanId, final Long courseworkId) {
        return taskRepository.search(enrollmentId, workPlanId, courseworkId)
                .stream().map(TaskResponse::from).toList();
    }

    /**
     * Replaces a task.
     *
     * @param id      the task id
     * @param request the new task values
     * @return the updated task
     * @throws NotFoundException if the task, work plan, coursework, or
     *                           enrollment does not exist
     * @throws ConflictException if the task is inconsistent with its work
     *                           plan or enrollment
     */
    public TaskResponse update(final Long id, final TaskRequest request) {
        Task task = findTask(id);
        apply(task, request);
        return TaskResponse.from(taskRepository.save(task));
    }

    /**
     * Deletes a task.
     *
     * @param id the task id
     * @throws NotFoundException if no task has the given id
     */
    public void delete(final Long id) {
        if (!taskRepository.existsById(id)) {
            throw new NotFoundException("Task not found");
        }
        taskRepository.deleteById(id);
    }

    private Task findTask(final Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Task not found"));
    }

    private void apply(final Task task, final TaskRequest request) {
        WorkPlan workPlan = workPlanRepository
                .findById(request.workPlanId())
                .orElseThrow(
                        () -> new NotFoundException("Work plan not found"));
        Coursework coursework = courseworkRepository
                .findById(request.courseworkId())
                .orElseThrow(
                        () -> new NotFoundException("Coursework not found"));
        Enrollment enrollment = enrollmentRepository
                .findById(request.enrollmentId())
                .orElseThrow(
                        () -> new NotFoundException("Enrollment not found"));

        if (!Objects.equals(workPlan.getEnrollment().getId(),
                enrollment.getId())) {
            throw new ConflictException(
                    "Task enrollment must match the work plan's enrollment");
        }
        if (!Objects.equals(coursework.getCourse().getId(),
                enrollment.getCourse().getId())) {
            throw new ConflictException(
                    "Coursework must belong to the enrollment's course");
        }
        LocalDate target = request.targetDate();
        if (target.isBefore(workPlan.getRangeStart())
                || target.isAfter(workPlan.getRangeEnd())) {
            throw new ConflictException(
                    "targetDate must fall within the work plan's range");
        }

        task.setWorkPlan(workPlan);
        task.setCoursework(coursework);
        task.setEnrollment(enrollment);
        task.setTitle(request.title());
        task.setTargetDate(target);
        task.setEstimatedWorkHours(request.estimatedWorkHours());
    }

}
