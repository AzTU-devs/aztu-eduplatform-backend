package com.eduplatform.eduplatform_backend.audit.web.dto;

import com.eduplatform.eduplatform_backend.audit.domain.ApiRequestLog;

import java.time.Instant;
import java.util.UUID;

/** Super-admin view of a single API request-log row. */
public record ApiRequestLogDto(
        UUID id,
        UUID actorId,
        String method,
        String path,
        String queryString,
        int statusCode,
        int durationMs,
        String ipAddress,
        String userAgent,
        UUID requestId,
        Instant occurredAt
) {
    public static ApiRequestLogDto from(ApiRequestLog l) {
        return new ApiRequestLogDto(
                l.getId(),
                l.getActorId(),
                l.getMethod(),
                l.getPath(),
                l.getQueryString(),
                l.getStatusCode(),
                l.getDurationMs(),
                l.getIpAddress(),
                l.getUserAgent(),
                l.getRequestId(),
                l.getOccurredAt());
    }
}
