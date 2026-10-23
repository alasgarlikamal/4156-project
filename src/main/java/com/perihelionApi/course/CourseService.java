/*
    AI usage: Course and Enrollment CRUD: Grok 4.7, ~111K tokens
 */
package com.perihelionApi.course;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.perihelionApi.common.ConflictException;
import com.perihelionApi.common.NotFoundException;
import com.perihelionApi.enrollment.EnrollmentRepository;

/**
 * Business logic for courses.
 */
@Service
@Transactional
public class CourseService {

    /** Persistence for courses. */
    private final CourseRepository courseRepository;

    /** Persistence for enrollments, used to guard deletes. */
    private final EnrollmentRepository enrollmentRepository;

    /**
     * Creates the service.
     *
     * @param courseRepository     the course repository
     * @param enrollmentRepository the enrollment repository
     */
    public CourseService(final CourseRepository courseRepository,
            final EnrollmentRepository enrollmentRepository) {
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
    }

    /**
     * Creates and persists a course.
     *
     * @param request the course values
     * @return the created course
     */
    public CourseResponse create(final CourseRequest request) {
        Course course = new Course();
        apply(course, request);
        return CourseResponse.from(courseRepository.save(course));
    }

    /**
     * Returns a single course.
     *
     * @param id the course id
     * @return the course
     * @throws NotFoundException if no course has the given id
     */
    @Transactional(readOnly = true)
    public CourseResponse get(final Long id) {
        return CourseResponse.from(findCourse(id));
    }

    /**
     * Lists courses, optionally filtered by title substring and code.
     *
     * @param title a case-insensitive title fragment, or null or blank
     * @param code  an exact course code, or null or blank
     * @return the matching courses ordered by title
     */
    @Transactional(readOnly = true)
    public List<CourseResponse> list(final String title, final String code) {
        String normalizedTitle = blankToNull(title);
        String normalizedCode = blankToNull(code);
        List<Course> courses;
        if (normalizedTitle != null && normalizedCode != null) {
            courses = courseRepository
                    .findByTitleContainingIgnoreCaseAndCodeOrderByTitleAsc(
                            normalizedTitle, normalizedCode);
        } else if (normalizedTitle != null) {
            courses = courseRepository
                    .findByTitleContainingIgnoreCaseOrderByTitleAsc(
                            normalizedTitle);
        } else if (normalizedCode != null) {
            courses = courseRepository
                    .findByCodeOrderByTitleAsc(normalizedCode);
        } else {
            courses = courseRepository.findAllByOrderByTitleAsc();
        }
        return courses.stream().map(CourseResponse::from).toList();
    }

    /**
     * Replaces a course.
     *
     * @param id      the course id
     * @param request the new course values
     * @return the updated course
     * @throws NotFoundException if no course has the given id
     */
    public CourseResponse update(final Long id, final CourseRequest request) {
        Course course = findCourse(id);
        apply(course, request);
        return CourseResponse.from(courseRepository.save(course));
    }

    /**
     * Deletes a course.
     *
     * @param id the course id
     * @throws NotFoundException if no course has the given id
     * @throws ConflictException if the course still has enrollments
     */
    public void delete(final Long id) {
        if (!courseRepository.existsById(id)) {
            throw new NotFoundException("Course not found");
        }
        if (enrollmentRepository.existsByCourseId(id)) {
            throw new ConflictException(
                    "Course cannot be deleted while enrollments exist");
        }
        courseRepository.deleteById(id);
    }

    private Course findCourse(final Long id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Course not found"));
    }

    private void apply(final Course course, final CourseRequest request) {
        course.setCode(blankToNull(request.code()));
        course.setTitle(request.title());
        course.setTermStart(request.termStart());
        course.setTermEnd(request.termEnd());
    }

    private static String blankToNull(final String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

}
