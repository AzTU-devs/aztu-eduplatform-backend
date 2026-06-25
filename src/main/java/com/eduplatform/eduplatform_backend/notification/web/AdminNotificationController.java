package com.eduplatform.eduplatform_backend.notification.web;

import com.eduplatform.eduplatform_backend.common.enums.NotificationChannel;
import com.eduplatform.eduplatform_backend.common.enums.RoleCode;
import com.eduplatform.eduplatform_backend.common.error.Errors;
import com.eduplatform.eduplatform_backend.common.web.ApiResponse;
import com.eduplatform.eduplatform_backend.notification.service.BroadcastService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Admin broadcast endpoint — sends a notification to every user, optionally
 * filtered by role. Requires {@code notification:manage}.
 */
@RestController
@RequestMapping("/api/admin/notifications")
@Tag(name = "Admin — Notifications")
@PreAuthorize("hasAuthority('notification:manage')")
public class AdminNotificationController {

    private final BroadcastService broadcast;

    public AdminNotificationController(BroadcastService broadcast) {
        this.broadcast = broadcast;
    }

    @PostMapping("/broadcast")
    @Operation(summary = "Broadcast a notification to all users (optionally by role)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> broadcast(@Valid @RequestBody BroadcastRequest req) {
        String role = normalizeRole(req.role());
        NotificationChannel channel = resolveChannel(req.channel());
        int recipientCount = broadcast.broadcast(req.title(), req.body(), role, channel);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.ok(Map.of("recipientCount", recipientCount)));
    }

    /** Validate the optional role against {@link RoleCode}; null/blank means "all users". */
    private String normalizeRole(String role) {
        if (role == null || role.isBlank()) {
            return null;
        }
        try {
            return RoleCode.valueOf(role).name();
        } catch (IllegalArgumentException ex) {
            throw Errors.badRequest("INVALID_ROLE", "Unknown role: " + role);
        }
    }

    /** Only IN_APP or EMAIL are accepted; null/blank defaults to IN_APP. */
    private NotificationChannel resolveChannel(String channel) {
        if (channel == null || channel.isBlank()) {
            return NotificationChannel.IN_APP;
        }
        NotificationChannel resolved;
        try {
            resolved = NotificationChannel.valueOf(channel);
        } catch (IllegalArgumentException ex) {
            throw Errors.badRequest("INVALID_CHANNEL", "Unsupported channel: " + channel);
        }
        if (resolved != NotificationChannel.IN_APP && resolved != NotificationChannel.EMAIL) {
            throw Errors.badRequest("INVALID_CHANNEL", "Channel must be IN_APP or EMAIL");
        }
        return resolved;
    }
}
