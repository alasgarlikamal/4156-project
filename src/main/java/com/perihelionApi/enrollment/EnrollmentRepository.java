package com.perihelionApi.enrollment;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

	boolean existsByCourse_Id(Long courseId);

	boolean existsByCourse_IdAndLearnerRef(Long courseId, String learnerRef);

	boolean existsByCourse_IdAndLearnerRefAndIdNot(Long courseId, String learnerRef, Long id);

	List<Enrollment> findAllByOrderByIdAsc();

	List<Enrollment> findByCourse_IdOrderByIdAsc(Long courseId);

	List<Enrollment> findByLearnerRefOrderByIdAsc(String learnerRef);

	List<Enrollment> findByCourse_IdAndLearnerRefOrderByIdAsc(Long courseId, String learnerRef);

}
