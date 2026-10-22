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

@Entity
@Table(name = "enrollment",
		uniqueConstraints = @UniqueConstraint(name = "enrollment_course_learner_uk",
				columnNames = { "course_id", "learner_ref" }),
		indexes = @Index(name = "enrollment_course_idx", columnList = "course_id"))
public class Enrollment {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "course_id", nullable = false)
	private Course course;

	@Column(name = "learner_ref", nullable = false, length = 255)
	private String learnerRef;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Course getCourse() {
		return course;
	}

	public void setCourse(Course course) {
		this.course = course;
	}

	public String getLearnerRef() {
		return learnerRef;
	}

	public void setLearnerRef(String learnerRef) {
		this.learnerRef = learnerRef;
	}

}
