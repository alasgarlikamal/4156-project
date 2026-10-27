/*
    AI usage: Course and Enrollment CRUD: Grok 4.7, ~111K tokens
 */
package com.perihelionApi.enrollment;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.perihelionApi.common.ConflictException;
import com.perihelionApi.common.NotFoundException;
import com.perihelionApi.course.Course;
import com.perihelionApi.course.CourseRepository;
import com.perihelionApi.submission.SubmissionRepository;
import com.perihelionApi.workplan.WorkPlanRepository;

/**
 * Business logic for enrollments.
 */
@Service
@Transactional
public class EnrollmentService {

    /** Persistence for enrollments. */
    private final EnrollmentRepository enrollmentRepository;

    /** Persistence for courses, used to resolve the enrolled course. */
    private final CourseRepository courseRepository;

    /** Persistence for submissions, used to guard deletes. */
    private final SubmissionRepository submissionRepository;

    /** Persistence for work plans, used to guard deletes. */
    private final WorkPlanRepository workPlanRepository;

    /**
     * Creates the service.
     *
     * @param enrollmentRepository the enrollment repository
     * @param courseRepository     the course repository
     * @param submissionRepository the submission repository
     * @param workPlanRepository   the work plan repository
     */
    public EnrollmentService(final EnrollmentRepository enrollmentRepository,
            final CourseRepository courseRepository,
            final SubmissionRepository submissionRepository,
            final WorkPlanRepository workPlanRepository) {
        this.enrollmentRepository = enrollmentRepository;
        this.courseRepository = courseRepository;
        this.submissionRepository = submissionRepository;
        this.workPlanRepository = workPlanRepository;
    }

    /**
     * Enrolls a learner in a course.
     *
     * @param request the enrollment values
     * @return the created enrollment
     * @throws NotFoundException if the course does not exist
     * @throws ConflictException if the learner is already enrolled
     */
    public EnrollmentResponse create(final EnrollmentRequest request) {
        Course course = findCourse(request.courseId());
        String learnerRef = request.learnerRef().toString();
        if (enrollmentRepository.existsByCourseIdAndLearnerRef(course.getId(),
                learnerRef)) {
            throw new ConflictException(
                    "An enrollment already exists for this course and learner");
        }
        Enrollment enrollment = new Enrollment();
        enrollment.setCourse(course);
        enrollment.setLearnerRef(learnerRef);
        return EnrollmentResponse.from(enrollmentRepository.save(enrollment));
    }

    /**
     * Returns a single enrollment.
     *
     * @param id the enrollment id
     * @return the enrollment
     * @throws NotFoundException if no enrollment has the given id
     */
    @Transactional(readOnly = true)
    public EnrollmentResponse get(final Long id) {
        return EnrollmentResponse.from(findEnrollment(id));
    }

    /**
     * Lists enrollments, optionally filtered by course and learner.
     *
     * @param courseId   a course id, or null
     * @param learnerRef a learner identifier, or null
     * @return the matching enrollments ordered by id
     */
    @Transactional(readOnly = true)
    public List<EnrollmentResponse> list(final Long courseId,
            final UUID learnerRef) {
        String learner = learnerRef == null ? null : learnerRef.toString();
        List<Enrollment> enrollments;
        if (courseId != null && learner != null) {
            enrollments = enrollmentRepository
                    .findByCourseIdAndLearnerRefOrderByIdAsc(courseId, learner);
        } else if (courseId != null) {
            enrollments = enrollmentRepository
                    .findByCourseIdOrderByIdAsc(courseId);
        } else if (learner != null) {
            enrollments = enrollmentRepository
                    .findByLearnerRefOrderByIdAsc(learner);
        } else {
            enrollments = enrollmentRepository.findAllByOrderByIdAsc();
        }
        return enrollments.stream().map(EnrollmentResponse::from).toList();
    }

    /**
     * Replaces an enrollment.
     *
     * @param id      the enrollment id
     * @param request the new enrollment values
     * @return the updated enrollment
     * @throws NotFoundException if the enrollment or course does not exist
     * @throws ConflictException if another enrollment already pairs the
     *                           learner with the course
     */
    public EnrollmentResponse update(final Long id,
            final EnrollmentRequest request) {
        Enrollment enrollment = findEnrollment(id);
        Course course = findCourse(request.courseId());
        String learnerRef = request.learnerRef().toString();
        if (enrollmentRepository.existsByCourseIdAndLearnerRefAndIdNot(
                course.getId(), learnerRef, id)) {
            throw new ConflictException(
                    "An enrollment already exists for this course and learner");
        }
        enrollment.setCourse(course);
        enrollment.setLearnerRef(learnerRef);
        return EnrollmentResponse.from(enrollmentRepository.save(enrollment));
    }

    /**
     * Deletes an enrollment.
     *
     * @param id the enrollment id
     * @throws NotFoundException if no enrollment has the given id
     * @throws ConflictException if the enrollment has submissions or work
     *                           plans
     */
    public void delete(final Long id) {
        if (!enrollmentRepository.existsById(id)) {
            throw new NotFoundException("Enrollment not found");
        }
        if (submissionRepository.existsByEnrollmentId(id)) {
            throw new ConflictException(
                    "Enrollment cannot be deleted while submissions exist");
        }
        if (workPlanRepository.existsByEnrollmentId(id)) {
            throw new ConflictException(
                    "Enrollment cannot be deleted while work plans exist");
        }
        enrollmentRepository.deleteById(id);
    }

    private Course findCourse(final Long courseId) {
        return courseRepository.findById(courseId)
                .orElseThrow(() -> new NotFoundException("Course not found"));
    }

    private Enrollment findEnrollment(final Long id) {
        return enrollmentRepository.findById(id)
                .orElseThrow(
                        () -> new NotFoundException("Enrollment not found"));
    }

}
