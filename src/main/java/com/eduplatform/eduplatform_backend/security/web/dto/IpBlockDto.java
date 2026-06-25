package com.eduplatform.eduplatform_backend.security.web.dto;

import com.eduplatform.eduplatform_backend.audit.domain.IpBlock;

import java.time.Instant;
import java.util.UUID;

/** Super-admin view of an IP block. */
public record IpBlockDto(
        UUID id,
        String ipAddress,
        String reason,
        UUID createdBy,
        Instant createdAt,
        Instant expiresAt
) {
    public static IpBlockDto from(IpBlock b) {
        return new IpBlockDto(
                b.getId(),
                b.getIpAddress(),
                b.getReason(),
                b.getCreatedBy(),
                b.getCreatedAt(),
                b.getExpiresAt());
    }
}
