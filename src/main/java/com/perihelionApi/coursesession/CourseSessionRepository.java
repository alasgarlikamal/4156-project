// Wrote file with github copilot used 6 credits
package com.perihelionApi.coursesession;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Persistence operations for course meeting times.
 */
public interface CourseSessionRepository
        extends JpaRepository<CourseSession, Long> {

    /**
     * Finds a course's meeting times in weekday and start-time order.
     *
     * @param courseId the course identifier
     * @return the ordered meeting times
     */
    List<CourseSession> findByCourseIdOrderByWeekdayAscStartTimeAsc(
            Long courseId);
}
