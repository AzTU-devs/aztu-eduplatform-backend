package com.eduplatform.eduplatform_backend.notification.web;

import jakarta.validation.constraints.NotBlank;

/**
 * Admin broadcast payload.
 *
 * @param title   notification title (required)
 * @param body    notification body (required)
 * @param role    target role: USER/TUTOR/ADMIN/SUPER_ADMIN, or {@code null} for all users
 * @param channel IN_APP or EMAIL; {@code null} defaults to IN_APP
 */
public record BroadcastRequest(
        @NotBlank String title,
        @NotBlank String body,
        String role,
        String channel
) {}
