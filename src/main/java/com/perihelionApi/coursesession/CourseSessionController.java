package com.perihelionApi.coursesession;

import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

/**
 * REST endpoints for course meeting times and calendar export.
 */
@RestController
@RequestMapping("/api/courses/{courseId}")
public class CourseSessionController {

    /** Business logic for course meetings. */
    private final CourseSessionService sessionService;

    /**
     * Creates the controller.
     *
     * @param newSessionService course meeting service
     */
    public CourseSessionController(
            final CourseSessionService newSessionService) {
        this.sessionService = newSessionService;
    }

    /**
     * Creates a meeting time.
     *
     * @param courseId the course identifier
     * @param request  meeting time values
     * @return the created meeting time
     */
    @PostMapping("/sessions")
    public ResponseEntity<CourseSessionResponse> create(
            @PathVariable final Long courseId,
            @Valid @RequestBody final CourseSessionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(sessionService.create(courseId, request));
    }

    /**
     * Lists a course's meeting times.
     *
     * @param courseId the course identifier
     * @return ordered meeting times
     */
    @GetMapping("/sessions")
    public List<CourseSessionResponse> list(
            @PathVariable final Long courseId) {
        return sessionService.list(courseId);
    }

    /**
     * Updates a meeting time.
     *
     * @param courseId  the course identifier
     * @param sessionId the session identifier
     * @param request   replacement values
     * @return the updated meeting time
     */
    @PutMapping("/sessions/{sessionId}")
    public CourseSessionResponse update(@PathVariable final Long courseId,
            @PathVariable final Long sessionId,
            @Valid @RequestBody final CourseSessionRequest request) {
        return sessionService.update(courseId, sessionId, request);
    }

    /**
     * Deletes a meeting time.
     *
     * @param courseId  the course identifier
     * @param sessionId the session identifier
     * @return a 204 response
     */
    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<Void> delete(@PathVariable final Long courseId,
            @PathVariable final Long sessionId) {
        sessionService.delete(courseId, sessionId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Exports the course's meeting times to an iCalendar file.
     *
     * @param courseId the course identifier
     * @return a downloadable calendar file
     */
    @GetMapping(value = "/calendar.ics", produces = "text/calendar")
    public ResponseEntity<String> exportCalendar(
            @PathVariable final Long courseId) {
        String calendar = sessionService.exportCalendar(courseId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(
                        "text/calendar;charset=UTF-8"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"course-" + courseId
                                + ".ics\"")
                .body(calendar);
    }
}
