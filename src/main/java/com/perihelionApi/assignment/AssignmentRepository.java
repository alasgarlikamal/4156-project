package com.perihelionApi.assignment;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data repository for assignment entities.
 */
public interface AssignmentRepository extends JpaRepository<Assignment, Long> {

    /**
     * Lists assignments in deadline order, breaking ties by id.
     *
     * @return all assignments
     */
    List<Assignment> findAllByOrderByDueDateAscIdAsc();

    /**
     * Lists a course's assignments in deadline order, breaking ties by id.
     *
     * @param courseId the course id
     * @return matching assignments
     */
    List<Assignment> findByCourseIdOrderByDueDateAscIdAsc(Long courseId);
}
