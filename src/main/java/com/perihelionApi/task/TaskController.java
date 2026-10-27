/*
    AI usage: Task CRUD: Grok 4.7, ~238K tokens
 */
package com.perihelionApi.task;

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
 * REST endpoints for creating, reading, updating, and deleting tasks.
 */
@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    /** Business logic for tasks. */
    private final TaskService taskService;

    /**
     * Creates the controller.
     *
     * @param taskService the task service
     */
    public TaskController(final TaskService taskService) {
        this.taskService = taskService;
    }

    /**
     * Creates a task.
     *
     * @param request the task to create
     * @return a 201 response with the created task
     */
    @PostMapping
    public ResponseEntity<TaskResponse> create(
            @Valid @RequestBody final TaskRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(taskService.create(request));
    }

    /**
     * Lists tasks matching every supplied filter.
     *
     * @param enrollmentId an enrollment id, or null
     * @param workPlanId   a work plan id, or null
     * @param courseworkId a coursework id, or null
     * @return the matching tasks ordered by target date and id
     */
    @GetMapping
    public List<TaskResponse> list(
            @RequestParam(required = false) final Long enrollmentId,
            @RequestParam(required = false) final Long workPlanId,
            @RequestParam(required = false) final Long courseworkId) {
        return taskService.list(enrollmentId, workPlanId, courseworkId);
    }

    /**
     * Returns a single task.
     *
     * @param id the task id
     * @return the task
     */
    @GetMapping("/{id}")
    public TaskResponse get(@PathVariable final Long id) {
        return taskService.get(id);
    }

    /**
     * Replaces a task.
     *
     * @param id      the task id
     * @param request the new task values
     * @return the updated task
     */
    @PutMapping("/{id}")
    public TaskResponse update(@PathVariable final Long id,
            @Valid @RequestBody final TaskRequest request) {
        return taskService.update(id, request);
    }

    /**
     * Deletes a task.
     *
     * @param id the task id
     * @return a 204 response
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable final Long id) {
        taskService.delete(id);
        return ResponseEntity.noContent().build();
    }

}
