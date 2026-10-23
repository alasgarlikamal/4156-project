/*
    AI usage: Course and Enrollment CRUD: Grok 4.7, ~111K tokens
 */
package com.perihelionApi.course;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * A course offered during a term.
 */
@Entity
@Table(name = "course")
public class Course {

    /** Surrogate primary key. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Optional course code such as "COMS W4156". */
    @Column(name = "code", length = 32)
    private String code;

    /** Human-readable course title. */
    @Column(name = "title", nullable = false, length = 255)
    private String title;

    /** First day of the term. */
    @Column(name = "term_start", nullable = false)
    private LocalDate termStart;

    /** Last day of the term. */
    @Column(name = "term_end", nullable = false)
    private LocalDate termEnd;

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
     * Returns the course code.
     *
     * @return the code, or null if none was given
     */
    public String getCode() {
        return code;
    }

    /**
     * Sets the course code.
     *
     * @param code the code, or null to clear it
     */
    public void setCode(final String code) {
        this.code = code;
    }

    /**
     * Returns the course title.
     *
     * @return the title
     */
    public String getTitle() {
        return title;
    }

    /**
     * Sets the course title.
     *
     * @param title the title
     */
    public void setTitle(final String title) {
        this.title = title;
    }

    /**
     * Returns the first day of the term.
     *
     * @return the term start date
     */
    public LocalDate getTermStart() {
        return termStart;
    }

    /**
     * Sets the first day of the term.
     *
     * @param termStart the term start date
     */
    public void setTermStart(final LocalDate termStart) {
        this.termStart = termStart;
    }

    /**
     * Returns the last day of the term.
     *
     * @return the term end date
     */
    public LocalDate getTermEnd() {
        return termEnd;
    }

    /**
     * Sets the last day of the term.
     *
     * @param termEnd the term end date
     */
    public void setTermEnd(final LocalDate termEnd) {
        this.termEnd = termEnd;
    }

}
