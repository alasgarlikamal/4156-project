/*
    AI usage: Task CRUD: Grok 4.7, ~238K tokens
 */
package com.perihelionApi.task;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Spring Data repository for {@link Task} entities.
 */
public interface TaskRepository extends JpaRepository<Task, Long> {

    /**
     * Returns tasks matching every non-null filter, ordered by target date
     * then id. A null filter matches every task.
     *
     * @param enrollmentId an enrollment id, or null
     * @param workPlanId   a work plan id, or null
     * @param courseworkId a coursework id, or null
     * @return the matching tasks
     */
    @Query("""
            select t from Task t
            where (:enrollmentId is null
                    or t.enrollment.id = :enrollmentId)
              and (:workPlanId is null or t.workPlan.id = :workPlanId)
              and (:courseworkId is null
                    or t.coursework.id = :courseworkId)
            order by t.targetDate asc, t.id asc
            """)
    List<Task> search(@Param("enrollmentId") Long enrollmentId,
            @Param("workPlanId") Long workPlanId,
            @Param("courseworkId") Long courseworkId);

    /**
     * Reports whether any task references the given work plan.
     *
     * @param workPlanId the work plan id
     * @return true if at least one task exists for the work plan
     */
    boolean existsByWorkPlanId(Long workPlanId);

    /**
     * Reports whether any task references the given coursework.
     *
     * @param courseworkId the coursework id
     * @return true if at least one task exists for the coursework
     */
    boolean existsByCourseworkId(Long courseworkId);

    /**
     * Reports whether any task references the given enrollment.
     *
     * @param enrollmentId the enrollment id
     * @return true if at least one task exists for the enrollment
     */
    boolean existsByEnrollmentId(Long enrollmentId);

}
