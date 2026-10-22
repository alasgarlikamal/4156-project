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

@Service
@Transactional
public class EnrollmentService {

	private final EnrollmentRepository enrollmentRepository;

	private final CourseRepository courseRepository;

	public EnrollmentService(EnrollmentRepository enrollmentRepository, CourseRepository courseRepository) {
		this.enrollmentRepository = enrollmentRepository;
		this.courseRepository = courseRepository;
	}

	public EnrollmentResponse create(EnrollmentRequest request) {
		Course course = findCourse(request.courseId());
		String learnerRef = request.learnerRef().toString();
		if (enrollmentRepository.existsByCourse_IdAndLearnerRef(course.getId(), learnerRef)) {
			throw new ConflictException("An enrollment already exists for this course and learner");
		}
		Enrollment enrollment = new Enrollment();
		enrollment.setCourse(course);
		enrollment.setLearnerRef(learnerRef);
		return EnrollmentResponse.from(enrollmentRepository.save(enrollment));
	}

	@Transactional(readOnly = true)
	public EnrollmentResponse get(Long id) {
		return EnrollmentResponse.from(findEnrollment(id));
	}

	@Transactional(readOnly = true)
	public List<EnrollmentResponse> list(Long courseId, UUID learnerRef) {
		String learner = learnerRef == null ? null : learnerRef.toString();
		List<Enrollment> enrollments;
		if (courseId != null && learner != null) {
			enrollments = enrollmentRepository.findByCourse_IdAndLearnerRefOrderByIdAsc(courseId, learner);
		}
		else if (courseId != null) {
			enrollments = enrollmentRepository.findByCourse_IdOrderByIdAsc(courseId);
		}
		else if (learner != null) {
			enrollments = enrollmentRepository.findByLearnerRefOrderByIdAsc(learner);
		}
		else {
			enrollments = enrollmentRepository.findAllByOrderByIdAsc();
		}
		return enrollments.stream().map(EnrollmentResponse::from).toList();
	}

	public EnrollmentResponse update(Long id, EnrollmentRequest request) {
		Enrollment enrollment = findEnrollment(id);
		Course course = findCourse(request.courseId());
		String learnerRef = request.learnerRef().toString();
		if (enrollmentRepository.existsByCourse_IdAndLearnerRefAndIdNot(course.getId(), learnerRef, id)) {
			throw new ConflictException("An enrollment already exists for this course and learner");
		}
		enrollment.setCourse(course);
		enrollment.setLearnerRef(learnerRef);
		return EnrollmentResponse.from(enrollmentRepository.save(enrollment));
	}

	public void delete(Long id) {
		if (!enrollmentRepository.existsById(id)) {
			throw new NotFoundException("Enrollment not found");
		}
		enrollmentRepository.deleteById(id);
	}

	private Course findCourse(Long courseId) {
		return courseRepository.findById(courseId).orElseThrow(() -> new NotFoundException("Course not found"));
	}

	private Enrollment findEnrollment(Long id) {
		return enrollmentRepository.findById(id).orElseThrow(() -> new NotFoundException("Enrollment not found"));
	}

}
