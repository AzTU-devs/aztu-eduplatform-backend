package com.eduplatform.eduplatform_backend.analytics.web;

import com.eduplatform.eduplatform_backend.analytics.service.AnalyticsService;
import com.eduplatform.eduplatform_backend.analytics.web.dto.AnalyticsOverviewDto;
import com.eduplatform.eduplatform_backend.common.web.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/analytics")
@Tag(name = "Admin — Analytics")
public class AnalyticsController {

    private final AnalyticsService service;

    public AnalyticsController(AnalyticsService service) {
        this.service = service;
    }

    @GetMapping("/overview")
    @PreAuthorize("hasAuthority('analytics:read')")
    @Operation(summary = "Aggregate platform metrics for the admin dashboard")
    public ApiResponse<AnalyticsOverviewDto> overview() {
        return ApiResponse.ok(service.overview());
    }
}
