/*
    AI usage: Coursework CRUD: Grok 4.7, ~238K tokens
 */
package com.perihelionApi.coursework;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Coursework representation returned to clients.
 *
 * @param id                   the coursework id
 * @param courseId             the owning course id
 * @param kind                 EXAM or ASSIGNMENT
 * @param title                the coursework title
 * @param deadline             when the work is due
 * @param estimatedEffortHours estimated effort in hours
 */
public record CourseworkResponse(Long id, Long courseId, CourseworkKind kind,
        String title, Instant deadline, BigDecimal estimatedEffortHours) {

    /**
     * Builds a response from an entity.
     *
     * @param coursework the entity
     * @return the response
     */
    public static CourseworkResponse from(final Coursework coursework) {
        return new CourseworkResponse(coursework.getId(),
                coursework.getCourse().getId(), coursework.getKind(),
                coursework.getTitle(), coursework.getDeadline(),
                coursework.getEstimatedEffortHours());
    }

}
