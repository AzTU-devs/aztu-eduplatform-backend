package com.eduplatform.eduplatform_backend.media.repo;

import com.eduplatform.eduplatform_backend.common.enums.MediaStatus;
import com.eduplatform.eduplatform_backend.media.domain.MediaFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MediaFileRepository extends JpaRepository<MediaFile, UUID> {

    List<MediaFile> findAllByOwnerUserId(UUID ownerUserId);

    List<MediaFile> findAllByStatus(MediaStatus status);
}
