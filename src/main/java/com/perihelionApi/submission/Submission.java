package com.perihelionApi.submission;

import java.time.Instant;

import com.perihelionApi.enrollment.Enrollment;

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
 * Submission of a piece of coursework.
 */
@Entity
@Table(name = "submission",
        uniqueConstraints = @UniqueConstraint(
                name = "submission_enrollment_coursework_uk",
                columnNames = { "enrollment_id", "coursework_id" }),
        indexes = @Index(name = "submission_enrollment_idx",
                columnList = "enrollment_id"))
public class Submission {

    /** Primary key. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The enrollment of the learner who submitted. */
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "enrollment_id", nullable = false)
    private Enrollment enrollment;

    /**
     * Id of the submitted coursework.
     */
    @Column(name = "coursework_id", nullable = false)
    private Long courseworkId;

    /** When the work was submitted. */
    @Column(name = "submitted_at", nullable = false)
    private Instant submittedAt;

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
     * Returns the submitting enrollment.
     *
     * @return the enrollment
     */
    public Enrollment getEnrollment() {
        return enrollment;
    }

    /**
     * Sets the submitting enrollment.
     *
     * @param enrollment the enrollment
     */
    public void setEnrollment(final Enrollment enrollment) {
        this.enrollment = enrollment;
    }

    /**
     * Returns the submitted coursework id.
     *
     * @return the coursework id
     */
    public Long getCourseworkId() {
        return courseworkId;
    }

    /**
     * Sets the submitted coursework id.
     *
     * @param courseworkId the coursework id
     */
    public void setCourseworkId(final Long courseworkId) {
        this.courseworkId = courseworkId;
    }

    /**
     * Returns the submission time.
     *
     * @return the submission time
     */
    public Instant getSubmittedAt() {
        return submittedAt;
    }

    /**
     * Sets the submission time.
     *
     * @param submittedAt the submission time
     */
    public void setSubmittedAt(final Instant submittedAt) {
        this.submittedAt = submittedAt;
    }

}
