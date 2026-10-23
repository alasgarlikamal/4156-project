package com.perihelionApi.submission;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.perihelionApi.common.ConflictException;
import com.perihelionApi.common.NotFoundException;
import com.perihelionApi.enrollment.Enrollment;
import com.perihelionApi.enrollment.EnrollmentRepository;

@ExtendWith(MockitoExtension.class)
class SubmissionServiceTest {

	private static final Instant SUBMITTED_AT = Instant.parse("2026-10-20T15:30:00Z");

	@Mock
	private SubmissionRepository submissionRepository;

	@Mock
	private EnrollmentRepository enrollmentRepository;

	@InjectMocks
	private SubmissionService submissionService;

	@Nested
	class Create {

		@Test
		void rejectsMissingEnrollment() {
			when(enrollmentRepository.findById(9L)).thenReturn(Optional.empty());

			assertThrows(NotFoundException.class,
					() -> submissionService.create(new SubmissionRequest(9L, 3L, SUBMITTED_AT)));
			verify(submissionRepository, never()).save(any());
		}

		@Test
		void rejectsDuplicateEnrollmentAndCoursework() {
			when(enrollmentRepository.findById(1L)).thenReturn(Optional.of(enrollment(1L)));
			when(submissionRepository.existsByEnrollmentIdAndCourseworkId(1L, 3L)).thenReturn(true);

			ConflictException exception = assertThrows(ConflictException.class,
					() -> submissionService.create(new SubmissionRequest(1L, 3L, SUBMITTED_AT)));

			assertEquals("A submission already exists for this enrollment and coursework", exception.getMessage());
			verify(submissionRepository, never()).save(any());
		}

		@Test
		void savesSubmissionWithGivenTime() {
			Enrollment enrollment = enrollment(4L);
			when(enrollmentRepository.findById(4L)).thenReturn(Optional.of(enrollment));
			when(submissionRepository.existsByEnrollmentIdAndCourseworkId(4L, 3L)).thenReturn(false);
			when(submissionRepository.save(any(Submission.class))).thenAnswer(invocation -> {
				Submission submission = invocation.getArgument(0);
				submission.setId(8L);
				return submission;
			});

			SubmissionResponse response = submissionService.create(new SubmissionRequest(4L, 3L, SUBMITTED_AT));

			assertEquals(new SubmissionResponse(8L, 4L, 3L, SUBMITTED_AT), response);
			ArgumentCaptor<Submission> captor = ArgumentCaptor.forClass(Submission.class);
			verify(submissionRepository).save(captor.capture());
			assertEquals(enrollment, captor.getValue().getEnrollment());
		}

		@Test
		void defaultsSubmittedAtToNow() {
			when(enrollmentRepository.findById(4L)).thenReturn(Optional.of(enrollment(4L)));
			when(submissionRepository.save(any(Submission.class))).thenAnswer(invocation -> invocation.getArgument(0));
			Instant before = Instant.now();

			SubmissionResponse response = submissionService.create(new SubmissionRequest(4L, 3L, null));

			assertNotNull(response.submittedAt());
			assertEquals(false, response.submittedAt().isBefore(before));
		}

	}

	@Nested
	class Read {

		@Test
		void getRejectsMissingSubmission() {
			when(submissionRepository.findById(5L)).thenReturn(Optional.empty());

			assertThrows(NotFoundException.class, () -> submissionService.get(5L));
		}

		@Test
		void getReturnsSubmission() {
			when(submissionRepository.findById(5L)).thenReturn(Optional.of(submission(5L, 4L, 3L)));

			assertEquals(new SubmissionResponse(5L, 4L, 3L, SUBMITTED_AT), submissionService.get(5L));
		}

		@Test
		void listWithoutFiltersReturnsAll() {
			when(submissionRepository.findAllByOrderByIdAsc()).thenReturn(List.of(submission(1L, 4L, 3L)));

			assertEquals(1, submissionService.list(null, null).size());
		}

		@Test
		void listFiltersByEnrollment() {
			when(submissionRepository.findByEnrollmentIdOrderByIdAsc(4L)).thenReturn(List.of(submission(1L, 4L, 3L)));

			assertEquals(4L, submissionService.list(4L, null).get(0).enrollmentId());
		}

		@Test
		void listFiltersByCoursework() {
			when(submissionRepository.findByCourseworkIdOrderByIdAsc(3L)).thenReturn(List.of(submission(1L, 4L, 3L)));

			assertEquals(3L, submissionService.list(null, 3L).get(0).courseworkId());
		}

		@Test
		void listFiltersByEnrollmentAndCoursework() {
			when(submissionRepository.findByEnrollmentIdAndCourseworkIdOrderByIdAsc(4L, 3L)).thenReturn(List.of());

			assertEquals(List.of(), submissionService.list(4L, 3L));
		}

	}

	@Nested
	class Update {

		@Test
		void rejectsMissingSubmission() {
			when(submissionRepository.findById(5L)).thenReturn(Optional.empty());

			assertThrows(NotFoundException.class,
					() -> submissionService.update(5L, new SubmissionRequest(4L, 3L, null)));
		}

		@Test
		void rejectsDuplicateOfAnotherSubmission() {
			when(submissionRepository.findById(5L)).thenReturn(Optional.of(submission(5L, 4L, 3L)));
			when(enrollmentRepository.findById(4L)).thenReturn(Optional.of(enrollment(4L)));
			when(submissionRepository.existsByEnrollmentIdAndCourseworkIdAndIdNot(4L, 7L, 5L)).thenReturn(true);

			assertThrows(ConflictException.class,
					() -> submissionService.update(5L, new SubmissionRequest(4L, 7L, null)));
			verify(submissionRepository, never()).save(any());
		}

		@Test
		void keepsSubmittedAtWhenOmitted() {
			when(submissionRepository.findById(5L)).thenReturn(Optional.of(submission(5L, 4L, 3L)));
			when(enrollmentRepository.findById(4L)).thenReturn(Optional.of(enrollment(4L)));
			when(submissionRepository.save(any(Submission.class))).thenAnswer(invocation -> invocation.getArgument(0));

			SubmissionResponse response = submissionService.update(5L, new SubmissionRequest(4L, 7L, null));

			assertEquals(new SubmissionResponse(5L, 4L, 7L, SUBMITTED_AT), response);
		}

		@Test
		void replacesSubmittedAtWhenGiven() {
			Instant later = SUBMITTED_AT.plusSeconds(3600);
			when(submissionRepository.findById(5L)).thenReturn(Optional.of(submission(5L, 4L, 3L)));
			when(enrollmentRepository.findById(4L)).thenReturn(Optional.of(enrollment(4L)));
			when(submissionRepository.save(any(Submission.class))).thenAnswer(invocation -> invocation.getArgument(0));

			SubmissionResponse response = submissionService.update(5L, new SubmissionRequest(4L, 3L, later));

			assertEquals(later, response.submittedAt());
		}

	}

	@Nested
	class Delete {

		@Test
		void rejectsMissingSubmission() {
			when(submissionRepository.existsById(5L)).thenReturn(false);

			assertThrows(NotFoundException.class, () -> submissionService.delete(5L));
			verify(submissionRepository, never()).deleteById(any());
		}

		@Test
		void deletesExistingSubmission() {
			when(submissionRepository.existsById(5L)).thenReturn(true);

			submissionService.delete(5L);

			verify(submissionRepository).deleteById(5L);
		}

	}

	private static Enrollment enrollment(Long id) {
		Enrollment enrollment = new Enrollment();
		enrollment.setId(id);
		return enrollment;
	}

	private static Submission submission(Long id, Long enrollmentId, Long courseworkId) {
		Submission submission = new Submission();
		submission.setId(id);
		submission.setEnrollment(enrollment(enrollmentId));
		submission.setCourseworkId(courseworkId);
		submission.setSubmittedAt(SUBMITTED_AT);
		return submission;
	}

}
