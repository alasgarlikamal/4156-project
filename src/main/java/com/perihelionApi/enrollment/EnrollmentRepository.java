/*
    AI usage: Course and Enrollment CRUD: Grok 4.7, ~111K tokens
 */
package com.perihelionApi.enrollment;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data repository for {@link Enrollment} entities.
 */
public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    /**
     * Reports whether any enrollment references the given course.
     *
     * @param courseId the course id
     * @return true if at least one enrollment exists for the course
     */
    boolean existsByCourseId(Long courseId);

    /**
     * Reports whether a learner is already enrolled in a course.
     *
     * @param courseId   the course id
     * @param learnerRef the learner identifier
     * @return true if such an enrollment exists
     */
    boolean existsByCourseIdAndLearnerRef(Long courseId, String learnerRef);

    /**
     * Reports whether a learner is enrolled in a course through an
     * enrollment other than the given one.
     *
     * @param courseId   the course id
     * @param learnerRef the learner identifier
     * @param id         the enrollment id to exclude
     * @return true if such an enrollment exists
     */
    boolean existsByCourseIdAndLearnerRefAndIdNot(Long courseId,
            String learnerRef, Long id);

    /**
     * Returns every enrollment ordered by id.
     *
     * @return all enrollments
     */
    List<Enrollment> findAllByOrderByIdAsc();

    /**
     * Returns enrollments for a course.
     *
     * @param courseId the course id
     * @return the matching enrollments ordered by id
     */
    List<Enrollment> findByCourseIdOrderByIdAsc(Long courseId);

    /**
     * Returns enrollments for a learner.
     *
     * @param learnerRef the learner identifier
     * @return the matching enrollments ordered by id
     */
    List<Enrollment> findByLearnerRefOrderByIdAsc(String learnerRef);

    /**
     * Returns enrollments for a learner in a course.
     *
     * @param courseId   the course id
     * @param learnerRef the learner identifier
     * @return the matching enrollments ordered by id
     */
    List<Enrollment> findByCourseIdAndLearnerRefOrderByIdAsc(Long courseId,
            String learnerRef);

}
