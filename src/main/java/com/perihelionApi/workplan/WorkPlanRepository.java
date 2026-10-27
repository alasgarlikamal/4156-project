/*
    AI usage: Work plan CRUD: Grok 4.7, ~238K tokens
 */
package com.perihelionApi.workplan;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data repository for {@link WorkPlan} entities.
 */
public interface WorkPlanRepository extends JpaRepository<WorkPlan, Long> {

    /**
     * Returns every work plan ordered by range start, then id.
     *
     * @return all work plans
     */
    List<WorkPlan> findAllByOrderByRangeStartAscIdAsc();

    /**
     * Returns the work plans of an enrollment ordered by range start,
     * then id.
     *
     * @param enrollmentId the enrollment id
     * @return the matching work plans
     */
    List<WorkPlan> findByEnrollmentIdOrderByRangeStartAscIdAsc(
            Long enrollmentId);

    /**
     * Reports whether any work plan references the given enrollment.
     *
     * @param enrollmentId the enrollment id
     * @return true if at least one work plan exists for the enrollment
     */
    boolean existsByEnrollmentId(Long enrollmentId);

}
