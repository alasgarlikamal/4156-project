package com.perihelionApi.assignment;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.perihelionApi.common.NotFoundException;
import com.perihelionApi.course.Course;
import com.perihelionApi.course.CourseRepository;

/**
 * Business logic for assignments.
 */
@Service
@Transactional
public class AssignmentService {

    /** Persistence for assignments. */
    private final AssignmentRepository assignmentRepository;

    /** Persistence for courses, used to resolve the owning course. */
    private final CourseRepository courseRepository;

    /**
     * Creates the service.
     *
     * @param assignmentRepository the assignment repository
     * @param courseRepository the course repository
     */
    public AssignmentService(final AssignmentRepository assignmentRepository,
            final CourseRepository courseRepository) {
        this.assignmentRepository = assignmentRepository;
        this.courseRepository = courseRepository;
    }

    /**
     * Creates and persists an assignment.
     *
     * @param request the assignment values
     * @return the created assignment
     * @throws NotFoundException if the course does not exist
     */
    public AssignmentResponse create(final AssignmentRequest request) {
        Assignment assignment = new Assignment();
        apply(assignment, request);
        return AssignmentResponse.from(assignmentRepository.save(assignment));
    }

    /**
     * Returns a single assignment.
     *
     * @param id the assignment id
     * @return the assignment
     * @throws NotFoundException if the assignment does not exist
     */
    @Transactional(readOnly = true)
    public AssignmentResponse get(final Long id) {
        return AssignmentResponse.from(findAssignment(id));
    }

    /**
     * Lists assignments, optionally filtered by course.
     *
     * @param courseId a course id, or null
     * @return assignments ordered by due date and id
     */
    @Transactional(readOnly = true)
    public List<AssignmentResponse> list(final Long courseId) {
        List<Assignment> assignments = courseId == null
                ? assignmentRepository.findAllByOrderByDueDateAscIdAsc()
                : assignmentRepository
                        .findByCourseIdOrderByDueDateAscIdAsc(courseId);
        return assignments.stream().map(AssignmentResponse::from).toList();
    }

    /**
     * Replaces an assignment.
     *
     * @param id the assignment id
     * @param request the new values
     * @return the updated assignment
     * @throws NotFoundException if the assignment or course does not exist
     */
    public AssignmentResponse update(final Long id,
            final AssignmentRequest request) {
        Assignment assignment = findAssignment(id);
        apply(assignment, request);
        return AssignmentResponse.from(assignmentRepository.save(assignment));
    }

    /**
     * Deletes an assignment.
     *
     * @param id the assignment id
     * @throws NotFoundException if the assignment does not exist
     */
    public void delete(final Long id) {
        if (!assignmentRepository.existsById(id)) {
            throw new NotFoundException("Assignment not found");
        }
        assignmentRepository.deleteById(id);
    }

    private Assignment findAssignment(final Long id) {
        return assignmentRepository.findById(id).orElseThrow(
                () -> new NotFoundException("Assignment not found"));
    }

    private void apply(final Assignment assignment,
            final AssignmentRequest request) {
        Course course = courseRepository.findById(request.courseId())
                .orElseThrow(() -> new NotFoundException("Course not found"));
        assignment.setCourse(course);
        assignment.setTitle(request.title());
        assignment.setDueDate(request.dueDate());
        assignment.setEstimatedWorkHours(request.estimatedWorkHours());
    }
}
