package com.eduplatform.eduplatform_backend.video.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

/** Begins an upload: registers metadata before the bytes are pushed. */
public record VideoInitRequest(
        @NotBlank String title,
        String filename,
        String mimeType,
        @PositiveOrZero long sizeBytes
) {}
