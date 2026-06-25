package com.eduplatform.eduplatform_backend.enrollment.repo;

import com.eduplatform.eduplatform_backend.enrollment.domain.Enrollment;
import com.eduplatform.eduplatform_backend.tutor.web.dto.TutorStudentDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, UUID> {

    Optional<Enrollment> findByUserIdAndCourseId(UUID userId, UUID courseId);

    boolean existsByUserIdAndCourseId(UUID userId, UUID courseId);

    Page<Enrollment> findAllByUserId(UUID userId, Pageable pageable);

    Page<Enrollment> findAllByCourseId(UUID courseId, Pageable pageable);

    long countByCourseId(UUID courseId);

    /** Students enrolled in courses owned by the tutor whose owning user id is {@code tutorUserId}. */
    @Query("""
           select new com.eduplatform.eduplatform_backend.tutor.web.dto.TutorStudentDto(
               e.id, u.id, u.firstName, u.lastName, u.email,
               c.id, c.title, e.enrolledAt, e.progressPercent, cast(e.status as string))
           from Enrollment e
             join e.user u
             join e.course c
             join c.tutor t
           where t.user.id = :tutorUserId
           order by e.enrolledAt desc
           """)
    Page<TutorStudentDto> findStudentsOfTutor(@Param("tutorUserId") UUID tutorUserId, Pageable pageable);
}
