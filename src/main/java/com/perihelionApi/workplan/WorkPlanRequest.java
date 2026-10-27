/*
    AI usage: Work plan CRUD: Grok 4.7, ~238K tokens
 */
package com.perihelionApi.workplan;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Request body for creating or replacing a work plan.
 *
 * @param enrollmentId         the enrollment the plan belongs to
 * @param rangeStart           the first day covered by the plan
 * @param rangeEnd             the last day covered by the plan
 * @param availableHoursPerDay hours available each day, from 0 to 24
 */
public record WorkPlanRequest(
        @NotNull @Positive Long enrollmentId,
        @NotNull LocalDate rangeStart,
        @NotNull LocalDate rangeEnd,
        @NotNull @DecimalMin("0.0") @DecimalMax("24.0")
        BigDecimal availableHoursPerDay) {

    /**
     * Checks that the range starts before it ends. Null dates are treated
     * as valid here so that the not-null constraints report them instead.
     *
     * @return true if the range is valid or either date is null
     */
    @AssertTrue(message = "rangeStart must be earlier than rangeEnd")
    public boolean isRangeValid() {
        if (rangeStart == null || rangeEnd == null) {
            return true;
        }
        return rangeStart.isBefore(rangeEnd);
    }

}
