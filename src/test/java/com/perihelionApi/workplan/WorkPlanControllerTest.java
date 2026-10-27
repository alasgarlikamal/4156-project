/*
    AI usage: Work plan tests: Grok 4.7, ~238K tokens
 */
package com.perihelionApi.workplan;

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

@WebMvcTest(WorkPlanController.class)
@Import(GlobalExceptionHandler.class)
class WorkPlanControllerTest {

    private static final String URL = "/api/workplans";
    private static final String BODY = """
            {"enrollmentId":2,"rangeStart":"2026-10-01",
             "rangeEnd":"2026-12-15","availableHoursPerDay":3.5}
            """;
    private static final WorkPlanRequest REQUEST = new WorkPlanRequest(2L,
            LocalDate.of(2026, 10, 1), LocalDate.of(2026, 12, 15),
            new BigDecimal("3.5"));
    private static final WorkPlanResponse RESPONSE = new WorkPlanResponse(1L, 2L,
            LocalDate.of(2026, 10, 1), LocalDate.of(2026, 12, 15),
            new BigDecimal("3.5"), Instant.parse("2026-09-01T12:00:00Z"));

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WorkPlanService workPlanService;

    @Test
    void createReturnsCreatedWorkPlan() throws Exception {
        when(workPlanService.create(REQUEST)).thenReturn(RESPONSE);

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.enrollmentId").value(2))
                .andExpect(jsonPath("$.availableHoursPerDay").value(3.5))
                .andExpect(jsonPath("$.createdAt").value("2026-09-01T12:00:00Z"));
    }

    @Test
    void createRejectsRangeStartNotBeforeRangeEnd() throws Exception {
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content("""
                {"enrollmentId":2,"rangeStart":"2026-12-15",
                 "rangeEnd":"2026-10-01","availableHoursPerDay":3.5}
                """)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.rangeValid")
                        .value("rangeStart must be earlier than rangeEnd"));

        verifyNoInteractions(workPlanService);
    }

    @ParameterizedTest
    @ValueSource(strings = { "-0.5", "24.5" })
    void createRejectsHoursOutsideZeroToTwentyFour(String hours) throws Exception {
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content("""
                {"enrollmentId":2,"rangeStart":"2026-10-01",
                 "rangeEnd":"2026-12-15","availableHoursPerDay":%s}
                """.formatted(hours))).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.availableHoursPerDay").exists());

        verifyNoInteractions(workPlanService);
    }

    @Test
    void createRejectsMissingFields() throws Exception {
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.enrollmentId").exists())
                .andExpect(jsonPath("$.fieldErrors.rangeStart").exists())
                .andExpect(jsonPath("$.fieldErrors.rangeEnd").exists())
                .andExpect(jsonPath("$.fieldErrors.availableHoursPerDay").exists());

        verifyNoInteractions(workPlanService);
    }

    @Test
    void createReturnsNotFoundForMissingEnrollment() throws Exception {
        when(workPlanService.create(any())).thenThrow(new NotFoundException("Enrollment not found"));

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Enrollment not found"));
    }

    @Test
    void listPassesEnrollmentFilter() throws Exception {
        when(workPlanService.list(2L)).thenReturn(List.of(RESPONSE));

        mockMvc.perform(get(URL).param("enrollmentId", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));

        verify(workPlanService).list(2L);
    }

    @Test
    void listWithoutFilterPassesNull() throws Exception {
        when(workPlanService.list(null)).thenReturn(List.of());

        mockMvc.perform(get(URL)).andExpect(status().isOk()).andExpect(content().json("[]"));

        verify(workPlanService).list(null);
    }

    @Test
    void getReturnsNotFound() throws Exception {
        when(workPlanService.get(99L)).thenThrow(new NotFoundException("Work plan not found"));

        mockMvc.perform(get(URL + "/99")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Work plan not found"));
    }

    @Test
    void updateReturnsUpdatedWorkPlan() throws Exception {
        when(workPlanService.update(1L, REQUEST)).thenReturn(RESPONSE);

        mockMvc.perform(put(URL + "/1").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void deleteReturnsNoContent() throws Exception {
        mockMvc.perform(delete(URL + "/1")).andExpect(status().isNoContent());

        verify(workPlanService).delete(1L);
    }

    @Test
    void deleteReturnsConflictWhenTasksExist() throws Exception {
        doThrow(new ConflictException("Work plan cannot be deleted while tasks exist"))
                .when(workPlanService).delete(1L);

        mockMvc.perform(delete(URL + "/1")).andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Work plan cannot be deleted while tasks exist"));
    }
}
