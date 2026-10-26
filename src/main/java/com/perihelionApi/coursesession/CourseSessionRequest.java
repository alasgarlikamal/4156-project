package com.perihelionApi.coursesession;

import java.time.LocalTime;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Request to create or replace a weekly course meeting time.
 *
 * @param weekday   ISO weekday number, Monday 1 through Sunday 7
 * @param startTime local start time in HH:mm format
 * @param endTime   local end time in HH:mm format
 */
public record CourseSessionRequest(
        @NotNull @Min(1) @Max(7) Integer weekday,
        @NotNull LocalTime startTime,
        @NotNull LocalTime endTime) {

    /**
     * Ensures that the meeting ends later on the same day it starts.
     *
     * @return true if either time is null or endTime is later
     */
    @AssertTrue(message = "endTime must be later than startTime")
    public boolean isTimeRangeValid() {
        if (startTime == null || endTime == null) {
            return true;
        }
        return endTime.isAfter(startTime);
    }
}
