/*
    AI usage: Course and Enrollment CRUD: Grok 4.7, ~111K tokens
 */
package com.perihelionApi.enrollment;

import java.util.List;
import java.util.UUID;

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
 * REST endpoints for creating, reading, updating, and deleting enrollments.
 */
@RestController
@RequestMapping("/api/enrollments")
public class EnrollmentController {

    /** Business logic for enrollments. */
    private final EnrollmentService enrollmentService;

    /**
     * Creates the controller.
     *
     * @param enrollmentService the enrollment service
     */
    public EnrollmentController(final EnrollmentService enrollmentService) {
        this.enrollmentService = enrollmentService;
    }

    /**
     * Creates an enrollment.
     *
     * @param request the enrollment to create
     * @return a 201 response with the created enrollment
     */
    @PostMapping
    public ResponseEntity<EnrollmentResponse> create(
            @Valid @RequestBody final EnrollmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(enrollmentService.create(request));
    }

    /**
     * Lists enrollments, optionally filtered by course and learner.
     *
     * @param courseId   a course id, or null
     * @param learnerRef a learner identifier, or null
     * @return the matching enrollments ordered by id
     */
    @GetMapping
    public List<EnrollmentResponse> list(
            @RequestParam(required = false) final Long courseId,
            @RequestParam(required = false) final UUID learnerRef) {
        return enrollmentService.list(courseId, learnerRef);
    }

    /**
     * Returns a single enrollment.
     *
     * @param id the enrollment id
     * @return the enrollment
     */
    @GetMapping("/{id}")
    public EnrollmentResponse get(@PathVariable final Long id) {
        return enrollmentService.get(id);
    }

    /**
     * Replaces an enrollment.
     *
     * @param id      the enrollment id
     * @param request the new enrollment values
     * @return the updated enrollment
     */
    @PutMapping("/{id}")
    public EnrollmentResponse update(@PathVariable final Long id,
            @Valid @RequestBody final EnrollmentRequest request) {
        return enrollmentService.update(id, request);
    }

    /**
     * Deletes an enrollment.
     *
     * @param id the enrollment id
     * @return a 204 response
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable final Long id) {
        enrollmentService.delete(id);
        return ResponseEntity.noContent().build();
    }

}
