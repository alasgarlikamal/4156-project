/*
	AI usage: Course and Enrollment Tests: Grok 4.7, ~111K tokens
 */
package com.perihelionApi.enrollment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.perihelionApi.common.ConflictException;
import com.perihelionApi.common.NotFoundException;
import com.perihelionApi.course.Course;
import com.perihelionApi.course.CourseRepository;
import com.perihelionApi.submission.SubmissionRepository;
import com.perihelionApi.workplan.WorkPlanRepository;

@ExtendWith(MockitoExtension.class)
class EnrollmentServiceTest {

	private static final UUID LEARNER = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

	@Mock
	private EnrollmentRepository enrollmentRepository;

	@Mock
	private CourseRepository courseRepository;

	@Mock
	private SubmissionRepository submissionRepository;

	@Mock
	private WorkPlanRepository workPlanRepository;

	@InjectMocks
	private EnrollmentService enrollmentService;

	@Test
	void createRejectsMissingCourse() {
		when(courseRepository.findById(9L)).thenReturn(Optional.empty());

		assertThrows(NotFoundException.class, () -> enrollmentService.create(new EnrollmentRequest(9L, LEARNER)));
		verify(enrollmentRepository, never()).save(any());
	}

	@Test
	void createRejectsDuplicateCourseAndLearner() {
		Course course = course(1L);
		when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
		when(enrollmentRepository.existsByCourseIdAndLearnerRef(1L, LEARNER.toString())).thenReturn(true);

		ConflictException exception = assertThrows(ConflictException.class,
				() -> enrollmentService.create(new EnrollmentRequest(1L, LEARNER)));

		assertEquals("An enrollment already exists for this course and learner", exception.getMessage());
		verify(enrollmentRepository, never()).save(any());
	}

	@Test
	void createSavesEnrollmentForExistingCourse() {
		Course course = course(4L);
		when(courseRepository.findById(4L)).thenReturn(Optional.of(course));
		when(enrollmentRepository.existsByCourseIdAndLearnerRef(4L, LEARNER.toString())).thenReturn(false);
		when(enrollmentRepository.save(any(Enrollment.class))).thenAnswer(invocation -> {
			Enrollment enrollment = invocation.getArgument(0);
			enrollment.setId(8L);
			return enrollment;
		});

		EnrollmentResponse response = enrollmentService.create(new EnrollmentRequest(4L, LEARNER));

		assertEquals(8L, response.id());
		assertEquals(4L, response.courseId());
		assertEquals(LEARNER, response.learnerRef());
		ArgumentCaptor<Enrollment> captor = ArgumentCaptor.forClass(Enrollment.class);
		verify(enrollmentRepository).save(captor.capture());
		assertEquals(course, captor.getValue().getCourse());
		assertEquals(LEARNER.toString(), captor.getValue().getLearnerRef());
	}

	@Test
	void deleteRejectsEnrollmentWithSubmissions() {
		when(enrollmentRepository.existsById(8L)).thenReturn(true);
		when(submissionRepository.existsByEnrollmentId(8L)).thenReturn(true);

		ConflictException exception = assertThrows(ConflictException.class, () -> enrollmentService.delete(8L));

		assertEquals("Enrollment cannot be deleted while submissions exist", exception.getMessage());
		verify(enrollmentRepository, never()).deleteById(any());
	}

	@Test
	void deleteRejectsEnrollmentWithWorkPlans() {
		when(enrollmentRepository.existsById(8L)).thenReturn(true);
		when(submissionRepository.existsByEnrollmentId(8L)).thenReturn(false);
		when(workPlanRepository.existsByEnrollmentId(8L)).thenReturn(true);

		ConflictException exception = assertThrows(ConflictException.class, () -> enrollmentService.delete(8L));

		assertEquals("Enrollment cannot be deleted while work plans exist", exception.getMessage());
		verify(enrollmentRepository, never()).deleteById(any());
	}

	@Test
	void deleteRemovesEnrollmentWithoutDependents() {
		when(enrollmentRepository.existsById(8L)).thenReturn(true);
		when(submissionRepository.existsByEnrollmentId(8L)).thenReturn(false);
		when(workPlanRepository.existsByEnrollmentId(8L)).thenReturn(false);

		enrollmentService.delete(8L);

		verify(enrollmentRepository).deleteById(8L);
	}

	private static Course course(Long id) {
		Course course = new Course();
		course.setId(id);
		return course;
	}

}
