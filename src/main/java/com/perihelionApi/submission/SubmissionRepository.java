package com.perihelionApi.submission;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data repository for {@link Submission} entities.
 */
public interface SubmissionRepository extends JpaRepository<Submission, Long> {

    /**
     * Reports whether any submission references the given enrollment.
     *
     * @param enrollmentId the enrollment id
     * @return true if at least one submission exists for the enrollment
     */
    boolean existsByEnrollmentId(Long enrollmentId);

    /**
     * Reports whether an enrollment has already submitted a coursework.
     *
     * @param enrollmentId the enrollment id
     * @param courseworkId the coursework id
     * @return true if such a submission exists
     */
    boolean existsByEnrollmentIdAndCourseworkId(Long enrollmentId,
            Long courseworkId);

    /**
     * Reports whether an enrollment has submitted a coursework.
     *
     * @param enrollmentId the enrollment id
     * @param courseworkId the coursework id
     * @param id           the submission id to exclude
     * @return true if such a submission exists
     */
    boolean existsByEnrollmentIdAndCourseworkIdAndIdNot(Long enrollmentId,
            Long courseworkId, Long id);

    /**
     * Returns every submission ordered by id.
     *
     * @return all submissions
     */
    List<Submission> findAllByOrderByIdAsc();

    /**
     * Returns submissions made through an enrollment.
     *
     * @param enrollmentId the enrollment id
     * @return the matching submissions ordered by id
     */
    List<Submission> findByEnrollmentIdOrderByIdAsc(Long enrollmentId);

    /**
     * Returns submissions of a coursework.
     *
     * @param courseworkId the coursework id
     * @return the matching submissions ordered by id
     */
    List<Submission> findByCourseworkIdOrderByIdAsc(Long courseworkId);

    /**
     * Returns submissions of a coursework made through an enrollment.
     *
     * @param enrollmentId the enrollment id
     * @param courseworkId the coursework id
     * @return the matching submissions ordered by id
     */
    List<Submission> findByEnrollmentIdAndCourseworkIdOrderByIdAsc(
            Long enrollmentId, Long courseworkId);

}
