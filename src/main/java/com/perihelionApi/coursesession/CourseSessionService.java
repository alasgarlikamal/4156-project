// Wrote file with github copilot used 6 credits
package com.perihelionApi.coursesession;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.perihelionApi.common.NotFoundException;
import com.perihelionApi.course.Course;
import com.perihelionApi.course.CourseRepository;

/**
 * Business logic for weekly course meetings and calendar export.
 */
@Service
@Transactional
public class CourseSessionService {

    /** Persistence for courses. */
    private final CourseRepository courseRepository;

    /** Persistence for course meeting times. */
    private final CourseSessionRepository sessionRepository;

    /**
     * Creates the service.
     *
     * @param newCourseRepository persistence for courses
     * @param newSessionRepository persistence for course sessions
     */
    public CourseSessionService(final CourseRepository newCourseRepository,
            final CourseSessionRepository newSessionRepository) {
        this.courseRepository = newCourseRepository;
        this.sessionRepository = newSessionRepository;
    }

    /**
     * Creates a meeting time for a course.
     *
     * @param courseId the course identifier
     * @param request  meeting time values
     * @return the persisted session
     */
    public CourseSessionResponse create(final Long courseId,
            final CourseSessionRequest request) {
        CourseSession session = new CourseSession();
        session.setCourse(findCourse(courseId));
        apply(session, request);
        return CourseSessionResponse.from(sessionRepository.save(session));
    }

    /**
     * Lists a course's meeting times.
     *
     * @param courseId the course identifier
     * @return ordered meeting times
     */
    @Transactional(readOnly = true)
    public List<CourseSessionResponse> list(final Long courseId) {
        findCourse(courseId);
        return sessionRepository
                .findByCourseIdOrderByWeekdayAscStartTimeAsc(courseId)
                .stream().map(CourseSessionResponse::from).toList();
    }

    /**
     * Replaces a meeting time belonging to the specified course.
     *
     * @param courseId  the course identifier
     * @param sessionId the session identifier
     * @param request   replacement values
     * @return the updated session
     */
    public CourseSessionResponse update(final Long courseId,
            final Long sessionId, final CourseSessionRequest request) {
        Course course = findCourse(courseId);
        CourseSession session = findSession(courseId, sessionId);
        session.setCourse(course);
        apply(session, request);
        return CourseSessionResponse.from(sessionRepository.save(session));
    }

    /**
     * Deletes a meeting time belonging to the specified course.
     *
     * @param courseId  the course identifier
     * @param sessionId the session identifier
     */
    public void delete(final Long courseId, final Long sessionId) {
        findCourse(courseId);
        sessionRepository.delete(findSession(courseId, sessionId));
    }

    /**
     * Exports all meetings during the course term as an iCalendar file.
     *
     * @param courseId the course identifier
     * @return RFC 5545 calendar text
     */
    @Transactional(readOnly = true)
    public String exportCalendar(final Long courseId) {
        Course course = findCourse(courseId);
        List<CourseSession> sessions = sessionRepository
                .findByCourseIdOrderByWeekdayAscStartTimeAsc(courseId);
        return CourseCalendarExporter.export(course, sessions);
    }

    private Course findCourse(final Long courseId) {
        return courseRepository.findById(courseId)
                .orElseThrow(() -> new NotFoundException("Course not found"));
    }

    private CourseSession findSession(final Long courseId,
            final Long sessionId) {
        CourseSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() ->
                        new NotFoundException("Course session not found"));
        if (!session.getCourse().getId().equals(courseId)) {
            throw new NotFoundException("Course session not found");
        }
        return session;
    }

    private void apply(final CourseSession session,
            final CourseSessionRequest request) {
        session.setWeekday(request.weekday());
        session.setStartTime(request.startTime());
        session.setEndTime(request.endTime());
    }
}
