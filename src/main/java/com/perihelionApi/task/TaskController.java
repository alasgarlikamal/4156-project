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

/**
 * Handles HTTP requests for task operations.
 */
@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    /** Service used to manage tasks. */
    private final TaskService taskService;

    /**
     * Creates a task controller.
     *
     * @param service service used to manage tasks
     */
    public TaskController(final TaskService service) {
        this.taskService = service;
    }

    /**
     * Creates a task.
     *
     * @param task the task to create
     * @return the created task with HTTP status 201
     */
    @PostMapping
    public ResponseEntity<Task> createTask(@RequestBody final Task task) {
        Task createdTask = taskService.createTask(task);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdTask);
    }

    /**
     * Retrieves tasks, optionally filtered by related records.
     *
     * @param enrollmentId optional enrollment identifier
     * @param workplanId optional work plan identifier
     * @param courseworkId optional coursework identifier
     * @return the matching tasks
     */
    @GetMapping
    public ResponseEntity<List<Task>> getTasks(
            @RequestParam(required = false) final Integer enrollmentId,
            @RequestParam(required = false) final Integer workplanId,
            @RequestParam(required = false) final Integer courseworkId) {

        List<Task> tasks;

        if (enrollmentId != null) {
            tasks = taskService.getTasksByEnrollment(enrollmentId);
        } else if (workplanId != null) {
            tasks = taskService.getTasksByWorkplan(workplanId);
        } else if (courseworkId != null) {
            tasks = taskService.getTasksByCoursework(courseworkId);
        } else {
            tasks = taskService.getAllTasks();
        }

        return ResponseEntity.ok(tasks);
    }

    /**
     * Retrieves a task by its identifier.
     *
     * @param id the task identifier
     * @return the requested task
     */
    @GetMapping("/{id}")
    public ResponseEntity<Task> getTaskById(
            @PathVariable("id") final Integer id) {
        return ResponseEntity.ok(taskService.getTaskById(id));
    }

    /**
     * Updates an existing task.
     *
     * @param id the task identifier
     * @param task the updated task values
     * @return the updated task
     */
    @PutMapping("/{id}")
    public ResponseEntity<Task> updateTask(
            @PathVariable("id") final Integer id,
            @RequestBody final Task task) {
        return ResponseEntity.ok(taskService.updateTask(id, task));
    }

    /**
     * Deletes a task by its identifier.
     *
     * @param id the task identifier
     * @return an empty response with HTTP status 204
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(
            @PathVariable("id") final Integer id) {
        taskService.deleteTask(id);
        return ResponseEntity.noContent().build();
    }
}
