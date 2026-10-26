package com.perihelionApi.assignment;

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
 * REST endpoints for creating, reading, updating, and deleting assignments.
 */
@RestController
@RequestMapping("/api/assignments")
public class AssignmentController {

    /** Business logic for assignments. */
    private final AssignmentService assignmentService;

    /**
     * Creates the controller.
     *
     * @param assignmentService the assignment service
     */
    public AssignmentController(final AssignmentService assignmentService) {
        this.assignmentService = assignmentService;
    }

    /**
     * Creates an assignment.
     *
     * @param request the assignment to create
     * @return a 201 response with the created assignment
     */
    @PostMapping
    public ResponseEntity<AssignmentResponse> create(
            @Valid @RequestBody final AssignmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(assignmentService.create(request));
    }

    /**
     * Lists assignments, optionally filtered by course.
     *
     * @param courseId a course id, or null
     * @return the matching assignments ordered by due date and id
     */
    @GetMapping
    public List<AssignmentResponse> list(
            @RequestParam(required = false) final Long courseId) {
        return assignmentService.list(courseId);
    }

    /**
     * Returns a single assignment.
     *
     * @param id the assignment id
     * @return the assignment
     */
    @GetMapping("/{id}")
    public AssignmentResponse get(@PathVariable final Long id) {
        return assignmentService.get(id);
    }

    /**
     * Replaces an assignment.
     *
     * @param id      the assignment id
     * @param request the new assignment values
     * @return the updated assignment
     */
    @PutMapping("/{id}")
    public AssignmentResponse update(@PathVariable final Long id,
            @Valid @RequestBody final AssignmentRequest request) {
        return assignmentService.update(id, request);
    }

    /**
     * Deletes an assignment.
     *
     * @param id the assignment id
     * @return a 204 response
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable final Long id) {
        assignmentService.delete(id);
        return ResponseEntity.noContent().build();
    }

}
