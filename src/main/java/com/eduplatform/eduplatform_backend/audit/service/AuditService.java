package com.eduplatform.eduplatform_backend.audit.service;

import com.eduplatform.eduplatform_backend.audit.domain.AuditLog;
import com.eduplatform.eduplatform_backend.audit.domain.SecurityEvent;
import com.eduplatform.eduplatform_backend.audit.repo.AuditLogRepository;
import com.eduplatform.eduplatform_backend.audit.repo.SecurityEventRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Persists audit + security events. All methods are best-effort: any failure is
 * logged and swallowed so the calling business flow is never broken by an audit
 * write. HTTP request may be {@code null} (e.g. background / non-web callers).
 */
@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);
    private static final int UA_MAX = 255;

    private final SecurityEventRepository securityEvents;
    private final AuditLogRepository auditLogs;

    public AuditService(SecurityEventRepository securityEvents, AuditLogRepository auditLogs) {
        this.securityEvents = securityEvents;
        this.auditLogs = auditLogs;
    }

    /**
     * Record a security event (login success/failure, lockout, etc.). Never throws.
     * Runs in its own transaction so the event is persisted even when the calling
     * flow later rolls back (e.g. a failed login that throws after recording).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordSecurityEvent(UUID userId, String eventType,
                                    Map<String, Object> detail, HttpServletRequest http) {
        try {
            SecurityEvent event = SecurityEvent.builder()
                    .id(UUID.randomUUID())
                    .userId(userId)
                    .eventType(eventType)
                    .ipAddress(remoteAddr(http))
                    .userAgent(userAgent(http))
                    .detail(detail)
                    .occurredAt(Instant.now())
                    .build();
            securityEvents.save(event);
        } catch (Exception ex) {
            log.warn("[audit] failed to record security event type={} user={}: {}",
                    eventType, userId, ex.getMessage());
        }
    }

    /** Record an audit-log entry for a state-changing action. Never throws. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordAudit(UUID actorId, String actorRole, String action,
                            String entityType, UUID entityId,
                            Map<String, Object> before, Map<String, Object> after,
                            HttpServletRequest http) {
        try {
            AuditLog entry = AuditLog.builder()
                    .id(UUID.randomUUID())
                    .actorId(actorId)
                    .actorRole(actorRole)
                    .action(action)
                    .entityType(entityType)
                    .entityId(entityId)
                    .beforeData(before)
                    .afterData(after)
                    .ipAddress(remoteAddr(http))
                    .userAgent(userAgent(http))
                    .occurredAt(Instant.now())
                    .build();
            auditLogs.save(entry);
        } catch (Exception ex) {
            log.warn("[audit] failed to record audit action={} entityType={} entityId={}: {}",
                    action, entityType, entityId, ex.getMessage());
        }
    }

    private static String remoteAddr(HttpServletRequest http) {
        if (http == null) return null;
        try {
            return http.getRemoteAddr();
        } catch (Exception ex) {
            return null;
        }
    }

    private static String userAgent(HttpServletRequest http) {
        if (http == null) return null;
        try {
            String ua = http.getHeader("User-Agent");
            if (ua == null) return null;
            return ua.length() > UA_MAX ? ua.substring(0, UA_MAX) : ua;
        } catch (Exception ex) {
            return null;
        }
    }
}
