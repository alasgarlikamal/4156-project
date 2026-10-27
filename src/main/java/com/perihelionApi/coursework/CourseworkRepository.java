/*
    AI usage: Coursework CRUD: Grok 4.7, ~238K tokens
 */
package com.perihelionApi.coursework;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Spring Data repository for {@link Coursework} entities.
 */
public interface CourseworkRepository
        extends JpaRepository<Coursework, Long> {

    /**
     * Returns coursework matching every non-null filter, ordered by
     * deadline then id. A null filter matches everything.
     *
     * @param courseId a course id, or null
     * @param kind     a coursework kind, or null
     * @return the matching coursework
     */
    @Query("""
            select c from Coursework c
            where (:courseId is null or c.course.id = :courseId)
              and (:kind is null or c.kind = :kind)
            order by c.deadline asc, c.id asc
            """)
    List<Coursework> search(@Param("courseId") Long courseId,
            @Param("kind") CourseworkKind kind);

    /**
     * Reports whether any coursework references the given course.
     *
     * @param courseId the course id
     * @return true if at least one coursework exists for the course
     */
    boolean existsByCourseId(Long courseId);

}
