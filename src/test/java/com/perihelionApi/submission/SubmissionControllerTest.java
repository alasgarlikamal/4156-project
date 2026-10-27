/*
	AI usage: Submission tests: Grok 4.7, ~238K tokens
 */
package com.perihelionApi.submission;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

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

@WebMvcTest(SubmissionController.class)
@Import(GlobalExceptionHandler.class)
class SubmissionControllerTest {

	private static final Instant SUBMITTED_AT = Instant.parse("2026-10-20T15:30:00Z");

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private SubmissionService submissionService;

	@Test
	void createReturnsCreatedSubmission() throws Exception {
		when(submissionService.create(any(SubmissionRequest.class)))
				.thenReturn(new SubmissionResponse(8L, 4L, 3L, SUBMITTED_AT));

		mockMvc.perform(post("/api/submissions").contentType(MediaType.APPLICATION_JSON).content("""
				{"enrollmentId":4,"courseworkId":3,"submittedAt":"2026-10-20T15:30:00Z"}
				""")).andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(8))
				.andExpect(jsonPath("$.enrollmentId").value(4)).andExpect(jsonPath("$.courseworkId").value(3))
				.andExpect(jsonPath("$.submittedAt").value("2026-10-20T15:30:00Z"));
	}

	@Test
	void createRejectsMissingFields() throws Exception {
		mockMvc.perform(post("/api/submissions").contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("Validation failed"))
				.andExpect(jsonPath("$.fieldErrors.enrollmentId").exists())
				.andExpect(jsonPath("$.fieldErrors.courseworkId").exists());

		verify(submissionService, never()).create(any());
	}

	@Test
	void createRejectsMalformedTimestamp() throws Exception {
		mockMvc.perform(post("/api/submissions").contentType(MediaType.APPLICATION_JSON).content("""
				{"enrollmentId":4,"courseworkId":3,"submittedAt":"yesterday"}
				""")).andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("Malformed request body"));

		verify(submissionService, never()).create(any());
	}

	@Test
	void createReturnsNotFoundWhenEnrollmentIsMissing() throws Exception {
		when(submissionService.create(any(SubmissionRequest.class)))
				.thenThrow(new NotFoundException("Enrollment not found"));

		mockMvc.perform(post("/api/submissions").contentType(MediaType.APPLICATION_JSON).content("""
				{"enrollmentId":9,"courseworkId":3}
				""")).andExpect(status().isNotFound()).andExpect(jsonPath("$.message").value("Enrollment not found"));
	}

	@Test
	void createReturnsNotFoundWhenCourseworkIsMissing() throws Exception {
		when(submissionService.create(any(SubmissionRequest.class)))
				.thenThrow(new NotFoundException("Coursework not found"));

		mockMvc.perform(post("/api/submissions").contentType(MediaType.APPLICATION_JSON).content("""
				{"enrollmentId":4,"courseworkId":99}
				""")).andExpect(status().isNotFound()).andExpect(jsonPath("$.message").value("Coursework not found"));
	}

	@Test
	void createReturnsConflictForCourseworkFromAnotherCourse() throws Exception {
		when(submissionService.create(any(SubmissionRequest.class)))
				.thenThrow(new ConflictException("Coursework must belong to the enrollment's course"));

		mockMvc.perform(post("/api/submissions").contentType(MediaType.APPLICATION_JSON).content("""
				{"enrollmentId":4,"courseworkId":3}
				""")).andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("Coursework must belong to the enrollment's course"));
	}

	@Test
	void createReturnsConflictForDuplicateSubmission() throws Exception {
		when(submissionService.create(any(SubmissionRequest.class)))
				.thenThrow(new ConflictException("A submission already exists for this enrollment and coursework"));

		mockMvc.perform(post("/api/submissions").contentType(MediaType.APPLICATION_JSON).content("""
				{"enrollmentId":4,"courseworkId":3}
				""")).andExpect(status().isConflict());
	}

	@Test
	void listPassesFiltersToService() throws Exception {
		when(submissionService.list(4L, null)).thenReturn(List.of(new SubmissionResponse(8L, 4L, 3L, SUBMITTED_AT)));

		mockMvc.perform(get("/api/submissions").param("enrollmentId", "4")).andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").value(8));
	}

	@Test
	void listRejectsNonNumericFilter() throws Exception {
		mockMvc.perform(get("/api/submissions").param("courseworkId", "abc")).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Invalid value for courseworkId"));
	}

	@Test
	void getReturnsNotFound() throws Exception {
		when(submissionService.get(3L)).thenThrow(new NotFoundException("Submission not found"));

		mockMvc.perform(get("/api/submissions/3")).andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("Submission not found"));
	}

	@Test
	void updateReturnsUpdatedSubmission() throws Exception {
		when(submissionService.update(any(Long.class), any(SubmissionRequest.class)))
				.thenReturn(new SubmissionResponse(8L, 4L, 7L, SUBMITTED_AT));

		mockMvc.perform(put("/api/submissions/8").contentType(MediaType.APPLICATION_JSON).content("""
				{"enrollmentId":4,"courseworkId":7}
				""")).andExpect(status().isOk()).andExpect(jsonPath("$.courseworkId").value(7));
	}

	@Test
	void deleteReturnsNoContent() throws Exception {
		mockMvc.perform(delete("/api/submissions/8")).andExpect(status().isNoContent());

		verify(submissionService).delete(8L);
	}

}
