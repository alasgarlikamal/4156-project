/*
    AI usage: Work plan CRUD: Grok 4.7, ~238K tokens
 */
package com.perihelionApi.workplan;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Work plan representation returned to clients.
 *
 * @param id                   the work plan id
 * @param enrollmentId         the enrollment id
 * @param rangeStart           the first day covered by the plan
 * @param rangeEnd             the last day covered by the plan
 * @param availableHoursPerDay hours available each day
 * @param createdAt            when the plan was created
 */
public record WorkPlanResponse(Long id, Long enrollmentId,
        LocalDate rangeStart, LocalDate rangeEnd,
        BigDecimal availableHoursPerDay, Instant createdAt) {

    /**
     * Builds a response from an entity.
     *
     * @param workPlan the entity
     * @return the response
     */
    public static WorkPlanResponse from(final WorkPlan workPlan) {
        return new WorkPlanResponse(workPlan.getId(),
                workPlan.getEnrollment().getId(), workPlan.getRangeStart(),
                workPlan.getRangeEnd(), workPlan.getAvailableHoursPerDay(),
                workPlan.getCreatedAt());
    }

}
