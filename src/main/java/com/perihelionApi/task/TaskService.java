
/*
    AI usage: Task CRUD implementation: ChatGPT GPT-6
 */
package com.perihelionApi.task;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TaskService {

    private final TaskRepository taskRepository;

    public TaskService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    public Task createTask(Task task) {
        task.setId(null);
        return taskRepository.save(task);
    }

    public List<Task> getAllTasks() {
        return taskRepository.findAll();
    }

    public Task getTaskById(Integer id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Task not found"));
    }

    public List<Task> getTasksByEnrollment(Integer enrollmentId) {
        return taskRepository.findByEnrollmentId(enrollmentId);
    }

    public List<Task> getTasksByWorkplan(Integer workplanId) {
        return taskRepository.findByWorkplanId(workplanId);
    }

    public List<Task> getTasksByCoursework(Integer courseworkId) {
        return taskRepository.findByCourseworkId(courseworkId);
    }

    public Task updateTask(Integer id, Task updatedTask) {
        Task task = getTaskById(id);

        task.setWorkplanId(updatedTask.getWorkplanId());
        task.setCourseworkId(updatedTask.getCourseworkId());
        task.setEnrollmentId(updatedTask.getEnrollmentId());
        task.setTitle(updatedTask.getTitle());
        task.setTargetDate(updatedTask.getTargetDate());
        task.setEstimatedWorkHours(
                updatedTask.getEstimatedWorkHours());

        return taskRepository.save(task);
    }

    public void deleteTask(Integer id) {
        Task task = getTaskById(id);
        taskRepository.delete(task);
    }
}
