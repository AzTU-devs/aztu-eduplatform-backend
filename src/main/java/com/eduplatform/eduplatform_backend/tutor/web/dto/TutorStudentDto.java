package com.eduplatform.eduplatform_backend.tutor.web.dto;

import java.time.Instant;
import java.util.UUID;

/** A student enrolled in one of the calling tutor's courses. */
public record TutorStudentDto(
        UUID enrollmentId,
        UUID userId,
        String firstName,
        String lastName,
        String email,
        UUID courseId,
        String courseTitle,
        Instant enrolledAt,
        short progressPercent,
        String status
) {}
