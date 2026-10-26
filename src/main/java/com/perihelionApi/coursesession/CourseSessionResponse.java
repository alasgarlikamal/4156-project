package com.perihelionApi.coursesession;

import java.time.LocalTime;

/**
 * A course's weekly meeting time.
 *
 * @param id        session identifier
 * @param courseId  owning course identifier
 * @param weekday   ISO weekday number, Monday 1 through Sunday 7
 * @param startTime local start time
 * @param endTime   local end time
 */
public record CourseSessionResponse(Long id, Long courseId, Integer weekday,
        LocalTime startTime, LocalTime endTime) {

    /**
     * Builds a response from a persisted session.
     *
     * @param session the session entity
     * @return the API response
     */
    public static CourseSessionResponse from(final CourseSession session) {
        return new CourseSessionResponse(session.getId(),
                session.getCourse().getId(), session.getWeekday(),
                session.getStartTime(), session.getEndTime());
    }
}
