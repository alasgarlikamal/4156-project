
/*
    AI usage: Task CRUD implementation: ChatGPT GPT-6
 */
package com.perihelionApi.task;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(name = "Task")
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "workplanId", nullable = false)
    private Integer workplanId;

    @Column(name = "courseworkId", nullable = false)
    private Integer courseworkId;

    @Column(name = "enrollmentId", nullable = false)
    private Integer enrollmentId;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(name = "targetDate", nullable = false)
    private LocalDate targetDate;

    @Column(name = "estimatedWorkHours", nullable = false)
    private Float estimatedWorkHours;

    public Task() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getWorkplanId() {
        return workplanId;
    }

    public void setWorkplanId(Integer workplanId) {
        this.workplanId = workplanId;
    }

    public Integer getCourseworkId() {
        return courseworkId;
    }

    public void setCourseworkId(Integer courseworkId) {
        this.courseworkId = courseworkId;
    }

    public Integer getEnrollmentId() {
        return enrollmentId;
    }

    public void setEnrollmentId(Integer enrollmentId) {
        this.enrollmentId = enrollmentId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public LocalDate getTargetDate() {
        return targetDate;
    }

    public void setTargetDate(LocalDate targetDate) {
        this.targetDate = targetDate;
    }

    public Float getEstimatedWorkHours() {
        return estimatedWorkHours;
    }

    public void setEstimatedWorkHours(Float estimatedWorkHours) {
        this.estimatedWorkHours = estimatedWorkHours;
    }
}
