package com.perihelionApi.task;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Provides database operations for tasks.
 */
public interface TaskRepository extends JpaRepository<Task, Integer> {

    /**
     * Finds tasks associated with an enrollment.
     *
     * @param enrollmentId the enrollment identifier
     * @return tasks belonging to the enrollment
     */
    List<Task> findByEnrollmentId(final Integer enrollmentId);

    /**
     * Finds tasks associated with a work plan.
     *
     * @param workplanId the work plan identifier
     * @return tasks belonging to the work plan
     */
    List<Task> findByWorkplanId(final Integer workplanId);

    /**
     * Finds tasks associated with coursework.
     *
     * @param courseworkId the coursework identifier
     * @return tasks belonging to the coursework
     */
    List<Task> findByCourseworkId(final Integer courseworkId);
}