package com.eduplatform.eduplatform_backend.video.repo;

import com.eduplatform.eduplatform_backend.video.domain.Video;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface VideoRepository extends JpaRepository<Video, UUID> {

    /** The caller's non-deleted videos (the {@code @SQLRestriction} scopes out soft-deleted rows). */
    Page<Video> findAllByOwnerUserIdOrderByCreatedAtDesc(UUID ownerUserId, Pageable pageable);
}
