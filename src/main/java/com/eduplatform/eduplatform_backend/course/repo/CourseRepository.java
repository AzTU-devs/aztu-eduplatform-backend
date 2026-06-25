package com.eduplatform.eduplatform_backend.course.repo;

import com.eduplatform.eduplatform_backend.common.enums.CourseStatus;
import com.eduplatform.eduplatform_backend.common.enums.CourseType;
import com.eduplatform.eduplatform_backend.course.domain.Course;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CourseRepository extends JpaRepository<Course, UUID>, JpaSpecificationExecutor<Course> {

    Optional<Course> findBySlug(String slug);

    boolean existsBySlug(String slug);

    Page<Course> findAllByStatus(CourseStatus status, Pageable pageable);

    Page<Course> findAllByCourseTypeAndStatus(CourseType courseType, CourseStatus status, Pageable pageable);

    Page<Course> findAllByTutorId(UUID tutorId, Pageable pageable);

    /** Courses owned by a tutor, filtered to a single status (tutor "my courses" / approval board / public profile). */
    Page<Course> findAllByTutorIdAndStatus(UUID tutorId, CourseStatus status, Pageable pageable);

    /** Full-text search over published courses; falls back to ILIKE when GIN cannot match. */
    @Query(value = """
           select * from courses c
           where c.deleted_at is null
             and c.status = 'PUBLISHED'
             and (
               to_tsvector('simple', c.title || ' ' || coalesce(c.subtitle,'') || ' ' || coalesce(c.description,''))
                 @@ plainto_tsquery('simple', :q)
               or c.title ilike concat('%', :q, '%')
             )
           order by c.published_at desc nulls last
           """,
           countQuery = """
           select count(*) from courses c
           where c.deleted_at is null
             and c.status = 'PUBLISHED'
             and (
               to_tsvector('simple', c.title || ' ' || coalesce(c.subtitle,'') || ' ' || coalesce(c.description,''))
                 @@ plainto_tsquery('simple', :q)
               or c.title ilike concat('%', :q, '%')
             )
           """,
           nativeQuery = true)
    Page<Course> search(@Param("q") String query, Pageable pageable);

    @Modifying
    @Query("update Course c set c.enrolledCount = c.enrolledCount + 1 where c.id = :id")
    int incrementEnrolledCount(@Param("id") UUID id);

    @Modifying
    @Query("update Course c set c.enrolledCount = c.enrolledCount - 1 where c.id = :id and c.enrolledCount > 0")
    int decrementEnrolledCount(@Param("id") UUID id);
}
