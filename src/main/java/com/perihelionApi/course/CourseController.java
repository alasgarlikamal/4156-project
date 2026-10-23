/*
    AI usage: Course and Enrollment CRUD: Grok 4.7, ~111K tokens
 */
package com.perihelionApi.course;

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
 * REST endpoints for creating, reading, updating, and deleting courses.
 */
@RestController
@RequestMapping("/api/courses")
public class CourseController {

    /** Business logic for courses. */
    private final CourseService courseService;

    /**
     * Creates the controller.
     *
     * @param courseService the course service
     */
    public CourseController(final CourseService courseService) {
        this.courseService = courseService;
    }

    /**
     * Creates a course.
     *
     * @param request the course to create
     * @return a 201 response with the created course
     */
    @PostMapping
    public ResponseEntity<CourseResponse> create(
            @Valid @RequestBody final CourseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(courseService.create(request));
    }

    /**
     * Lists courses, optionally filtered by title substring and code.
     *
     * @param title a case-insensitive title fragment, or null
     * @param code  an exact course code, or null
     * @return the matching courses ordered by title
     */
    @GetMapping
    public List<CourseResponse> list(
            @RequestParam(required = false) final String title,
            @RequestParam(required = false) final String code) {
        return courseService.list(title, code);
    }

    /**
     * Returns a single course.
     *
     * @param id the course id
     * @return the course
     */
    @GetMapping("/{id}")
    public CourseResponse get(@PathVariable final Long id) {
        return courseService.get(id);
    }

    /**
     * Replaces a course.
     *
     * @param id      the course id
     * @param request the new course values
     * @return the updated course
     */
    @PutMapping("/{id}")
    public CourseResponse update(@PathVariable final Long id,
            @Valid @RequestBody final CourseRequest request) {
        return courseService.update(id, request);
    }

    /**
     * Deletes a course.
     *
     * @param id the course id
     * @return a 204 response
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable final Long id) {
        courseService.delete(id);
        return ResponseEntity.noContent().build();
    }

}
