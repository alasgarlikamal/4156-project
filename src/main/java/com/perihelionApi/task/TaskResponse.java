/*
    AI usage: Task CRUD: Grok 4.7, ~238K tokens
 */
package com.perihelionApi.task;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Task representation returned to clients.
 *
 * @param id                 the task id
 * @param workPlanId         the work plan id
 * @param courseworkId       the coursework id
 * @param enrollmentId       the enrollment id
 * @param title              the task title
 * @param targetDate         the day the task should be finished
 * @param estimatedWorkHours estimated effort in hours
 */
public record TaskResponse(Long id, Long workPlanId, Long courseworkId,
        Long enrollmentId, String title, LocalDate targetDate,
        BigDecimal estimatedWorkHours) {

    /**
     * Builds a response from an entity.
     *
     * @param task the entity
     * @return the response
     */
    public static TaskResponse from(final Task task) {
        return new TaskResponse(task.getId(), task.getWorkPlan().getId(),
                task.getCoursework().getId(), task.getEnrollment().getId(),
                task.getTitle(), task.getTargetDate(),
                task.getEstimatedWorkHours());
    }

}
