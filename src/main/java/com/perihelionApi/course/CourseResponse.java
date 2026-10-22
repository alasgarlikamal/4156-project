package com.perihelionApi.course;

import java.time.LocalDate;

public record CourseResponse(Long id, String code, String title, LocalDate termStart, LocalDate termEnd) {

	public static CourseResponse from(Course course) {
		return new CourseResponse(course.getId(), course.getCode(), course.getTitle(), course.getTermStart(),
				course.getTermEnd());
	}

}
