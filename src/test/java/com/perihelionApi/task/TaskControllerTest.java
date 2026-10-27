/*
    AI usage: Task tests: Grok 4.7, ~238K tokens
 */
package com.perihelionApi.task;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
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

@WebMvcTest(TaskController.class)
@Import(GlobalExceptionHandler.class)
class TaskControllerTest {

    private static final String URL = "/api/tasks";
    private static final String BODY = """
            {"workPlanId":10,"courseworkId":20,"enrollmentId":30,
             "title":"Read chapter 3","targetDate":"2026-10-15",
             "estimatedWorkHours":2.5}
            """;
    private static final TaskRequest REQUEST = new TaskRequest(10L, 20L, 30L,
            "Read chapter 3", LocalDate.of(2026, 10, 15), new BigDecimal("2.5"));
    private static final TaskResponse RESPONSE = new TaskResponse(1L, 10L, 20L, 30L,
            "Read chapter 3", LocalDate.of(2026, 10, 15), new BigDecimal("2.5"));

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TaskService taskService;

    @Test
    void createReturnsCreatedTask() throws Exception {
        when(taskService.create(REQUEST)).thenReturn(RESPONSE);

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.workPlanId").value(10))
                .andExpect(jsonPath("$.courseworkId").value(20))
                .andExpect(jsonPath("$.enrollmentId").value(30))
                .andExpect(jsonPath("$.title").value("Read chapter 3"));
    }

    @Test
    void createRejectsBlankTitleAndNegativeHours() throws Exception {
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content("""
                {"workPlanId":10,"courseworkId":20,"enrollmentId":30,
                 "title":"   ","targetDate":"2026-10-15","estimatedWorkHours":-1}
                """)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.title").exists())
                .andExpect(jsonPath("$.fieldErrors.estimatedWorkHours").exists());

        verifyNoInteractions(taskService);
    }

    @Test
    void createRejectsMissingReferences() throws Exception {
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content("""
                {"title":"Read","targetDate":"2026-10-15","estimatedWorkHours":1}
                """)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.workPlanId").exists())
                .andExpect(jsonPath("$.fieldErrors.courseworkId").exists())
                .andExpect(jsonPath("$.fieldErrors.enrollmentId").exists());

        verifyNoInteractions(taskService);
    }

    @Test
    void createReturnsNotFoundForMissingWorkPlan() throws Exception {
        when(taskService.create(any())).thenThrow(new NotFoundException("Work plan not found"));

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Work plan not found"));
    }

    @Test
    void createReturnsConflictWhenRuleFails() throws Exception {
        when(taskService.create(any())).thenThrow(
                new ConflictException("targetDate must fall within the work plan's range"));

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("targetDate must fall within the work plan's range"));
    }

    @Test
    void listPassesEveryFilter() throws Exception {
        when(taskService.list(30L, 10L, 20L)).thenReturn(List.of(RESPONSE));

        mockMvc.perform(get(URL).param("enrollmentId", "30").param("workPlanId", "10")
                .param("courseworkId", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));

        verify(taskService).list(30L, 10L, 20L);
    }

    @Test
    void listPassesNullForOmittedFilters() throws Exception {
        when(taskService.list(null, 10L, null)).thenReturn(List.of());

        mockMvc.perform(get(URL).param("workPlanId", "10")).andExpect(status().isOk());

        verify(taskService).list(null, 10L, null);
    }

    @Test
    void getReturnsNotFound() throws Exception {
        when(taskService.get(99L)).thenThrow(new NotFoundException("Task not found"));

        mockMvc.perform(get(URL + "/99")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Task not found"));
    }

    @Test
    void updateReturnsUpdatedTask() throws Exception {
        when(taskService.update(1L, REQUEST)).thenReturn(RESPONSE);

        mockMvc.perform(put(URL + "/1").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void deleteReturnsNoContent() throws Exception {
        mockMvc.perform(delete(URL + "/1")).andExpect(status().isNoContent());

        verify(taskService).delete(1L);
    }
}
