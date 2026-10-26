// Wrote file with github copilot used 6 credits
package com.perihelionApi.coursesession;

import java.time.LocalTime;

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
 * A weekly meeting time belonging to a course.
 */
@Entity
@Table(name = "course_session")
public class CourseSession {

    /** Surrogate primary key. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The course that owns this meeting time. */
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    /** ISO weekday number: Monday is 1 and Sunday is 7. */
    @Column(nullable = false)
    private Integer weekday;

    /** Local start time in the course calendar timezone. */
    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    /** Local end time in the course calendar timezone. */
    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    /**
     * Returns the session identifier.
     *
     * @return the identifier, or null before persistence
     */
    public Long getId() {
        return id;
    }

    /**
     * Sets the session identifier.
     *
     * @param newId the identifier
     */
    public void setId(final Long newId) {
        this.id = newId;
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
     * @param newCourse the course
     */
    public void setCourse(final Course newCourse) {
        this.course = newCourse;
    }

    /**
     * Returns the ISO weekday number.
     *
     * @return Monday as 1 through Sunday as 7
     */
    public Integer getWeekday() {
        return weekday;
    }

    /**
     * Sets the ISO weekday number.
     *
     * @param newWeekday Monday as 1 through Sunday as 7
     */
    public void setWeekday(final Integer newWeekday) {
        this.weekday = newWeekday;
    }

    /**
     * Returns the local meeting start time.
     *
     * @return the start time
     */
    public LocalTime getStartTime() {
        return startTime;
    }

    /**
     * Sets the local meeting start time.
     *
     * @param newStartTime the start time
     */
    public void setStartTime(final LocalTime newStartTime) {
        this.startTime = newStartTime;
    }

    /**
     * Returns the local meeting end time.
     *
     * @return the end time
     */
    public LocalTime getEndTime() {
        return endTime;
    }

    /**
     * Sets the local meeting end time.
     *
     * @param newEndTime the end time
     */
    public void setEndTime(final LocalTime newEndTime) {
        this.endTime = newEndTime;
    }
}
