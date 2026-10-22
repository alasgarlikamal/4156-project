package com.perihelionApi.course;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.perihelionApi.common.ConflictException;
import com.perihelionApi.common.NotFoundException;
import com.perihelionApi.enrollment.EnrollmentRepository;

@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

	@Mock
	private CourseRepository courseRepository;

	@Mock
	private EnrollmentRepository enrollmentRepository;

	@InjectMocks
	private CourseService courseService;

	@Test
	void createStoresNullWhenCodeIsBlank() {
		when(courseRepository.save(any(Course.class))).thenAnswer(invocation -> {
			Course course = invocation.getArgument(0);
			course.setId(1L);
			return course;
		});

		CourseResponse response = courseService.create(new CourseRequest("   ", "Advanced Software Engineering",
				LocalDate.of(2026, 1, 20), LocalDate.of(2026, 5, 10)));

		assertNull(response.code());
		ArgumentCaptor<Course> captor = ArgumentCaptor.forClass(Course.class);
		verify(courseRepository).save(captor.capture());
		assertNull(captor.getValue().getCode());
		assertEquals("Advanced Software Engineering", captor.getValue().getTitle());
		assertEquals(LocalDate.of(2026, 1, 20), captor.getValue().getTermStart());
		assertEquals(LocalDate.of(2026, 5, 10), captor.getValue().getTermEnd());
	}

	@Test
	void deleteRejectsCourseWithEnrollments() {
		when(courseRepository.existsById(1L)).thenReturn(true);
		when(enrollmentRepository.existsByCourse_Id(1L)).thenReturn(true);

		ConflictException exception = assertThrows(ConflictException.class, () -> courseService.delete(1L));

		assertEquals("Course cannot be deleted while enrollments exist", exception.getMessage());
		verify(courseRepository, never()).deleteById(any());
	}

	@Test
	void deleteRejectsMissingCourse() {
		when(courseRepository.existsById(9L)).thenReturn(false);

		assertThrows(NotFoundException.class, () -> courseService.delete(9L));
		verify(enrollmentRepository, never()).existsByCourse_Id(any());
		verify(courseRepository, never()).deleteById(any());
	}

}
