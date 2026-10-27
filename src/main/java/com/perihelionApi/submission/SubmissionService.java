/*
    AI usage: Submission coursework link: Grok 4.7, ~238K tokens
 */
package com.perihelionApi.submission;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.perihelionApi.common.ConflictException;
import com.perihelionApi.common.NotFoundException;
import com.perihelionApi.coursework.Coursework;
import com.perihelionApi.coursework.CourseworkRepository;
import com.perihelionApi.enrollment.Enrollment;
import com.perihelionApi.enrollment.EnrollmentRepository;

/**
 * Business logic for submissions.
 */
@Service
@Transactional
public class SubmissionService {

    /** Message used when an enrollment already submitted a coursework. */
    private static final String DUPLICATE_MESSAGE =
            "A submission already exists for this enrollment and coursework";

    /** Persistence for submissions. */
    private final SubmissionRepository submissionRepository;

    /** Persistence for enrollments, used to resolve the submitter. */
    private final EnrollmentRepository enrollmentRepository;

    /** Persistence for coursework, used to resolve the submitted work. */
    private final CourseworkRepository courseworkRepository;

    /**
     * Creates the service.
     *
     * @param submissionRepository the submission repository
     * @param enrollmentRepository the enrollment repository
     * @param courseworkRepository the coursework repository
     */
    public SubmissionService(final SubmissionRepository submissionRepository,
            final EnrollmentRepository enrollmentRepository,
            final CourseworkRepository courseworkRepository) {
        this.submissionRepository = submissionRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.courseworkRepository = courseworkRepository;
    }

    /**
     * Records a submission.
     *
     * @param request the submission values
     * @return the created submission
     * @throws NotFoundException if the enrollment or coursework does not
     *                           exist
     * @throws ConflictException if the coursework belongs to another course
     *                           or the enrollment already submitted it
     */
    public SubmissionResponse create(final SubmissionRequest request) {
        Enrollment enrollment = findEnrollment(request.enrollmentId());
        Coursework coursework = findCoursework(request.courseworkId());
        requireSameCourse(enrollment, coursework);
        if (submissionRepository.existsByEnrollmentIdAndCourseworkId(
                enrollment.getId(), coursework.getId())) {
            throw new ConflictException(DUPLICATE_MESSAGE);
        }
        Submission submission = new Submission();
        submission.setEnrollment(enrollment);
        submission.setCoursework(coursework);
        submission.setSubmittedAt(request.submittedAt() == null
                ? Instant.now() : request.submittedAt());
        return SubmissionResponse.from(submissionRepository.save(submission));
    }

    /**
     * Returns a single submission.
     *
     * @param id the submission id
     * @return the submission
     * @throws NotFoundException if no submission has the given id
     */
    @Transactional(readOnly = true)
    public SubmissionResponse get(final Long id) {
        return SubmissionResponse.from(findSubmission(id));
    }

    /**
     * Lists submissions, optionally filtered by enrollment and coursework.
     *
     * @param enrollmentId an enrollment id, or null
     * @param courseworkId a coursework id, or null
     * @return the matching submissions ordered by id
     */
    @Transactional(readOnly = true)
    public List<SubmissionResponse> list(final Long enrollmentId,
            final Long courseworkId) {
        List<Submission> submissions;
        if (enrollmentId != null && courseworkId != null) {
            submissions = submissionRepository
                    .findByEnrollmentIdAndCourseworkIdOrderByIdAsc(
                            enrollmentId, courseworkId);
        } else if (enrollmentId != null) {
            submissions = submissionRepository
                    .findByEnrollmentIdOrderByIdAsc(enrollmentId);
        } else if (courseworkId != null) {
            submissions = submissionRepository
                    .findByCourseworkIdOrderByIdAsc(courseworkId);
        } else {
            submissions = submissionRepository.findAllByOrderByIdAsc();
        }
        return submissions.stream().map(SubmissionResponse::from).toList();
    }

    /**
     * Replaces a submission.
     *
     * @param id      the submission id
     * @param request the new submission values
     * @return the updated submission
     * @throws NotFoundException if the submission, enrollment, or coursework
     *                           does not exist
     * @throws ConflictException if the coursework belongs to another course
     *                           or another submission already pairs the
     *                           enrollment with the coursework
     */
    public SubmissionResponse update(final Long id,
            final SubmissionRequest request) {
        Submission submission = findSubmission(id);
        Enrollment enrollment = findEnrollment(request.enrollmentId());
        Coursework coursework = findCoursework(request.courseworkId());
        requireSameCourse(enrollment, coursework);
        if (submissionRepository.existsByEnrollmentIdAndCourseworkIdAndIdNot(
                enrollment.getId(), coursework.getId(), id)) {
            throw new ConflictException(DUPLICATE_MESSAGE);
        }
        submission.setEnrollment(enrollment);
        submission.setCoursework(coursework);
        if (request.submittedAt() != null) {
            submission.setSubmittedAt(request.submittedAt());
        }
        return SubmissionResponse.from(submissionRepository.save(submission));
    }

    /**
     * Deletes a submission.
     *
     * @param id the submission id
     * @throws NotFoundException if no submission has the given id
     */
    public void delete(final Long id) {
        if (!submissionRepository.existsById(id)) {
            throw new NotFoundException("Submission not found");
        }
        submissionRepository.deleteById(id);
    }

    private static void requireSameCourse(final Enrollment enrollment,
            final Coursework coursework) {
        if (!Objects.equals(enrollment.getCourse().getId(),
                coursework.getCourse().getId())) {
            throw new ConflictException(
                    "Coursework must belong to the enrollment's course");
        }
    }

    private Enrollment findEnrollment(final Long enrollmentId) {
        return enrollmentRepository.findById(enrollmentId)
                .orElseThrow(
                        () -> new NotFoundException("Enrollment not found"));
    }

    private Coursework findCoursework(final Long courseworkId) {
        return courseworkRepository.findById(courseworkId)
                .orElseThrow(
                        () -> new NotFoundException("Coursework not found"));
    }

    private Submission findSubmission(final Long id) {
        return submissionRepository.findById(id)
                .orElseThrow(
                        () -> new NotFoundException("Submission not found"));
    }

}
