/*
    AI usage: Coursework CRUD: Grok 4.7, ~238K tokens
 */
package com.perihelionApi.coursework;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

/**
 * REST endpoints for creating, reading, updating, and deleting coursework.
 */
@RestController
@RequestMapping("/api/coursework")
public class CourseworkController {

    /** Business logic for coursework. */
    private final CourseworkService courseworkService;

    /**
     * Creates the controller.
     *
     * @param courseworkService the coursework service
     */
    public CourseworkController(final CourseworkService courseworkService) {
        this.courseworkService = courseworkService;
    }

    /**
     * Creates coursework.
     *
     * @param request the coursework to create
     * @return a 201 response with the created coursework
     */
    @PostMapping
    public ResponseEntity<CourseworkResponse> create(
            @Valid @RequestBody final CourseworkRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(courseworkService.create(request));
    }

    /**
     * Lists coursework, optionally filtered by course and kind.
     *
     * @param courseId a course id, or null
     * @param kind     EXAM or ASSIGNMENT, or null
     * @return the matching coursework ordered by deadline and id
     */
    @GetMapping
    public List<CourseworkResponse> list(
            @RequestParam(required = false) final Long courseId,
            @RequestParam(required = false) final CourseworkKind kind) {
        return courseworkService.list(courseId, kind);
    }

    /**
     * Returns a single piece of coursework.
     *
     * @param id the coursework id
     * @return the coursework
     */
    @GetMapping("/{id}")
    public CourseworkResponse get(@PathVariable final Long id) {
        return courseworkService.get(id);
    }

    /**
     * Replaces coursework.
     *
     * @param id      the coursework id
     * @param request the new values
     * @return the updated coursework
     */
    @PutMapping("/{id}")
    public CourseworkResponse update(@PathVariable final Long id,
            @Valid @RequestBody final CourseworkRequest request) {
        return courseworkService.update(id, request);
    }

    /**
     * Deletes coursework.
     *
     * @param id the coursework id
     * @return a 204 response
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable final Long id) {
        courseworkService.delete(id);
        return ResponseEntity.noContent().build();
    }

}
