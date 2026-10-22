package com.perihelionApi.course;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseRepository extends JpaRepository<Course, Long> {

	List<Course> findAllByOrderByTitleAsc();

	List<Course> findByTitleContainingIgnoreCaseOrderByTitleAsc(String title);

	List<Course> findByCodeOrderByTitleAsc(String code);

	List<Course> findByTitleContainingIgnoreCaseAndCodeOrderByTitleAsc(String title, String code);

}
