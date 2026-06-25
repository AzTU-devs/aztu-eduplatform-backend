package com.eduplatform.eduplatform_backend.security.web;

import com.eduplatform.eduplatform_backend.common.security.AuthenticatedPrincipal;
import com.eduplatform.eduplatform_backend.common.security.CurrentUser;
import com.eduplatform.eduplatform_backend.common.web.ApiResponse;
import com.eduplatform.eduplatform_backend.common.web.PageResponse;
import com.eduplatform.eduplatform_backend.security.service.SecurityConsoleService;
import com.eduplatform.eduplatform_backend.security.web.dto.BlockIpRequest;
import com.eduplatform.eduplatform_backend.security.web.dto.IpBlockDto;
import com.eduplatform.eduplatform_backend.security.web.dto.SecurityEventDto;
import com.eduplatform.eduplatform_backend.security.web.dto.SecurityOverviewDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/super/security")
@Tag(name = "Super — Security Console", description = "Security overview, events, IP blocks, account unlock")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAuthority('security:manage')")
public class SecurityConsoleController {

    private final SecurityConsoleService service;

    public SecurityConsoleController(SecurityConsoleService service) {
        this.service = service;
    }

    @GetMapping("/overview")
    @Operation(summary = "Security posture: recent login failures, active IP blocks, locked accounts")
    public ApiResponse<SecurityOverviewDto> overview() {
        return ApiResponse.ok(service.overview());
    }

    @GetMapping("/events")
    @Operation(summary = "List security events (paged, filterable by type)")
    public ApiResponse<PageResponse<SecurityEventDto>> events(
            @RequestParam(required = false) String type,
            @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.ok(PageResponse.of(service.events(type, pageable)));
    }

    @PostMapping("/block-ip")
    @Operation(summary = "Block a client IP address")
    public ResponseEntity<ApiResponse<IpBlockDto>> blockIp(@Valid @RequestBody BlockIpRequest req,
                                                           @CurrentUser AuthenticatedPrincipal me) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(service.blockIp(req, me.userId())));
    }

    @DeleteMapping("/block-ip/{id}")
    @Operation(summary = "Remove an IP block")
    public ResponseEntity<Void> unblockIp(@PathVariable UUID id) {
        service.unblockIp(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/unlock/{userId}")
    @Operation(summary = "Unlock a user account (status ACTIVE, reset failed logins)")
    public ApiResponse<Void> unlockUser(@PathVariable UUID userId) {
        service.unlockUser(userId);
        return ApiResponse.ok(null);
    }
}
