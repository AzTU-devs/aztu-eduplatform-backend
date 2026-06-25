package com.eduplatform.eduplatform_backend.training.web.dto;

import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Create / partial-update payload. On PATCH only non-null fields are applied,
 * so all fields are optional here.
 */
public record TrainingUpsertRequest(
        String title,
        String description,
        String city,
        String addressLine,
        LocalDate startDate,
        LocalDate endDate,
        @PositiveOrZero Integer capacity,
        @PositiveOrZero BigDecimal price,
        String currency,
        String status
) {}
