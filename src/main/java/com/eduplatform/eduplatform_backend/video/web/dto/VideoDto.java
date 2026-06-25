package com.eduplatform.eduplatform_backend.video.web.dto;

import java.time.Instant;
import java.util.UUID;

public record VideoDto(
        UUID id,
        UUID ownerUserId,
        String title,
        String storage,
        String mimeType,
        long byteSize,
        Integer durationSec,
        String status,
        String uploadPath,
        Instant createdAt,
        Instant updatedAt
) {}
