package com.eduplatform.eduplatform_backend.training.web;

import com.eduplatform.eduplatform_backend.common.web.ApiResponse;
import com.eduplatform.eduplatform_backend.common.web.PageResponse;
import com.eduplatform.eduplatform_backend.training.service.TrainingService;
import com.eduplatform.eduplatform_backend.training.web.dto.TrainingDto;
import com.eduplatform.eduplatform_backend.training.web.mapper.TrainingMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public training catalog — PUBLISHED, non-deleted trainings. No auth required
 * ({@code /api/public/**} GET is permitted in SecurityConfig).
 */
@RestController
@RequestMapping("/api/public/trainings")
@Tag(name = "Public — Trainings")
public class TrainingPublicController {

    private final TrainingService service;
    private final TrainingMapper mapper;

    public TrainingPublicController(TrainingService service, TrainingMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @GetMapping
    @Operation(summary = "List published trainings", security = {})
    public ApiResponse<PageResponse<TrainingDto>> list(Pageable pageable) {
        return ApiResponse.ok(PageResponse.of(service.listPublic(pageable), mapper::toDto));
    }
}
