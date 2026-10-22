/*
	AI usage: Course and Enrollment Tests: Grok 4.7, ~111K tokens
 */
package com.perihelionApi.enrollment;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.perihelionApi.common.ConflictException;
import com.perihelionApi.common.GlobalExceptionHandler;
import com.perihelionApi.common.NotFoundException;

@WebMvcTest(EnrollmentController.class)
@Import(GlobalExceptionHandler.class)
class EnrollmentControllerTest {

	private static final UUID LEARNER = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private EnrollmentService enrollmentService;

	@Test
	void createReturnsCreatedEnrollment() throws Exception {
		when(enrollmentService.create(any(EnrollmentRequest.class)))
				.thenReturn(new EnrollmentResponse(8L, 4L, LEARNER));

		mockMvc.perform(post("/api/enrollments").contentType(MediaType.APPLICATION_JSON).content("""
				{"courseId":4,"learnerRef":"550e8400-e29b-41d4-a716-446655440000"}
				""")).andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(8))
				.andExpect(jsonPath("$.courseId").value(4))
				.andExpect(jsonPath("$.learnerRef").value(LEARNER.toString()));
	}

	@Test
	void createRejectsMalformedLearnerRef() throws Exception {
		mockMvc.perform(post("/api/enrollments").contentType(MediaType.APPLICATION_JSON).content("""
				{"courseId":4,"learnerRef":"not-a-uuid"}
				""")).andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("Malformed request body"))
				.andExpect(jsonPath("$.fieldErrors").isEmpty());

		verify(enrollmentService, never()).create(any());
	}

	@Test
	void createReturnsNotFoundWhenCourseIsMissing() throws Exception {
		when(enrollmentService.create(any(EnrollmentRequest.class)))
				.thenThrow(new NotFoundException("Course not found"));

		mockMvc.perform(post("/api/enrollments").contentType(MediaType.APPLICATION_JSON).content("""
				{"courseId":9,"learnerRef":"550e8400-e29b-41d4-a716-446655440000"}
				""")).andExpect(status().isNotFound()).andExpect(jsonPath("$.message").value("Course not found"));
	}

	@Test
	void createReturnsConflictForDuplicateEnrollment() throws Exception {
		when(enrollmentService.create(any(EnrollmentRequest.class)))
				.thenThrow(new ConflictException("An enrollment already exists for this course and learner"));

		mockMvc.perform(post("/api/enrollments").contentType(MediaType.APPLICATION_JSON).content("""
				{"courseId":4,"learnerRef":"550e8400-e29b-41d4-a716-446655440000"}
				""")).andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("An enrollment already exists for this course and learner"));
	}

	@Test
	void getReturnsNotFound() throws Exception {
		when(enrollmentService.get(3L)).thenThrow(new NotFoundException("Enrollment not found"));

		mockMvc.perform(get("/api/enrollments/3")).andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("Enrollment not found"));
	}

}
