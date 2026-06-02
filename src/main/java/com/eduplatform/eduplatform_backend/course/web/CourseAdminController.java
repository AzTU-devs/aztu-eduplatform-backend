package com.eduplatform.eduplatform_backend.course.web;

import com.eduplatform.eduplatform_backend.common.security.AuthenticatedPrincipal;
import com.eduplatform.eduplatform_backend.common.security.CurrentUser;
import com.eduplatform.eduplatform_backend.common.web.ApiResponse;
import com.eduplatform.eduplatform_backend.course.service.CourseService;
import com.eduplatform.eduplatform_backend.course.web.dto.CourseDto;
import com.eduplatform.eduplatform_backend.course.web.mapper.CourseMapper;
import com.eduplatform.eduplatform_backend.tutor.web.dto.ApprovalDecisionRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/courses")
@Tag(name = "Admin — Courses")
public class CourseAdminController {

    private final CourseService service;
    private final CourseMapper mapper;

    public CourseAdminController(CourseService service, CourseMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @PostMapping("/{id}/decision")
    @PreAuthorize("hasAuthority('course:approve')")
    @Operation(summary = "Approve or reject a course awaiting review")
    public ApiResponse<CourseDto> decide(@PathVariable UUID id,
                                         @Valid @RequestBody ApprovalDecisionRequest req,
                                         @CurrentUser AuthenticatedPrincipal me) {
        return ApiResponse.ok(mapper.toDto(service.adminDecide(id, me.userId(), req.decision(), req.note())));
    }
}
