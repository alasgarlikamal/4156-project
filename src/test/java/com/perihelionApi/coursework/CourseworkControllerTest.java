/*
    AI usage: Coursework tests: Grok 4.7, ~238K tokens
 */
package com.perihelionApi.coursework;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.perihelionApi.common.ConflictException;
import com.perihelionApi.common.GlobalExceptionHandler;
import com.perihelionApi.common.NotFoundException;

@WebMvcTest(CourseworkController.class)
@Import(GlobalExceptionHandler.class)
class CourseworkControllerTest {

    private static final String URL = "/api/coursework";
    private static final String BODY = """
            {"courseId":2,"kind":"ASSIGNMENT","title":"Project proposal",
             "deadline":"2026-11-01T23:59:00Z","estimatedEffortHours":4.5}
            """;
    private static final Instant DEADLINE = Instant.parse("2026-11-01T23:59:00Z");
    private static final CourseworkRequest REQUEST = new CourseworkRequest(2L,
            CourseworkKind.ASSIGNMENT, "Project proposal", DEADLINE, new BigDecimal("4.5"));
    private static final CourseworkResponse RESPONSE = new CourseworkResponse(1L, 2L,
            CourseworkKind.ASSIGNMENT, "Project proposal", DEADLINE, new BigDecimal("4.5"));

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CourseworkService courseworkService;

    @Test
    void createReturnsCreatedCoursework() throws Exception {
        when(courseworkService.create(REQUEST)).thenReturn(RESPONSE);

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.courseId").value(2))
                .andExpect(jsonPath("$.kind").value("ASSIGNMENT"))
                .andExpect(jsonPath("$.title").value("Project proposal"))
                .andExpect(jsonPath("$.deadline").value("2026-11-01T23:59:00Z"))
                .andExpect(jsonPath("$.estimatedEffortHours").value(4.5));
    }

    @Test
    void createAcceptsExamKind() throws Exception {
        when(courseworkService.create(any())).thenReturn(RESPONSE);

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
                .content(BODY.replace("ASSIGNMENT", "EXAM"))).andExpect(status().isCreated());
    }

    @Test
    void createRejectsUnknownKind() throws Exception {
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
                .content(BODY.replace("ASSIGNMENT", "QUIZ"))).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed request body"));

        verifyNoInteractions(courseworkService);
    }

    @ParameterizedTest
    @ValueSource(strings = { "courseId", "kind", "title", "deadline", "estimatedEffortHours" })
    void createRejectsMissingRequiredFields(String field) throws Exception {
        String body = BODY.replaceAll("\"" + field + "\":(?:\"[^\"]*\"|[0-9.]+)",
                "\"" + field + "\":null");

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors." + field).exists());

        verifyNoInteractions(courseworkService);
    }

    @Test
    void createRejectsNegativeEffort() throws Exception {
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
                .content(BODY.replace("4.5", "-1"))).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.estimatedEffortHours").exists());

        verifyNoInteractions(courseworkService);
    }

    @Test
    void createReturnsNotFoundForMissingCourse() throws Exception {
        when(courseworkService.create(REQUEST)).thenThrow(new NotFoundException("Course not found"));

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Course not found"));
    }

    @Test
    void listPassesCourseAndKindFilters() throws Exception {
        when(courseworkService.list(2L, CourseworkKind.EXAM)).thenReturn(List.of(RESPONSE));

        mockMvc.perform(get(URL).param("courseId", "2").param("kind", "EXAM"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));

        verify(courseworkService).list(2L, CourseworkKind.EXAM);
    }

    @Test
    void listWithoutFiltersPassesNulls() throws Exception {
        when(courseworkService.list(null, null)).thenReturn(List.of());

        mockMvc.perform(get(URL)).andExpect(status().isOk()).andExpect(content().json("[]"));

        verify(courseworkService).list(null, null);
    }

    @Test
    void listRejectsUnknownKindFilter() throws Exception {
        mockMvc.perform(get(URL).param("kind", "QUIZ")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid value for kind"));

        verifyNoInteractions(courseworkService);
    }

    @Test
    void getReturnsNotFound() throws Exception {
        when(courseworkService.get(99L)).thenThrow(new NotFoundException("Coursework not found"));

        mockMvc.perform(get(URL + "/99")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Coursework not found"));
    }

    @Test
    void updateReturnsReplacedCoursework() throws Exception {
        when(courseworkService.update(1L, REQUEST)).thenReturn(RESPONSE);

        mockMvc.perform(put(URL + "/1").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Project proposal"));
    }

    @Test
    void deleteReturnsNoContent() throws Exception {
        mockMvc.perform(delete(URL + "/1")).andExpect(status().isNoContent());

        verify(courseworkService).delete(1L);
    }

    @Test
    void deleteReturnsConflictWhenReferenced() throws Exception {
        doThrow(new ConflictException("Coursework cannot be deleted while submissions exist"))
                .when(courseworkService).delete(1L);

        mockMvc.perform(delete(URL + "/1")).andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("Coursework cannot be deleted while submissions exist"));
    }
}
