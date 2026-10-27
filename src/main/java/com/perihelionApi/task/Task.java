/*
    AI usage: Task CRUD: Grok 4.7, ~238K tokens
 */
package com.perihelionApi.task;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.perihelionApi.coursework.Coursework;
import com.perihelionApi.enrollment.Enrollment;
import com.perihelionApi.workplan.WorkPlan;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * A unit of planned work inside a work plan, aimed at one piece of
 * coursework.
 */
@Entity
@Table(name = "task",
        indexes = {
            @Index(name = "task_work_plan_idx", columnList = "work_plan_id"),
            @Index(name = "task_enrollment_idx",
                    columnList = "enrollment_id") })
public class Task {

    /** Surrogate primary key. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The work plan this task belongs to. */
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "work_plan_id", nullable = false)
    private WorkPlan workPlan;

    /** The coursework this task works toward. */
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "coursework_id", nullable = false)
    private Coursework coursework;

    /** The enrollment that owns this task. */
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "enrollment_id", nullable = false)
    private Enrollment enrollment;

    /** Short description of the task. */
    @Column(name = "title", nullable = false, length = 255)
    private String title;

    /** Day the task should be finished. */
    @Column(name = "target_date", nullable = false)
    private LocalDate targetDate;

    /** Estimated effort in hours. */
    @Column(name = "estimated_work_hours", nullable = false)
    private BigDecimal estimatedWorkHours;

    /**
     * Returns the primary key.
     *
     * @return the id, or null if not yet persisted
     */
    public Long getId() {
        return id;
    }

    /**
     * Sets the primary key.
     *
     * @param id the id
     */
    public void setId(final Long id) {
        this.id = id;
    }

    /**
     * Returns the owning work plan.
     *
     * @return the work plan
     */
    public WorkPlan getWorkPlan() {
        return workPlan;
    }

    /**
     * Sets the owning work plan.
     *
     * @param workPlan the work plan
     */
    public void setWorkPlan(final WorkPlan workPlan) {
        this.workPlan = workPlan;
    }

    /**
     * Returns the targeted coursework.
     *
     * @return the coursework
     */
    public Coursework getCoursework() {
        return coursework;
    }

    /**
     * Sets the targeted coursework.
     *
     * @param coursework the coursework
     */
    public void setCoursework(final Coursework coursework) {
        this.coursework = coursework;
    }

    /**
     * Returns the owning enrollment.
     *
     * @return the enrollment
     */
    public Enrollment getEnrollment() {
        return enrollment;
    }

    /**
     * Sets the owning enrollment.
     *
     * @param enrollment the enrollment
     */
    public void setEnrollment(final Enrollment enrollment) {
        this.enrollment = enrollment;
    }

    /**
     * Returns the title.
     *
     * @return the title
     */
    public String getTitle() {
        return title;
    }

    /**
     * Sets the title.
     *
     * @param title the title
     */
    public void setTitle(final String title) {
        this.title = title;
    }

    /**
     * Returns the target date.
     *
     * @return the target date
     */
    public LocalDate getTargetDate() {
        return targetDate;
    }

    /**
     * Sets the target date.
     *
     * @param targetDate the target date
     */
    public void setTargetDate(final LocalDate targetDate) {
        this.targetDate = targetDate;
    }

    /**
     * Returns the estimated effort in hours.
     *
     * @return the estimated work hours
     */
    public BigDecimal getEstimatedWorkHours() {
        return estimatedWorkHours;
    }

    /**
     * Sets the estimated effort in hours.
     *
     * @param estimatedWorkHours the estimated work hours
     */
    public void setEstimatedWorkHours(final BigDecimal estimatedWorkHours) {
        this.estimatedWorkHours = estimatedWorkHours;
    }

}
