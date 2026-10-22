package com.perihelionApi.course;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
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

@WebMvcTest(CourseController.class)
@Import(GlobalExceptionHandler.class)
class CourseControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private CourseService courseService;

	@Test
	void createReturnsCreatedCourse() throws Exception {
		when(courseService.create(any(CourseRequest.class))).thenReturn(new CourseResponse(1L, "COMS W4156",
				"Advanced Software Engineering", LocalDate.of(2026, 1, 20), LocalDate.of(2026, 5, 10)));

		mockMvc.perform(post("/api/courses").contentType(MediaType.APPLICATION_JSON).content("""
				{"code":"COMS W4156","title":"Advanced Software Engineering","termStart":"2026-01-20","termEnd":"2026-05-10"}
				""")).andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(1))
				.andExpect(jsonPath("$.code").value("COMS W4156"));
	}

	@Test
	void createAllowsMissingCode() throws Exception {
		when(courseService.create(any(CourseRequest.class))).thenReturn(new CourseResponse(2L, null,
				"Advanced Software Engineering", LocalDate.of(2026, 1, 20), LocalDate.of(2026, 5, 10)));

		mockMvc.perform(post("/api/courses").contentType(MediaType.APPLICATION_JSON).content("""
				{"title":"Advanced Software Engineering","termStart":"2026-01-20","termEnd":"2026-05-10"}
				""")).andExpect(status().isCreated()).andExpect(jsonPath("$.code").doesNotExist());
	}

	@Test
	void createRejectsCodeThatDoesNotMatchVergilForm() throws Exception {
		mockMvc.perform(post("/api/courses").contentType(MediaType.APPLICATION_JSON).content("""
				{"code":"coms4156","title":"Advanced Software Engineering","termStart":"2026-01-20","termEnd":"2026-05-10"}
				""")).andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors.code")
						.value("code must look like COMS W4156"));

		verify(courseService, never()).create(any());
	}

	@Test
	void createRejectsTermStartThatIsNotEarlierThanTermEnd() throws Exception {
		mockMvc.perform(post("/api/courses").contentType(MediaType.APPLICATION_JSON).content("""
				{"code":"COMS W4156","title":"Advanced Software Engineering","termStart":"2026-05-10","termEnd":"2026-01-20"}
				""")).andExpect(status().isBadRequest())
				.andExpect(content().string(containsString("termStart must be earlier than termEnd")));

		verify(courseService, never()).create(any());
	}

	@Test
	void getReturnsNotFound() throws Exception {
		when(courseService.get(99L)).thenThrow(new NotFoundException("Course not found"));

		mockMvc.perform(get("/api/courses/99")).andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("Course not found"))
				.andExpect(jsonPath("$.fieldErrors").isEmpty());
	}

	@Test
	void deleteReturnsConflictWhenEnrollmentsExist() throws Exception {
		doThrow(new ConflictException("Course cannot be deleted while enrollments exist")).when(courseService)
				.delete(1L);

		mockMvc.perform(delete("/api/courses/1")).andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("Course cannot be deleted while enrollments exist"));
	}

	@Test
	void listPassesTitleAndCodeFilters() throws Exception {
		when(courseService.list("software", "COMS W4156")).thenReturn(List.of());

		mockMvc.perform(get("/api/courses").param("title", "software").param("code", "COMS W4156"))
				.andExpect(status().isOk());

		verify(courseService).list("software", "COMS W4156");
	}

}
