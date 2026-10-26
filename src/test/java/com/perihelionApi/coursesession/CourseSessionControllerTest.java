package com.perihelionApi.coursesession;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.perihelionApi.common.GlobalExceptionHandler;

@WebMvcTest(CourseSessionController.class)
@Import(GlobalExceptionHandler.class)
class CourseSessionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CourseSessionService sessionService;

    @Test
    void createReturnsCreatedSession() throws Exception {
        when(sessionService.create(any(), any(CourseSessionRequest.class)))
                .thenReturn(new CourseSessionResponse(3L, 1L, 1,
                        LocalTime.of(14, 0), LocalTime.of(15, 15)));

        mockMvc.perform(post("/api/courses/1/sessions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"weekday":1,"startTime":"14:00","endTime":"15:15"}
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.weekday").value(1));
    }

    @Test
    void createRejectsInvalidWeekday() throws Exception {
        mockMvc.perform(post("/api/courses/1/sessions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"weekday":8,"startTime":"14:00","endTime":"15:15"}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.weekday").exists());

        verify(sessionService, never()).create(any(), any());
    }

    @Test
    void createRejectsEndTimeBeforeStartTime() throws Exception {
        mockMvc.perform(post("/api/courses/1/sessions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"weekday":1,"startTime":"15:15","endTime":"14:00"}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(
                        org.hamcrest.Matchers.containsString(
                                "endTime must be later than startTime")));

        verify(sessionService, never()).create(any(), any());
    }

    @Test
    void createRejectsMissingStartOrEndTime() throws Exception {
        mockMvc.perform(post("/api/courses/1/sessions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"weekday":1,"endTime":"15:15"}
                        """))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/courses/1/sessions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"weekday":1,"startTime":"14:00"}
                        """))
                .andExpect(status().isBadRequest());

        verify(sessionService, never()).create(any(), any());
    }

    @Test
    void listReturnsCourseSessions() throws Exception {
        when(sessionService.list(1L)).thenReturn(List.of(
                new CourseSessionResponse(3L, 1L, 1,
                        LocalTime.of(14, 0), LocalTime.of(15, 15))));

        mockMvc.perform(get("/api/courses/1/sessions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].weekday").value(1));
    }

    @Test
    void deleteReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/courses/1/sessions/3"))
                .andExpect(status().isNoContent());

        verify(sessionService).delete(1L, 3L);
    }

    @Test
    void exportReturnsDownloadableCalendar() throws Exception {
        when(sessionService.exportCalendar(1L)).thenReturn(
                "BEGIN:VCALENDAR\r\nEND:VCALENDAR\r\n");

        mockMvc.perform(get("/api/courses/1/calendar.ics"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.parseMediaType(
                                "text/calendar;charset=UTF-8")))
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename=\"course-1.ics\""))
                .andExpect(content().string(
                        "BEGIN:VCALENDAR\r\nEND:VCALENDAR\r\n"));
    }
}
