package com.perihelionApi.course;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.perihelionApi.common.ConflictException;
import com.perihelionApi.common.NotFoundException;
import com.perihelionApi.enrollment.EnrollmentRepository;

@Service
@Transactional
public class CourseService {

	private final CourseRepository courseRepository;

	private final EnrollmentRepository enrollmentRepository;

	public CourseService(CourseRepository courseRepository, EnrollmentRepository enrollmentRepository) {
		this.courseRepository = courseRepository;
		this.enrollmentRepository = enrollmentRepository;
	}

	public CourseResponse create(CourseRequest request) {
		Course course = new Course();
		apply(course, request);
		return CourseResponse.from(courseRepository.save(course));
	}

	@Transactional(readOnly = true)
	public CourseResponse get(Long id) {
		return CourseResponse.from(findCourse(id));
	}

	@Transactional(readOnly = true)
	public List<CourseResponse> list(String title, String code) {
		String normalizedTitle = blankToNull(title);
		String normalizedCode = blankToNull(code);
		List<Course> courses;
		if (normalizedTitle != null && normalizedCode != null) {
			courses = courseRepository.findByTitleContainingIgnoreCaseAndCodeOrderByTitleAsc(normalizedTitle,
					normalizedCode);
		}
		else if (normalizedTitle != null) {
			courses = courseRepository.findByTitleContainingIgnoreCaseOrderByTitleAsc(normalizedTitle);
		}
		else if (normalizedCode != null) {
			courses = courseRepository.findByCodeOrderByTitleAsc(normalizedCode);
		}
		else {
			courses = courseRepository.findAllByOrderByTitleAsc();
		}
		return courses.stream().map(CourseResponse::from).toList();
	}

	public CourseResponse update(Long id, CourseRequest request) {
		Course course = findCourse(id);
		apply(course, request);
		return CourseResponse.from(courseRepository.save(course));
	}

	public void delete(Long id) {
		if (!courseRepository.existsById(id)) {
			throw new NotFoundException("Course not found");
		}
		if (enrollmentRepository.existsByCourse_Id(id)) {
			throw new ConflictException("Course cannot be deleted while enrollments exist");
		}
		courseRepository.deleteById(id);
	}

	private Course findCourse(Long id) {
		return courseRepository.findById(id).orElseThrow(() -> new NotFoundException("Course not found"));
	}

	private void apply(Course course, CourseRequest request) {
		course.setCode(blankToNull(request.code()));
		course.setTitle(request.title());
		course.setTermStart(request.termStart());
		course.setTermEnd(request.termEnd());
	}

	private static String blankToNull(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return value.trim();
	}

}
