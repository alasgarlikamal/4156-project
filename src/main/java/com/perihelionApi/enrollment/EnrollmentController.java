/*
	AI usage: Course and Enrollment CRUD: Grok 4.7, ~111K tokens
 */
package com.perihelionApi.enrollment;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/enrollments")
public class EnrollmentController {

	private final EnrollmentService enrollmentService;

	public EnrollmentController(EnrollmentService enrollmentService) {
		this.enrollmentService = enrollmentService;
	}

	@PostMapping
	public ResponseEntity<EnrollmentResponse> create(@Valid @RequestBody EnrollmentRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(enrollmentService.create(request));
	}

	@GetMapping
	public List<EnrollmentResponse> list(@RequestParam(required = false) Long courseId,
			@RequestParam(required = false) UUID learnerRef) {
		return enrollmentService.list(courseId, learnerRef);
	}

	@GetMapping("/{id}")
	public EnrollmentResponse get(@PathVariable Long id) {
		return enrollmentService.get(id);
	}

	@PutMapping("/{id}")
	public EnrollmentResponse update(@PathVariable Long id, @Valid @RequestBody EnrollmentRequest request) {
		return enrollmentService.update(id, request);
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable Long id) {
		enrollmentService.delete(id);
		return ResponseEntity.noContent().build();
	}

}
