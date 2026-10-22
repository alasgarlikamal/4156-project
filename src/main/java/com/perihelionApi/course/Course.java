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

@Entity
@Table(name = "course")
public class Course {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "code", length = 32)
	private String code;

	@Column(name = "title", nullable = false, length = 255)
	private String title;

	@Column(name = "term_start", nullable = false)
	private LocalDate termStart;

	@Column(name = "term_end", nullable = false)
	private LocalDate termEnd;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public LocalDate getTermStart() {
		return termStart;
	}

	public void setTermStart(LocalDate termStart) {
		this.termStart = termStart;
	}

	public LocalDate getTermEnd() {
		return termEnd;
	}

	public void setTermEnd(LocalDate termEnd) {
		this.termEnd = termEnd;
	}

}
