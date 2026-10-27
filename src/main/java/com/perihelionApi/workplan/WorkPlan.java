/*
    AI usage: Work plan CRUD: Grok 4.7, ~238K tokens
 */
package com.perihelionApi.workplan;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

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

/**
 * A learner's study plan for an enrollment over a date range.
 */
@Entity
@Table(name = "work_plan",
        indexes = @Index(name = "work_plan_enrollment_idx",
                columnList = "enrollment_id"))
public class WorkPlan {

    /** Surrogate primary key. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The enrollment this plan belongs to. */
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "enrollment_id", nullable = false)
    private Enrollment enrollment;

    /** First day covered by the plan. */
    @Column(name = "range_start", nullable = false)
    private LocalDate rangeStart;

    /** Last day covered by the plan. */
    @Column(name = "range_end", nullable = false)
    private LocalDate rangeEnd;

    /** Hours the learner can spend on the plan each day. */
    @Column(name = "available_hours_per_day", nullable = false)
    private BigDecimal availableHoursPerDay;

    /** When the plan was created. */
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

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
     * Returns the first day covered by the plan.
     *
     * @return the range start
     */
    public LocalDate getRangeStart() {
        return rangeStart;
    }

    /**
     * Sets the first day covered by the plan.
     *
     * @param rangeStart the range start
     */
    public void setRangeStart(final LocalDate rangeStart) {
        this.rangeStart = rangeStart;
    }

    /**
     * Returns the last day covered by the plan.
     *
     * @return the range end
     */
    public LocalDate getRangeEnd() {
        return rangeEnd;
    }

    /**
     * Sets the last day covered by the plan.
     *
     * @param rangeEnd the range end
     */
    public void setRangeEnd(final LocalDate rangeEnd) {
        this.rangeEnd = rangeEnd;
    }

    /**
     * Returns the hours available per day.
     *
     * @return the available hours per day
     */
    public BigDecimal getAvailableHoursPerDay() {
        return availableHoursPerDay;
    }

    /**
     * Sets the hours available per day.
     *
     * @param availableHoursPerDay the available hours per day
     */
    public void setAvailableHoursPerDay(
            final BigDecimal availableHoursPerDay) {
        this.availableHoursPerDay = availableHoursPerDay;
    }

    /**
     * Returns the creation time.
     *
     * @return the creation time
     */
    public Instant getCreatedAt() {
        return createdAt;
    }

    /**
     * Sets the creation time.
     *
     * @param createdAt the creation time
     */
    public void setCreatedAt(final Instant createdAt) {
        this.createdAt = createdAt;
    }

}
