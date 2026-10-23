
/*
    AI usage: Task service tests: ChatGPT GPT-6
 */
package com.perihelionApi.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @InjectMocks
    private TaskService taskService;

    private Task task;

    @BeforeEach
    void setUp() {
        task = new Task();
        task.setId(1);
        task.setWorkplanId(2);
        task.setCourseworkId(3);
        task.setEnrollmentId(4);
        task.setTitle("Study for exam");
        task.setTargetDate(LocalDate.of(2026, 11, 1));
        task.setEstimatedWorkHours(2.5f);
    }

    @Test
    void createTaskSavesTask() {
        when(taskRepository.save(task)).thenReturn(task);

        Task result = taskService.createTask(task);

        assertEquals("Study for exam", result.getTitle());
        verify(taskRepository).save(task);
    }

    @Test
    void getAllTasksReturnsTasks() {
        when(taskRepository.findAll()).thenReturn(List.of(task));

        List<Task> result = taskService.getAllTasks();

        assertEquals(1, result.size());
        assertEquals(1, result.get(0).getId());
    }

    @Test
    void getTaskByIdReturnsTask() {
        when(taskRepository.findById(1)).thenReturn(Optional.of(task));

        Task result = taskService.getTaskById(1);

        assertEquals("Study for exam", result.getTitle());
    }

    @Test
    void getTaskByIdThrowsWhenMissing() {
        when(taskRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(
                ResponseStatusException.class,
                () -> taskService.getTaskById(99));
    }

    @Test
    void getTasksByEnrollmentReturnsMatchingTasks() {
        when(taskRepository.findByEnrollmentId(4))
                .thenReturn(List.of(task));

        List<Task> result = taskService.getTasksByEnrollment(4);

        assertEquals(1, result.size());
        verify(taskRepository).findByEnrollmentId(4);
    }

    @Test
    void getTasksByWorkplanReturnsMatchingTasks() {
        when(taskRepository.findByWorkplanId(2))
                .thenReturn(List.of(task));

        List<Task> result = taskService.getTasksByWorkplan(2);

        assertEquals(1, result.size());
        verify(taskRepository).findByWorkplanId(2);
    }

    @Test
    void getTasksByCourseworkReturnsMatchingTasks() {
        when(taskRepository.findByCourseworkId(3))
                .thenReturn(List.of(task));

        List<Task> result = taskService.getTasksByCoursework(3);

        assertEquals(1, result.size());
        verify(taskRepository).findByCourseworkId(3);
    }

    @Test
    void updateTaskChangesFields() {
        Task updated = new Task();
        updated.setWorkplanId(5);
        updated.setCourseworkId(6);
        updated.setEnrollmentId(7);
        updated.setTitle("Finish assignment");
        updated.setTargetDate(LocalDate.of(2026, 11, 5));
        updated.setEstimatedWorkHours(4.0f);

        when(taskRepository.findById(1)).thenReturn(Optional.of(task));
        when(taskRepository.save(task)).thenReturn(task);

        Task result = taskService.updateTask(1, updated);

        assertEquals("Finish assignment", result.getTitle());
        assertEquals(5, result.getWorkplanId());
        assertEquals(6, result.getCourseworkId());
        assertEquals(7, result.getEnrollmentId());
        assertEquals(4.0f, result.getEstimatedWorkHours());
        verify(taskRepository).save(task);
    }

    @Test
    void deleteTaskDeletesExistingTask() {
        when(taskRepository.findById(1)).thenReturn(Optional.of(task));

        taskService.deleteTask(1);

        verify(taskRepository).delete(task);
    }

    @Test
    void deleteMissingTaskDoesNotDeleteAnything() {
        when(taskRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(
                ResponseStatusException.class,
                () -> taskService.deleteTask(99));

        verify(taskRepository, never()).delete(task);
    }
}
