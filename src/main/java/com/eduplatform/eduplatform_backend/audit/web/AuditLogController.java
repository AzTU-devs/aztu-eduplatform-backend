package com.eduplatform.eduplatform_backend.audit.web;

import com.eduplatform.eduplatform_backend.audit.repo.AuditLogRepository;
import com.eduplatform.eduplatform_backend.audit.web.dto.AuditLogDto;
import com.eduplatform.eduplatform_backend.common.error.Errors;
import com.eduplatform.eduplatform_backend.common.web.ApiResponse;
import com.eduplatform.eduplatform_backend.common.web.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/super/audit-logs")
@Tag(name = "Super — Audit Logs", description = "Read the platform audit trail (super admin)")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAuthority('audit:read')")
public class AuditLogController {

    private final AuditLogRepository auditLogs;

    public AuditLogController(AuditLogRepository auditLogs) {
        this.auditLogs = auditLogs;
    }

    @GetMapping
    @Operation(summary = "List audit-log entries (paged, filterable by action / entityType / actorId)")
    public ApiResponse<PageResponse<AuditLogDto>> list(
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) UUID actorId,
            @PageableDefault(size = 20) Pageable pageable) {
        String actionFilter = blankToNull(action);
        String entityTypeFilter = blankToNull(entityType);
        return ApiResponse.ok(PageResponse.of(
                auditLogs.search(actionFilter, entityTypeFilter, actorId, pageable), AuditLogDto::from));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a single audit-log entry")
    public ApiResponse<AuditLogDto> get(@PathVariable UUID id) {
        return ApiResponse.ok(auditLogs.findById(id)
                .map(AuditLogDto::from)
                .orElseThrow(() -> Errors.notFound("AUDIT_LOG_NOT_FOUND", "Audit log entry does not exist")));
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
