/*
    AI usage: Course and Enrollment CRUD: Grok 4.7, ~111K tokens
 */
package com.perihelionApi.course;

import java.time.LocalDate;

/**
 * Course representation returned to clients.
 *
 * @param id        the course id
 * @param code      the course code, or null
 * @param title     the course title
 * @param termStart the first day of the term
 * @param termEnd   the last day of the term
 */
public record CourseResponse(Long id, String code, String title,
        LocalDate termStart, LocalDate termEnd) {

    /**
     * Builds a response from an entity.
     *
     * @param course the entity
     * @return the response
     */
    public static CourseResponse from(final Course course) {
        return new CourseResponse(course.getId(), course.getCode(),
                course.getTitle(), course.getTermStart(), course.getTermEnd());
    }

}
