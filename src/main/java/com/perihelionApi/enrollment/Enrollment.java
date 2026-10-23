/*
    AI usage: Course and Enrollment CRUD: Grok 4.7, ~111K tokens
 */
package com.perihelionApi.enrollment;

import com.perihelionApi.course.Course;

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
import jakarta.persistence.UniqueConstraint;

/**
 * A learner's enrollment in a course.
 */
@Entity
@Table(name = "enrollment",
        uniqueConstraints = @UniqueConstraint(
                name = "enrollment_course_learner_uk",
                columnNames = { "course_id", "learner_ref" }),
        indexes = @Index(name = "enrollment_course_idx",
                columnList = "course_id"))
public class Enrollment {

    /** Surrogate primary key. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The course the learner is enrolled in. */
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    /** Opaque identifier of the learner, stored as a UUID string. */
    @Column(name = "learner_ref", nullable = false, length = 255)
    private String learnerRef;

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
     * Returns the enrolled course.
     *
     * @return the course
     */
    public Course getCourse() {
        return course;
    }

    /**
     * Sets the enrolled course.
     *
     * @param course the course
     */
    public void setCourse(final Course course) {
        this.course = course;
    }

    /**
     * Returns the learner identifier.
     *
     * @return the learner identifier as a UUID string
     */
    public String getLearnerRef() {
        return learnerRef;
    }

    /**
     * Sets the learner identifier.
     *
     * @param learnerRef the learner identifier as a UUID string
     */
    public void setLearnerRef(final String learnerRef) {
        this.learnerRef = learnerRef;
    }

}
