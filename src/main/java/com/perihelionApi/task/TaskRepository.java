
/*
    AI usage: Task CRUD implementation: ChatGPT GPT-6
 */
package com.perihelionApi.task;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskRepository extends JpaRepository<Task, Integer> {

    List<Task> findByEnrollmentId(Integer enrollmentId);

    List<Task> findByWorkplanId(Integer workplanId);

    List<Task> findByCourseworkId(Integer courseworkId);
}
