package com.eduplatform.eduplatform_backend.enrollment.service;

import com.eduplatform.eduplatform_backend.common.enums.CourseStatus;
import com.eduplatform.eduplatform_backend.common.enums.EnrollmentSource;
import com.eduplatform.eduplatform_backend.common.enums.EnrollmentStatus;
import com.eduplatform.eduplatform_backend.common.enums.LessonProgressStatus;
import com.eduplatform.eduplatform_backend.common.error.Errors;
import com.eduplatform.eduplatform_backend.course.domain.Course;
import com.eduplatform.eduplatform_backend.course.domain.Lesson;
import com.eduplatform.eduplatform_backend.course.repo.CourseRepository;
import com.eduplatform.eduplatform_backend.course.repo.LessonRepository;
import com.eduplatform.eduplatform_backend.enrollment.domain.Enrollment;
import com.eduplatform.eduplatform_backend.enrollment.domain.LessonProgress;
import com.eduplatform.eduplatform_backend.enrollment.domain.LessonProgressId;
import com.eduplatform.eduplatform_backend.enrollment.repo.EnrollmentRepository;
import com.eduplatform.eduplatform_backend.enrollment.repo.LessonProgressRepository;
import com.eduplatform.eduplatform_backend.enrollment.web.dto.LessonProgressUpdateRequest;
import com.eduplatform.eduplatform_backend.identity.domain.User;
import com.eduplatform.eduplatform_backend.identity.repo.UserRepository;
import com.eduplatform.eduplatform_backend.tutor.web.dto.TutorStudentDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class EnrollmentService {

    private final EnrollmentRepository enrollments;
    private final LessonProgressRepository progress;
    private final CourseRepository courses;
    private final LessonRepository lessons;
    private final UserRepository users;

    public EnrollmentService(EnrollmentRepository enrollments, LessonProgressRepository progress,
                             CourseRepository courses, LessonRepository lessons, UserRepository users) {
        this.enrollments = enrollments;
        this.progress = progress;
        this.courses = courses;
        this.lessons = lessons;
        this.users = users;
    }

    /** Free-tier or admin-grant enrollment. Paid enrollments are created by the payment flow. */
    @Transactional
    public Enrollment enroll(UUID userId, UUID courseId, EnrollmentSource source) {
        Course course = courses.findById(courseId)
                .orElseThrow(() -> Errors.notFound("COURSE_NOT_FOUND", "Course does not exist"));
        if (course.getStatus() != CourseStatus.PUBLISHED) {
            throw Errors.conflict("COURSE_NOT_PUBLISHED", "Cannot enrol in a non-published course");
        }
        if (enrollments.existsByUserIdAndCourseId(userId, courseId)) {
            throw Errors.conflict("ALREADY_ENROLLED", "You are already enrolled in this course");
        }
        if (source == EnrollmentSource.PURCHASE && !course.isFree()) {
            throw Errors.unprocessable("PAYMENT_REQUIRED",
                    "This course is paid; enrol via the checkout flow instead");
        }
        User user = users.findById(userId)
                .orElseThrow(() -> Errors.notFound("USER_NOT_FOUND", "User does not exist"));

        Enrollment e = Enrollment.builder()
                .user(user)
                .course(course)
                .status(EnrollmentStatus.ACTIVE)
                .source(source)
                .enrolledAt(Instant.now())
                .build();
        e.setId(UUID.randomUUID());
        e = enrollments.save(e);

        courses.incrementEnrolledCount(courseId);
        return e;
    }

    @Transactional(readOnly = true)
    public Page<Enrollment> mine(UUID userId, Pageable pageable) {
        return enrollments.findAllByUserId(userId, pageable);
    }

    /** Students enrolled in courses owned by the calling tutor (resolved by the tutor's owning user id). */
    @Transactional(readOnly = true)
    public Page<TutorStudentDto> studentsOfTutor(UUID tutorUserId, Pageable pageable) {
        return enrollments.findStudentsOfTutor(tutorUserId, pageable);
    }

    /** Saved lesson progress for the user's enrolment in a course; empty if not enrolled or no progress. */
    @Transactional(readOnly = true)
    public List<LessonProgress> courseProgress(UUID userId, UUID courseId) {
        return enrollments.findByUserIdAndCourseId(userId, courseId)
                .map(e -> progress.findAllByEnrollmentId(e.getId()))
                .orElseGet(List::of);
    }

    @Transactional
    public LessonProgress updateProgress(UUID userId, UUID courseId, UUID lessonId,
                                         LessonProgressUpdateRequest req) {
        Enrollment e = enrollments.findByUserIdAndCourseId(userId, courseId)
                .orElseThrow(() -> Errors.forbidden("NOT_ENROLLED", "You are not enrolled in this course"));
        Lesson lesson = lessons.findById(lessonId)
                .orElseThrow(() -> Errors.notFound("LESSON_NOT_FOUND", "Lesson does not exist"));
        if (!lesson.getModule().getCourse().getId().equals(courseId)) {
            throw Errors.badRequest("LESSON_COURSE_MISMATCH", "Lesson does not belong to this course");
        }

        LessonProgressId pk = new LessonProgressId(e.getId(), lessonId);
        LessonProgress lp = progress.findById(pk).orElseGet(() -> {
            LessonProgress fresh = LessonProgress.builder()
                    .id(pk).enrollment(e).lesson(lesson)
                    .status(LessonProgressStatus.NOT_STARTED)
                    .positionSec(0)
                    .updatedAt(Instant.now())
                    .build();
            return progress.save(fresh);
        });
        lp.setStatus(req.status());
        lp.setPositionSec(req.positionSec());
        if (req.status() == LessonProgressStatus.COMPLETED && lp.getCompletedAt() == null) {
            lp.setCompletedAt(Instant.now());
        }
        progress.save(lp);

        // Recompute coarse progress %
        long total = lessons.countByCourseId(courseId);
        long completed = progress.countCompletedByEnrollment(e.getId());
        short pct = total == 0 ? 0 : (short) Math.round(100.0 * completed / total);
        e.setProgressPercent(pct);
        e.setLastAccessedAt(Instant.now());
        if (pct == 100 && e.getCompletedAt() == null) {
            e.setCompletedAt(Instant.now());
            e.setStatus(EnrollmentStatus.COMPLETED);
        }
        enrollments.save(e);
        return lp;
    }
}
