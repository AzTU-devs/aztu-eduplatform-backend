package com.eduplatform.eduplatform_backend.tutor.web;

import com.eduplatform.eduplatform_backend.common.web.ApiResponse;
import com.eduplatform.eduplatform_backend.common.web.PageResponse;
import com.eduplatform.eduplatform_backend.course.service.CourseService;
import com.eduplatform.eduplatform_backend.course.web.dto.CourseSummaryDto;
import com.eduplatform.eduplatform_backend.course.web.mapper.CourseMapper;
import com.eduplatform.eduplatform_backend.tutor.service.TutorService;
import com.eduplatform.eduplatform_backend.tutor.web.dto.TutorProfileDto;
import com.eduplatform.eduplatform_backend.tutor.web.mapper.TutorMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/public/tutors")
@Tag(name = "Public — Tutors")
public class TutorPublicController {

    private final TutorService tutorService;
    private final TutorMapper tutorMapper;
    private final CourseService courseService;
    private final CourseMapper courseMapper;

    public TutorPublicController(TutorService tutorService, TutorMapper tutorMapper,
                                 CourseService courseService, CourseMapper courseMapper) {
        this.tutorService = tutorService;
        this.tutorMapper = tutorMapper;
        this.courseService = courseService;
        this.courseMapper = courseMapper;
    }

    @GetMapping("/{tutorId}")
    @Operation(summary = "Public tutor profile (approved tutors only)", security = {})
    public ApiResponse<TutorProfileDto> profile(@PathVariable UUID tutorId) {
        return ApiResponse.ok(tutorMapper.toDto(tutorService.publicProfile(tutorId)));
    }

    @GetMapping("/{tutorId}/courses")
    @Operation(summary = "A tutor's published courses", security = {})
    public ApiResponse<PageResponse<CourseSummaryDto>> courses(@PathVariable UUID tutorId, Pageable pageable) {
        return ApiResponse.ok(PageResponse.of(
                courseService.listPublishedByTutor(tutorId, pageable), courseMapper::toSummaryDto));
    }
}
