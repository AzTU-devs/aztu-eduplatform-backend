package com.eduplatform.eduplatform_backend.course.repo;

import com.eduplatform.eduplatform_backend.course.domain.Lesson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LessonRepository extends JpaRepository<Lesson, UUID> {

    List<Lesson> findAllByModuleIdOrderByOrderIndexAsc(UUID moduleId);

    @Query("""
           select l from Lesson l
             join l.module m
           where m.course.id = :courseId
           order by m.orderIndex asc, l.orderIndex asc
           """)
    List<Lesson> findAllByCourseId(@Param("courseId") UUID courseId);

    @Query("""
           select count(l) from Lesson l
             join l.module m
           where m.course.id = :courseId
           """)
    long countByCourseId(@Param("courseId") UUID courseId);
}
