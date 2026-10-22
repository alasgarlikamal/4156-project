package com.perihelionApi.enrollment;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record EnrollmentRequest(@NotNull Long courseId, @NotNull UUID learnerRef) {
}
