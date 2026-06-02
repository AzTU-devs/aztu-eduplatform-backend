package com.eduplatform.eduplatform_backend.tutor.web;

import com.eduplatform.eduplatform_backend.common.enums.TutorApprovalStatus;
import com.eduplatform.eduplatform_backend.common.security.AuthenticatedPrincipal;
import com.eduplatform.eduplatform_backend.common.security.CurrentUser;
import com.eduplatform.eduplatform_backend.common.web.ApiResponse;
import com.eduplatform.eduplatform_backend.common.web.PageResponse;
import com.eduplatform.eduplatform_backend.tutor.service.TutorService;
import com.eduplatform.eduplatform_backend.tutor.web.dto.ApprovalDecisionRequest;
import com.eduplatform.eduplatform_backend.tutor.web.dto.TutorApplyRequest;
import com.eduplatform.eduplatform_backend.tutor.web.dto.TutorProfileDto;
import com.eduplatform.eduplatform_backend.tutor.web.mapper.TutorMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/portal/tutor")
@Tag(name = "Portal — Tutor")
public class TutorController {

    private final TutorService service;
    private final TutorMapper mapper;

    public TutorController(TutorService service, TutorMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @PostMapping("/apply")
    @PreAuthorize("hasAuthority('tutor:apply')")
    @Operation(summary = "Apply to become a tutor")
    public ResponseEntity<ApiResponse<TutorProfileDto>> apply(@Valid @RequestBody TutorApplyRequest req,
                                                              @CurrentUser AuthenticatedPrincipal me) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(mapper.toDto(service.apply(me.userId(), req))));
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get my tutor profile")
    public ApiResponse<TutorProfileDto> me(@CurrentUser AuthenticatedPrincipal me) {
        return ApiResponse.ok(mapper.toDto(service.myProfile(me.userId())));
    }

    // --- admin ---

    @GetMapping("/admin")
    @PreAuthorize("hasAuthority('tutor:approve')")
    @Operation(summary = "List tutor profiles by approval status")
    public ApiResponse<PageResponse<TutorProfileDto>> list(
            @RequestParam(defaultValue = "PENDING") TutorApprovalStatus status,
            Pageable pageable) {
        return ApiResponse.ok(PageResponse.of(service.listByStatus(status, pageable), mapper::toDto));
    }

    @PostMapping("/admin/{tutorId}/decision")
    @PreAuthorize("hasAuthority('tutor:approve')")
    @Operation(summary = "Approve or reject a tutor application")
    public ApiResponse<TutorProfileDto> decide(@PathVariable UUID tutorId,
                                               @Valid @RequestBody ApprovalDecisionRequest req,
                                               @CurrentUser AuthenticatedPrincipal me) {
        return ApiResponse.ok(mapper.toDto(service.decide(tutorId, me.userId(), req)));
    }
}
