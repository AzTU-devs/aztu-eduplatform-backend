package com.eduplatform.eduplatform_backend.audit.web.dto;

import com.eduplatform.eduplatform_backend.audit.domain.AuditLog;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/** Super-admin view of a single audit-log entry. */
public record AuditLogDto(
        UUID id,
        UUID actorId,
        String actorRole,
        String action,
        String entityType,
        UUID entityId,
        Map<String, Object> before,
        Map<String, Object> after,
        String ipAddress,
        String userAgent,
        UUID requestId,
        Instant occurredAt
) {
    public static AuditLogDto from(AuditLog l) {
        return new AuditLogDto(
                l.getId(),
                l.getActorId(),
                l.getActorRole(),
                l.getAction(),
                l.getEntityType(),
                l.getEntityId(),
                l.getBeforeData(),
                l.getAfterData(),
                l.getIpAddress(),
                l.getUserAgent(),
                l.getRequestId(),
                l.getOccurredAt());
    }
}
