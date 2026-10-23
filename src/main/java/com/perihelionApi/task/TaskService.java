package com.perihelionApi.task;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Provides business logic for managing tasks.
 */
@Service
public class TaskService {

    /** Repository used to access task records. */
    private final TaskRepository taskRepository;

    /**
     * Creates a task service.
     *
     * @param taskRepository repository used to access task records
     */
    public TaskService(final TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    /**
     * Creates and saves a task.
     *
     * @param task the task to create
     * @return the saved task
     */
    public Task createTask(final Task task) {
        return taskRepository.save(task);
    }

    /**
     * Retrieves all tasks.
     *
     * @return all stored tasks
     */
    public List<Task> getAllTasks() {
        return taskRepository.findAll();
    }

    /**
     * Retrieves a task by its identifier.
     *
     * @param id the task identifier
     * @return the matching task
     * @throws ResponseStatusException if the task does not exist
     */
    public Task getTaskById(final Integer id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Task not found"));
    }

    /**
     * Retrieves tasks associated with an enrollment.
     *
     * @param enrollmentId the enrollment identifier
     * @return tasks belonging to the enrollment
     */
    public List<Task> getTasksByEnrollment(final Integer enrollmentId) {
        return taskRepository.findByEnrollmentId(enrollmentId);
    }

    /**
     * Retrieves tasks associated with a work plan.
     *
     * @param workplanId the work plan identifier
     * @return tasks belonging to the work plan
     */
    public List<Task> getTasksByWorkplan(final Integer workplanId) {
        return taskRepository.findByWorkplanId(workplanId);
    }

    /**
     * Retrieves tasks associated with coursework.
     *
     * @param courseworkId the coursework identifier
     * @return tasks belonging to the coursework
     */
    public List<Task> getTasksByCoursework(final Integer courseworkId) {
        return taskRepository.findByCourseworkId(courseworkId);
    }

    /**
     * Updates an existing task.
     *
     * @param id the identifier of the task to update
     * @param updatedTask the task containing the updated values
     * @return the updated task
     * @throws ResponseStatusException if the task does not exist
     */
    public Task updateTask(final Integer id, final Task updatedTask) {
        Task existingTask = getTaskById(id);

        existingTask.setWorkplanId(updatedTask.getWorkplanId());
        existingTask.setCourseworkId(updatedTask.getCourseworkId());
        existingTask.setEnrollmentId(updatedTask.getEnrollmentId());
        existingTask.setTitle(updatedTask.getTitle());
        existingTask.setTargetDate(updatedTask.getTargetDate());
        existingTask.setEstimatedWorkHours(
                updatedTask.getEstimatedWorkHours());

        return taskRepository.save(existingTask);
    }

    /**
     * Deletes a task by its identifier.
     *
     * @param id the identifier of the task to delete
     * @throws ResponseStatusException if the task does not exist
     */
    public void deleteTask(final Integer id) {
        Task task = getTaskById(id);
        taskRepository.delete(task);
    }
}