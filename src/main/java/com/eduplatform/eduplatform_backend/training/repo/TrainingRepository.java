package com.eduplatform.eduplatform_backend.training.repo;

import com.eduplatform.eduplatform_backend.training.domain.Training;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface TrainingRepository extends JpaRepository<Training, UUID> {

    /** A tutor profile's non-deleted trainings (newest first). */
    Page<Training> findAllByTutorIdOrderByCreatedAtDesc(UUID tutorId, Pageable pageable);

    /** Trainings in a given lifecycle status (used by the public catalog). */
    Page<Training> findAllByStatusOrderByCreatedAtDesc(String status, Pageable pageable);
}
