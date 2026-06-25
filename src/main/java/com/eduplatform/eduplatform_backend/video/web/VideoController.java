package com.eduplatform.eduplatform_backend.video.web;

import com.eduplatform.eduplatform_backend.common.security.AuthenticatedPrincipal;
import com.eduplatform.eduplatform_backend.common.security.CurrentUser;
import com.eduplatform.eduplatform_backend.common.web.ApiResponse;
import com.eduplatform.eduplatform_backend.common.web.PageResponse;
import com.eduplatform.eduplatform_backend.video.service.VideoService;
import com.eduplatform.eduplatform_backend.video.web.dto.VideoCompleteRequest;
import com.eduplatform.eduplatform_backend.video.web.dto.VideoDto;
import com.eduplatform.eduplatform_backend.video.web.dto.VideoInitRequest;
import com.eduplatform.eduplatform_backend.video.web.mapper.VideoMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * Owner-scoped video library. Every endpoint requires {@code video:manage} and
 * additionally checks that the caller owns the targeted video.
 */
@RestController
@RequestMapping("/api/videos")
@Tag(name = "Videos")
@PreAuthorize("hasAuthority('video:manage')")
public class VideoController {

    private final VideoService service;
    private final VideoMapper mapper;

    public VideoController(VideoService service, VideoMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @GetMapping
    @Operation(summary = "List the caller's videos (newest first)")
    public ApiResponse<PageResponse<VideoDto>> list(Pageable pageable,
                                                    @CurrentUser AuthenticatedPrincipal me) {
        return ApiResponse.ok(PageResponse.of(service.list(me.userId(), pageable), mapper::toDto));
    }

    @PostMapping("/init")
    @Operation(summary = "Register a PENDING video and obtain its upload path")
    public ResponseEntity<ApiResponse<VideoDto>> init(@Valid @RequestBody VideoInitRequest req,
                                                      @CurrentUser AuthenticatedPrincipal me) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(mapper.toDto(service.init(req, me.userId()))));
    }

    @PutMapping(value = "/{id}/content", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload the raw bytes for a video (multipart)")
    public ApiResponse<VideoDto> upload(@PathVariable UUID id,
                                        @RequestParam("file") MultipartFile file,
                                        @CurrentUser AuthenticatedPrincipal me) {
        return ApiResponse.ok(mapper.toDto(service.uploadContent(id, file, me.userId())));
    }

    @PostMapping("/{id}/complete")
    @Operation(summary = "Mark a video READY and record its duration")
    public ApiResponse<VideoDto> complete(@PathVariable UUID id,
                                          @Valid @RequestBody(required = false) VideoCompleteRequest req,
                                          @CurrentUser AuthenticatedPrincipal me) {
        Integer durationSec = req == null ? null : req.durationSec();
        return ApiResponse.ok(mapper.toDto(service.complete(id, durationSec, me.userId())));
    }

    @GetMapping("/{id}/content")
    @Operation(summary = "Stream the stored bytes of a video")
    public ResponseEntity<Resource> content(@PathVariable UUID id,
                                            @CurrentUser AuthenticatedPrincipal me) {
        VideoService.Content c = service.loadContent(id, me.userId());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(c.mimeType()))
                .contentLength(c.byteSize())
                .header(HttpHeaders.CACHE_CONTROL, "private, max-age=3600")
                .body(c.resource());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft-delete a video and remove its stored bytes")
    public ResponseEntity<Void> delete(@PathVariable UUID id,
                                       @CurrentUser AuthenticatedPrincipal me) {
        service.delete(id, me.userId());
        return ResponseEntity.noContent().build();
    }
}
