/*
    AI usage: Course and Enrollment CRUD: Grok 4.7, ~111K tokens
 */
package com.perihelionApi.course;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data repository for {@link Course} entities.
 */
public interface CourseRepository extends JpaRepository<Course, Long> {

    /**
     * Returns every course ordered by title.
     *
     * @return all courses
     */
    List<Course> findAllByOrderByTitleAsc();

    /**
     * Returns courses whose title contains the given text.
     *
     * @param title a case-insensitive title fragment
     * @return the matching courses ordered by title
     */
    List<Course> findByTitleContainingIgnoreCaseOrderByTitleAsc(String title);

    /**
     * Returns courses with the given code.
     *
     * @param code an exact course code
     * @return the matching courses ordered by title
     */
    List<Course> findByCodeOrderByTitleAsc(String code);

    /**
     * Returns courses matching both a title fragment and a code.
     *
     * @param title a case-insensitive title fragment
     * @param code  an exact course code
     * @return the matching courses ordered by title
     */
    List<Course> findByTitleContainingIgnoreCaseAndCodeOrderByTitleAsc(
            String title, String code);

}
