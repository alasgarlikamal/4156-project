/*
    AI usage: Coursework CRUD: Grok 4.7, ~238K tokens
 */
package com.perihelionApi.coursework;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.perihelionApi.common.ConflictException;
import com.perihelionApi.common.NotFoundException;
import com.perihelionApi.course.Course;
import com.perihelionApi.course.CourseRepository;
import com.perihelionApi.submission.SubmissionRepository;
import com.perihelionApi.task.TaskRepository;

/**
 * Business logic for coursework.
 */
@Service
@Transactional
public class CourseworkService {

    /** Persistence for coursework. */
    private final CourseworkRepository courseworkRepository;

    /** Persistence for courses, used to resolve the owning course. */
    private final CourseRepository courseRepository;

    /** Persistence for submissions, used to guard deletes. */
    private final SubmissionRepository submissionRepository;

    /** Persistence for tasks, used to guard deletes. */
    private final TaskRepository taskRepository;

    /**
     * Creates the service.
     *
     * @param courseworkRepository the coursework repository
     * @param courseRepository     the course repository
     * @param submissionRepository the submission repository
     * @param taskRepository       the task repository
     */
    public CourseworkService(final CourseworkRepository courseworkRepository,
            final CourseRepository courseRepository,
            final SubmissionRepository submissionRepository,
            final TaskRepository taskRepository) {
        this.courseworkRepository = courseworkRepository;
        this.courseRepository = courseRepository;
        this.submissionRepository = submissionRepository;
        this.taskRepository = taskRepository;
    }

    /**
     * Creates and persists coursework.
     *
     * @param request the coursework values
     * @return the created coursework
     * @throws NotFoundException if the course does not exist
     */
    public CourseworkResponse create(final CourseworkRequest request) {
        Coursework coursework = new Coursework();
        apply(coursework, request);
        return CourseworkResponse.from(courseworkRepository.save(coursework));
    }

    /**
     * Returns a single piece of coursework.
     *
     * @param id the coursework id
     * @return the coursework
     * @throws NotFoundException if no coursework has the given id
     */
    @Transactional(readOnly = true)
    public CourseworkResponse get(final Long id) {
        return CourseworkResponse.from(findCoursework(id));
    }

    /**
     * Lists coursework matching every supplied filter.
     *
     * @param courseId a course id, or null
     * @param kind     a coursework kind, or null
     * @return the matching coursework ordered by deadline and id
     */
    @Transactional(readOnly = true)
    public List<CourseworkResponse> list(final Long courseId,
            final CourseworkKind kind) {
        return courseworkRepository.search(courseId, kind).stream()
                .map(CourseworkResponse::from).toList();
    }

    /**
     * Replaces coursework.
     *
     * @param id      the coursework id
     * @param request the new values
     * @return the updated coursework
     * @throws NotFoundException if the coursework or course does not exist
     */
    public CourseworkResponse update(final Long id,
            final CourseworkRequest request) {
        Coursework coursework = findCoursework(id);
        apply(coursework, request);
        return CourseworkResponse.from(courseworkRepository.save(coursework));
    }

    /**
     * Deletes coursework.
     *
     * @param id the coursework id
     * @throws NotFoundException if the coursework does not exist
     * @throws ConflictException if submissions or tasks still reference it
     */
    public void delete(final Long id) {
        if (!courseworkRepository.existsById(id)) {
            throw new NotFoundException("Coursework not found");
        }
        if (submissionRepository.existsByCourseworkId(id)) {
            throw new ConflictException(
                    "Coursework cannot be deleted while submissions exist");
        }
        if (taskRepository.existsByCourseworkId(id)) {
            throw new ConflictException(
                    "Coursework cannot be deleted while tasks exist");
        }
        courseworkRepository.deleteById(id);
    }

    private Coursework findCoursework(final Long id) {
        return courseworkRepository.findById(id).orElseThrow(
                () -> new NotFoundException("Coursework not found"));
    }

    private void apply(final Coursework coursework,
            final CourseworkRequest request) {
        Course course = courseRepository.findById(request.courseId())
                .orElseThrow(() -> new NotFoundException("Course not found"));
        coursework.setCourse(course);
        coursework.setKind(request.kind());
        coursework.setTitle(request.title());
        coursework.setDeadline(request.deadline());
        coursework.setEstimatedEffortHours(request.estimatedEffortHours());
    }

}
