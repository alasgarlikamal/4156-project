package com.perihelionApi.assignment;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.perihelionApi.course.Course;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * An assignment belonging to a course.
 */
@Entity
@Table(name = "assignment")
public class Assignment {

    /** Surrogate primary key. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The course that owns the assignment. */
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    /** Human-readable assignment title. */
    @Column(nullable = false, length = 255)
    private String title;

    /** Assignment deadline. */
    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    /** Estimated effort in hours. */
    @Column(name = "estimated_work_hours", nullable = false)
    private BigDecimal estimatedWorkHours;

    /**
     * Returns the id.
     *
     * @return the id
     */
    public Long getId() {
        return id;
    }

    /**
     * Sets the id.
     *
     * @param id the id
     */
    public void setId(final Long id) {
        this.id = id;
    }

    /**
     * Returns the course.
     *
     * @return the course
     */
    public Course getCourse() {
        return course;
    }

    /**
     * Sets the course.
     *
     * @param course the course
     */
    public void setCourse(final Course course) {
        this.course = course;
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
     * Returns the dueDate.
     *
     * @return the dueDate
     */
    public LocalDate getDueDate() {
        return dueDate;
    }

    /**
     * Sets the dueDate.
     *
     * @param dueDate the dueDate
     */
    public void setDueDate(final LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    /**
     * Returns the estimatedWorkHours.
     *
     * @return the estimatedWorkHours
     */
    public BigDecimal getEstimatedWorkHours() {
        return estimatedWorkHours;
    }

    /**
     * Sets the estimatedWorkHours.
     *
     * @param estimatedWorkHours the estimatedWorkHours
     */
    public void setEstimatedWorkHours(final BigDecimal estimatedWorkHours) {
        this.estimatedWorkHours = estimatedWorkHours;
    }

}
