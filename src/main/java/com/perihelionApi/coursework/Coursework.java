/*
    AI usage: Coursework CRUD: Grok 4.7, ~238K tokens
 */
package com.perihelionApi.coursework;

import java.math.BigDecimal;
import java.time.Instant;

import com.perihelionApi.course.Course;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * A graded piece of work belonging to a course, such as an exam or an
 * assignment.
 */
@Entity
@Table(name = "coursework",
        indexes = @Index(name = "coursework_course_deadline_idx",
                columnList = "course_id, deadline"))
public class Coursework {

    /** Surrogate primary key. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The course that owns the coursework. */
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    /** Whether this is an exam or an assignment. */
    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false, length = 32)
    private CourseworkKind kind;

    /** Human-readable title. */
    @Column(name = "title", nullable = false, length = 255)
    private String title;

    /** When the work is due or the exam takes place. */
    @Column(name = "deadline", nullable = false)
    private Instant deadline;

    /** Estimated effort in hours. */
    @Column(name = "estimated_effort_hours", nullable = false)
    private BigDecimal estimatedEffortHours;

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
     * Returns the owning course.
     *
     * @return the course
     */
    public Course getCourse() {
        return course;
    }

    /**
     * Sets the owning course.
     *
     * @param course the course
     */
    public void setCourse(final Course course) {
        this.course = course;
    }

    /**
     * Returns the kind.
     *
     * @return the kind
     */
    public CourseworkKind getKind() {
        return kind;
    }

    /**
     * Sets the kind.
     *
     * @param kind the kind
     */
    public void setKind(final CourseworkKind kind) {
        this.kind = kind;
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
     * Returns the deadline.
     *
     * @return the deadline
     */
    public Instant getDeadline() {
        return deadline;
    }

    /**
     * Sets the deadline.
     *
     * @param deadline the deadline
     */
    public void setDeadline(final Instant deadline) {
        this.deadline = deadline;
    }

    /**
     * Returns the estimated effort in hours.
     *
     * @return the estimated effort hours
     */
    public BigDecimal getEstimatedEffortHours() {
        return estimatedEffortHours;
    }

    /**
     * Sets the estimated effort in hours.
     *
     * @param estimatedEffortHours the estimated effort hours
     */
    public void setEstimatedEffortHours(
            final BigDecimal estimatedEffortHours) {
        this.estimatedEffortHours = estimatedEffortHours;
    }

}
