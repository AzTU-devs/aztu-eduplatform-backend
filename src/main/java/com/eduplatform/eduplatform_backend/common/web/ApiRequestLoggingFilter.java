package com.eduplatform.eduplatform_backend.common.web;

import com.eduplatform.eduplatform_backend.audit.domain.ApiRequestLog;
import com.eduplatform.eduplatform_backend.audit.domain.IpBlock;
import com.eduplatform.eduplatform_backend.audit.repo.ApiRequestLogRepository;
import com.eduplatform.eduplatform_backend.audit.repo.IpBlockRepository;
import com.eduplatform.eduplatform_backend.common.security.AuthenticatedPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Cross-cutting servlet filter for {@code /api/**} traffic that:
 * <ol>
 *   <li>rejects requests from a currently-blocked client IP with a 403 before they
 *       reach the handler, and</li>
 *   <li>best-effort records an {@link ApiRequestLog} row (method, path, status,
 *       duration, IP, UA, actor) for super-admin observability.</li>
 * </ol>
 *
 * <p>Auto-registered by Spring Boot as a {@link Component} {@link OncePerRequestFilter}.
 * All persistence/IO failures are swallowed so the request flow is never broken.</p>
 */
@Component
public class ApiRequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(ApiRequestLoggingFilter.class);
    private static final int UA_MAX = 255;
    private static final int PATH_MAX = 512;
    private static final int QUERY_MAX = 1024;

    private final ApiRequestLogRepository requestLogs;
    private final IpBlockRepository ipBlocks;

    public ApiRequestLoggingFilter(ApiRequestLogRepository requestLogs, IpBlockRepository ipBlocks) {
        this.requestLogs = requestLogs;
        this.ipBlocks = ipBlocks;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        String path = req.getRequestURI();

        // Only concern ourselves with the API surface.
        if (path == null || !path.startsWith("/api/")) {
            chain.doFilter(req, res);
            return;
        }

        String ip = req.getRemoteAddr();

        // 1) IP-block enforcement.
        if (isBlocked(ip)) {
            res.setStatus(HttpServletResponse.SC_FORBIDDEN);
            res.setContentType(MediaType.APPLICATION_JSON_VALUE);
            res.getWriter().write("{\"status\":403,\"code\":\"IP_BLOCKED\",\"message\":\"Your IP address is blocked\"}");
            return;
        }

        // 2) Time the request and log it afterwards.
        long start = System.nanoTime();
        try {
            chain.doFilter(req, res);
        } finally {
            try {
                persist(req, res, ip, path, start);
            } catch (Exception ex) {
                log.debug("[api-log] failed to persist request log for {}: {}", path, ex.getMessage());
            }
        }
    }

    /** True when an unexpired block exists for this IP. Failures fall open (not blocked). */
    private boolean isBlocked(String ip) {
        if (ip == null || ip.isBlank()) return false;
        try {
            Optional<IpBlock> block = ipBlocks.findByIpAddress(ip);
            if (block.isEmpty()) return false;
            Instant expiresAt = block.get().getExpiresAt();
            return expiresAt == null || expiresAt.isAfter(Instant.now());
        } catch (Exception ex) {
            log.debug("[api-log] IP block lookup failed for {}: {}", ip, ex.getMessage());
            return false;
        }
    }

    private void persist(HttpServletRequest req, HttpServletResponse res, String ip, String path, long startNanos) {
        // Skip noise / non-API infra paths.
        if (path.startsWith("/actuator") || path.startsWith("/swagger") || path.startsWith("/v3/api-docs")) {
            return;
        }

        int durationMs = (int) ((System.nanoTime() - startNanos) / 1_000_000L);

        ApiRequestLog entry = ApiRequestLog.builder()
                .id(UUID.randomUUID())
                .actorId(currentActorId())
                .method(req.getMethod())
                .path(truncate(path, PATH_MAX))
                .queryString(truncate(req.getQueryString(), QUERY_MAX))
                .statusCode(res.getStatus())
                .durationMs(durationMs)
                .ipAddress(ip)
                .userAgent(truncate(req.getHeader("User-Agent"), UA_MAX))
                .occurredAt(Instant.now())
                .build();
        requestLogs.save(entry);
    }

    private static UUID currentActorId() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof AuthenticatedPrincipal p) {
                return p.userId();
            }
        } catch (Exception ignored) {
            // no authenticated actor
        }
        return null;
    }

    private static String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() > max ? s.substring(0, max) : s;
    }
}
