/*
	AI usage: Course and Enrollment CRUD: Grok 4.7, ~111K tokens
 */
package com.perihelionApi.course;

import java.util.List;

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
@RequestMapping("/api/courses")
public class CourseController {

	private final CourseService courseService;

	public CourseController(CourseService courseService) {
		this.courseService = courseService;
	}

	@PostMapping
	public ResponseEntity<CourseResponse> create(@Valid @RequestBody CourseRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(courseService.create(request));
	}

	@GetMapping
	public List<CourseResponse> list(@RequestParam(required = false) String title,
			@RequestParam(required = false) String code) {
		return courseService.list(title, code);
	}

	@GetMapping("/{id}")
	public CourseResponse get(@PathVariable Long id) {
		return courseService.get(id);
	}

	@PutMapping("/{id}")
	public CourseResponse update(@PathVariable Long id, @Valid @RequestBody CourseRequest request) {
		return courseService.update(id, request);
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable Long id) {
		courseService.delete(id);
		return ResponseEntity.noContent().build();
	}

}
