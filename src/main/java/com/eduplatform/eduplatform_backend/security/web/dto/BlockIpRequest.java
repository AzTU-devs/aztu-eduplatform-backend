package com.eduplatform.eduplatform_backend.security.web.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.Instant;

/** Request body to create an IP block. {@code expiresAt} is optional (null = permanent). */
public record BlockIpRequest(
        @NotBlank String ipAddress,
        String reason,
        Instant expiresAt
) {}
