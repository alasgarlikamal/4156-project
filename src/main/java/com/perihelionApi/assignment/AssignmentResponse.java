package com.perihelionApi.assignment;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Assignment representation returned to clients.
 *
 * @param id the assignment id
 * @param courseId the course id
 * @param title the assignment title
 * @param dueDate the assignment deadline
 * @param estimatedWorkHours estimated effort in hours
 */
public record AssignmentResponse(Long id, Long courseId, String title,
        LocalDate dueDate, BigDecimal estimatedWorkHours) {

    /**
     * Builds a response from an entity.
     *
     * @param assignment the entity
     * @return the response
     */
    public static AssignmentResponse from(final Assignment assignment) {
        return new AssignmentResponse(assignment.getId(),
                assignment.getCourse().getId(), assignment.getTitle(),
                assignment.getDueDate(), assignment.getEstimatedWorkHours());
    }
}
