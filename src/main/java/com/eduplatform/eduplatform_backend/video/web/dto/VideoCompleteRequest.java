package com.eduplatform.eduplatform_backend.video.web.dto;

import jakarta.validation.constraints.PositiveOrZero;

/** Finalises an upload, optionally recording the measured duration. */
public record VideoCompleteRequest(
        @PositiveOrZero Integer durationSec
) {}
