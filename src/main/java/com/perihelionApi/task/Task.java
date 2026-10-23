package com.perihelionApi.task;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;

/**
 * Represents a task associated with a work plan, coursework, and enrollment.
 */
@Entity
@Table(name = "Task")
public class Task {

    /** Unique identifier for the task. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** Identifier of the work plan associated with this task. */
    @Column(name = "workplanId")
    private Integer workplanId;

    /** Identifier of the coursework associated with this task. */
    @Column(name = "courseworkId")
    private Integer courseworkId;

    /** Identifier of the enrollment associated with this task. */
    @Column(name = "enrollmentId")
    private Integer enrollmentId;

    /** Title describing the task. */
    @Column(name = "title")
    private String title;

    /** Target completion date for the task. */
    @Column(name = "targetDate")
    private LocalDate targetDate;

    /** Estimated number of hours needed to complete the task. */
    @Column(name = "estimatedWorkHours")
    private Float estimatedWorkHours;

    /**
     * Creates an empty task for JPA.
     */
    protected Task() {
        // Required by JPA.
    }

    /**
     * Returns the task identifier.
     *
     * @return the task identifier
     */
    public Integer getId() {
        return id;
    }

    /**
     * Sets the task identifier.
     *
     * @param id the task identifier
     */
    public void setId(final Integer id) {
        this.id = id;
    }

    /**
     * Returns the associated work plan identifier.
     *
     * @return the work plan identifier
     */
    public Integer getWorkplanId() {
        return workplanId;
    }

    /**
     * Sets the associated work plan identifier.
     *
     * @param workplanId the work plan identifier
     */
    public void setWorkplanId(final Integer workplanId) {
        this.workplanId = workplanId;
    }

    /**
     * Returns the associated coursework identifier.
     *
     * @return the coursework identifier
     */
    public Integer getCourseworkId() {
        return courseworkId;
    }

    /**
     * Sets the associated coursework identifier.
     *
     * @param courseworkId the coursework identifier
     */
    public void setCourseworkId(final Integer courseworkId) {
        this.courseworkId = courseworkId;
    }

    /**
     * Returns the associated enrollment identifier.
     *
     * @return the enrollment identifier
     */
    public Integer getEnrollmentId() {
        return enrollmentId;
    }

    /**
     * Sets the associated enrollment identifier.
     *
     * @param enrollmentId the enrollment identifier
     */
    public void setEnrollmentId(final Integer enrollmentId) {
        this.enrollmentId = enrollmentId;
    }

    /**
     * Returns the task title.
     *
     * @return the task title
     */
    public String getTitle() {
        return title;
    }

    /**
     * Sets the task title.
     *
     * @param title the task title
     */
    public void setTitle(final String title) {
        this.title = title;
    }

    /**
     * Returns the target completion date.
     *
     * @return the target completion date
     */
    public LocalDate getTargetDate() {
        return targetDate;
    }

    /**
     * Sets the target completion date.
     *
     * @param targetDate the target completion date
     */
    public void setTargetDate(final LocalDate targetDate) {
        this.targetDate = targetDate;
    }

    /**
     * Returns the estimated work hours.
     *
     * @return the estimated work hours
     */
    public Float getEstimatedWorkHours() {
        return estimatedWorkHours;
    }

    /**
     * Sets the estimated work hours.
     *
     * @param estimatedWorkHours the estimated work hours
     */
    public void setEstimatedWorkHours(final Float estimatedWorkHours) {
        this.estimatedWorkHours = estimatedWorkHours;
    }
}