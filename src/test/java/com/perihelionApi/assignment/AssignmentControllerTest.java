package com.perihelionApi.assignment;

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
import java.time.LocalDate;
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

@WebMvcTest(AssignmentController.class)
@Import(GlobalExceptionHandler.class)
class AssignmentControllerTest {

    private static final String URL = "/api/assignments";
    private static final String BODY = """
            {"courseId":2,"title":"Project proposal",
             "dueDate":"2026-11-01","estimatedWorkHours":4.5}
            """;
    private static final AssignmentRequest REQUEST = new AssignmentRequest(
            2L, "Project proposal", LocalDate.of(2026, 11, 1),
            new BigDecimal("4.5"));
    private static final AssignmentResponse RESPONSE = new AssignmentResponse(
            1L, 2L, "Project proposal", LocalDate.of(2026, 11, 1),
            new BigDecimal("4.5"));

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AssignmentService assignmentService;

    @Test
    void createReturnsCreatedAssignment() throws Exception {
        when(assignmentService.create(REQUEST)).thenReturn(RESPONSE);
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
                .content(BODY)).andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.courseId").value(2))
                .andExpect(jsonPath("$.title").value("Project proposal"))
                .andExpect(jsonPath("$.dueDate").value("2026-11-01"))
                .andExpect(jsonPath("$.estimatedWorkHours").value(4.5));
        verify(assignmentService).create(REQUEST);
    }

    @Test
    void listReturnsAssignmentsWithoutFilter() throws Exception {
        when(assignmentService.list(null)).thenReturn(List.of(RESPONSE));
        mockMvc.perform(get(URL)).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
        verify(assignmentService).list(null);
    }

    @Test
    void listPassesCourseFilterAndReturnsEmptyList() throws Exception {
        when(assignmentService.list(2L)).thenReturn(List.of());
        mockMvc.perform(get(URL).param("courseId", "2"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
        verify(assignmentService).list(2L);
    }

    @Test
    void getReturnsAssignment() throws Exception {
        when(assignmentService.get(1L)).thenReturn(RESPONSE);
        mockMvc.perform(get(URL + "/1")).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
        verify(assignmentService).get(1L);
    }

    @Test
    void updateReturnsReplacedAssignment() throws Exception {
        when(assignmentService.update(1L, REQUEST)).thenReturn(RESPONSE);
        mockMvc.perform(put(URL + "/1")
                .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Project proposal"));
        verify(assignmentService).update(1L, REQUEST);
    }

    @Test
    void deleteReturnsNoContent() throws Exception {
        mockMvc.perform(delete(URL + "/1"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
        verify(assignmentService).delete(1L);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "null", "{", ""})
    void createRejectsMissingOrMalformedBody(final String body)
            throws Exception {
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
                .content(body)).andExpect(status().isBadRequest());
        verifyNoInteractions(assignmentService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"courseId", "title", "dueDate",
            "estimatedWorkHours"})
    void createRejectsMissingRequiredFields(final String field)
            throws Exception {
        String body = BODY.replaceAll("\"" + field
                + "\":(?:\"[^\"]*\"|[0-9.]+)",
                "\"" + field + "\":null");
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
                .content(body)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors." + field).exists());
        verifyNoInteractions(assignmentService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1"})
    void createRejectsNonPositiveCourseId(final String id) throws Exception {
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
                .content(BODY.replace("\"courseId\":2", "\"courseId\":" + id)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.courseId").exists());
        verifyNoInteractions(assignmentService);
    }

    @Test
    void updateValidatesRequest() throws Exception {
        mockMvc.perform(put(URL + "/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY.replace("4.5", "-1")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.estimatedWorkHours")
                        .exists());
        verifyNoInteractions(assignmentService);
    }

    @ParameterizedTest
    @ValueSource(strings = {" ", "LONG"})
    void createRejectsInvalidTitle(final String title) throws Exception {
        String value = title.equals("LONG") ? "x".repeat(256) : title;
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
                .content(BODY.replace("Project proposal", value)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.title").exists());
        verifyNoInteractions(assignmentService);
    }

    @Test
    void createAllowsZeroEffort() throws Exception {
        when(assignmentService.create(any())).thenReturn(RESPONSE);
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
                .content(BODY.replace("4.5", "0")))
                .andExpect(status().isCreated());
    }

    @Test
    void createRejectsInvalidDate() throws Exception {
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
                .content(BODY.replace("2026-11-01", "not-a-date")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Malformed request body"));
        verifyNoInteractions(assignmentService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/bad-id", "?courseId=bad-id"})
    void rejectsInvalidParameters(final String suffix) throws Exception {
        mockMvc.perform(get(URL + suffix))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(assignmentService);
    }

    @Test
    void getReturnsNotFound() throws Exception {
        when(assignmentService.get(99L))
                .thenThrow(new NotFoundException("Assignment not found"));
        mockMvc.perform(get(URL + "/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Assignment not found"))
                .andExpect(jsonPath("$.fieldErrors").isEmpty());
    }

    @Test
    void createReturnsNotFoundForMissingCourse() throws Exception {
        when(assignmentService.create(REQUEST))
                .thenThrow(new NotFoundException("Course not found"));
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
                .content(BODY)).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Course not found"));
    }

    @Test
    void updateReturnsNotFound() throws Exception {
        when(assignmentService.update(99L, REQUEST))
                .thenThrow(new NotFoundException("Assignment not found"));
        mockMvc.perform(put(URL + "/99")
                .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteReturnsNotFound() throws Exception {
        doThrow(new NotFoundException("Assignment not found"))
                .when(assignmentService).delete(99L);
        mockMvc.perform(delete(URL + "/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteReturnsConflict() throws Exception {
        doThrow(new ConflictException("Assignment is still referenced"))
                .when(assignmentService).delete(1L);
        mockMvc.perform(delete(URL + "/1"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("Assignment is still referenced"));
    }
}
