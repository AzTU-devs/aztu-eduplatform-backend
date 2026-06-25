package com.eduplatform.eduplatform_backend.security.web.dto;

import com.eduplatform.eduplatform_backend.audit.domain.SecurityEvent;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/** Super-admin view of a single security event. */
public record SecurityEventDto(
        UUID id,
        UUID userId,
        String eventType,
        String ipAddress,
        String userAgent,
        Map<String, Object> detail,
        Instant occurredAt
) {
    public static SecurityEventDto from(SecurityEvent e) {
        return new SecurityEventDto(
                e.getId(),
                e.getUserId(),
                e.getEventType(),
                e.getIpAddress(),
                e.getUserAgent(),
                e.getDetail(),
                e.getOccurredAt());
    }
}
