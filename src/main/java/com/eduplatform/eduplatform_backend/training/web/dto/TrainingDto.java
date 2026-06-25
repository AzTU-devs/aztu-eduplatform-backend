package com.eduplatform.eduplatform_backend.training.web.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record TrainingDto(
        UUID id,
        UUID tutorId,
        String title,
        String description,
        String city,
        String addressLine,
        LocalDate startDate,
        LocalDate endDate,
        int capacity,
        int enrolledCount,
        BigDecimal price,
        String currency,
        String status,
        Instant createdAt,
        Instant updatedAt
) {}
