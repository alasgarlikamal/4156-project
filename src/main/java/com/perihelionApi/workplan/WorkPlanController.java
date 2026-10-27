/*
    AI usage: Work plan CRUD: Grok 4.7, ~238K tokens
 */
package com.perihelionApi.workplan;

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
 * REST endpoints for creating, reading, updating, and deleting work plans.
 */
@RestController
@RequestMapping("/api/workplans")
public class WorkPlanController {

    /** Business logic for work plans. */
    private final WorkPlanService workPlanService;

    /**
     * Creates the controller.
     *
     * @param workPlanService the work plan service
     */
    public WorkPlanController(final WorkPlanService workPlanService) {
        this.workPlanService = workPlanService;
    }

    /**
     * Creates a work plan.
     *
     * @param request the work plan to create
     * @return a 201 response with the created work plan
     */
    @PostMapping
    public ResponseEntity<WorkPlanResponse> create(
            @Valid @RequestBody final WorkPlanRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(workPlanService.create(request));
    }

    /**
     * Lists work plans, optionally filtered by enrollment.
     *
     * @param enrollmentId an enrollment id, or null
     * @return the matching work plans ordered by range start and id
     */
    @GetMapping
    public List<WorkPlanResponse> list(
            @RequestParam(required = false) final Long enrollmentId) {
        return workPlanService.list(enrollmentId);
    }

    /**
     * Returns a single work plan.
     *
     * @param id the work plan id
     * @return the work plan
     */
    @GetMapping("/{id}")
    public WorkPlanResponse get(@PathVariable final Long id) {
        return workPlanService.get(id);
    }

    /**
     * Replaces a work plan.
     *
     * @param id      the work plan id
     * @param request the new work plan values
     * @return the updated work plan
     */
    @PutMapping("/{id}")
    public WorkPlanResponse update(@PathVariable final Long id,
            @Valid @RequestBody final WorkPlanRequest request) {
        return workPlanService.update(id, request);
    }

    /**
     * Deletes a work plan.
     *
     * @param id the work plan id
     * @return a 204 response
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable final Long id) {
        workPlanService.delete(id);
        return ResponseEntity.noContent().build();
    }

}
