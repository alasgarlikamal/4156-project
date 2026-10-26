package com.perihelionApi.submission;

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
 * REST endpoints for creating, reading, updating, and deleting submissions.
 */
@RestController
@RequestMapping("/api/submissions")
public class SubmissionController {

    /** Logic for submissions. */
    private final SubmissionService submissionService;

    /**
     * Creates the controller.
     *
     * @param submissionService the submission service
     */
    public SubmissionController(final SubmissionService submissionService) {
        this.submissionService = submissionService;
    }

    /**
     * Creates a submission.
     *
     * @param request the submission to create
     * @return a 201 response with the created submission
     */
    @PostMapping
    public ResponseEntity<SubmissionResponse> create(
            @Valid @RequestBody final SubmissionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(submissionService.create(request));
    }

    /**
     * Lists submissions, optionally filtered by enrollment and coursework.
     *
     * @param enrollmentId an enrollment id, or null
     * @param courseworkId a coursework id, or null
     * @return the matching submissions ordered by id
     */
    @GetMapping
    public List<SubmissionResponse> list(
            @RequestParam(required = false) final Long enrollmentId,
            @RequestParam(required = false) final Long courseworkId) {
        return submissionService.list(enrollmentId, courseworkId);
    }

    /**
     * Returns a single submission.
     *
     * @param id the submission id
     * @return the submission
     */
    @GetMapping("/{id}")
    public SubmissionResponse get(@PathVariable final Long id) {
        return submissionService.get(id);
    }

    /**
     * Replaces a submission.
     *
     * @param id      the submission id
     * @param request the new submission values
     * @return the updated submission
     */
    @PutMapping("/{id}")
    public SubmissionResponse update(@PathVariable final Long id,
            @Valid @RequestBody final SubmissionRequest request) {
        return submissionService.update(id, request);
    }

    /**
     * Deletes a submission.
     *
     * @param id the submission id
     * @return a 204 response
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable final Long id) {
        submissionService.delete(id);
        return ResponseEntity.noContent().build();
    }

}
