package com.eduplatform.eduplatform_backend.audit.web;

import com.eduplatform.eduplatform_backend.audit.repo.ApiRequestLogRepository;
import com.eduplatform.eduplatform_backend.audit.web.dto.ApiRequestLogDto;
import com.eduplatform.eduplatform_backend.common.web.ApiResponse;
import com.eduplatform.eduplatform_backend.common.web.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/super/api-logs")
@Tag(name = "Super — API Logs", description = "Read captured API request logs (super admin)")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAuthority('apilog:read')")
public class ApiLogController {

    private final ApiRequestLogRepository requestLogs;

    public ApiLogController(ApiRequestLogRepository requestLogs) {
        this.requestLogs = requestLogs;
    }

    @GetMapping
    @Operation(summary = "List API request logs (paged, filterable by status / method)")
    public ApiResponse<PageResponse<ApiRequestLogDto>> list(
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String method,
            @PageableDefault(size = 20) Pageable pageable) {
        String methodFilter = (method == null || method.isBlank()) ? null : method.trim().toUpperCase();
        return ApiResponse.ok(PageResponse.of(
                requestLogs.search(status, methodFilter, pageable), ApiRequestLogDto::from));
    }
}
