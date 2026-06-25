package com.eduplatform.eduplatform_backend.training.web;

import com.eduplatform.eduplatform_backend.common.security.AuthenticatedPrincipal;
import com.eduplatform.eduplatform_backend.common.security.CurrentUser;
import com.eduplatform.eduplatform_backend.common.web.ApiResponse;
import com.eduplatform.eduplatform_backend.common.web.PageResponse;
import com.eduplatform.eduplatform_backend.training.service.TrainingService;
import com.eduplatform.eduplatform_backend.training.web.dto.TrainingDto;
import com.eduplatform.eduplatform_backend.training.web.dto.TrainingUpsertRequest;
import com.eduplatform.eduplatform_backend.training.web.mapper.TrainingMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Tutor-facing training management. Requires {@code training:manage} and is
 * scoped to the caller's own tutor profile.
 */
@RestController
@RequestMapping("/api/portal/trainings")
@Tag(name = "Portal — Trainings (tutor)")
@PreAuthorize("hasAuthority('training:manage')")
public class TrainingController {

    private final TrainingService service;
    private final TrainingMapper mapper;

    public TrainingController(TrainingService service, TrainingMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @GetMapping
    @Operation(summary = "List the caller's trainings")
    public ApiResponse<PageResponse<TrainingDto>> list(Pageable pageable,
                                                       @CurrentUser AuthenticatedPrincipal me) {
        return ApiResponse.ok(PageResponse.of(service.listMine(me.userId(), pageable), mapper::toDto));
    }

    @PostMapping
    @Operation(summary = "Create a training")
    public ResponseEntity<ApiResponse<TrainingDto>> create(@Valid @RequestBody TrainingUpsertRequest req,
                                                           @CurrentUser AuthenticatedPrincipal me) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(mapper.toDto(service.create(req, me.userId()))));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one of the caller's trainings")
    public ApiResponse<TrainingDto> get(@PathVariable UUID id,
                                        @CurrentUser AuthenticatedPrincipal me) {
        return ApiResponse.ok(mapper.toDto(service.getMine(id, me.userId())));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Partially update one of the caller's trainings")
    public ApiResponse<TrainingDto> update(@PathVariable UUID id,
                                           @Valid @RequestBody TrainingUpsertRequest req,
                                           @CurrentUser AuthenticatedPrincipal me) {
        return ApiResponse.ok(mapper.toDto(service.update(id, req, me.userId())));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft-delete one of the caller's trainings")
    public ResponseEntity<Void> delete(@PathVariable UUID id,
                                       @CurrentUser AuthenticatedPrincipal me) {
        service.delete(id, me.userId());
        return ResponseEntity.noContent().build();
    }
}
