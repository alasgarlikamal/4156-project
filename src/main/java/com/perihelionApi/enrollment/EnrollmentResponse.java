package com.perihelionApi.enrollment;

import java.util.UUID;

public record EnrollmentResponse(Long id, Long courseId, UUID learnerRef) {

	public static EnrollmentResponse from(Enrollment enrollment) {
		return new EnrollmentResponse(enrollment.getId(), enrollment.getCourse().getId(),
				UUID.fromString(enrollment.getLearnerRef()));
	}

}
