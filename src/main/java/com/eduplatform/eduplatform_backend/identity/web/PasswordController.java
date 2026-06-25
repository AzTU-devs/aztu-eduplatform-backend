package com.eduplatform.eduplatform_backend.identity.web;

import com.eduplatform.eduplatform_backend.identity.service.PasswordResetService;
import com.eduplatform.eduplatform_backend.identity.web.dto.ForgotPasswordRequest;
import com.eduplatform.eduplatform_backend.identity.web.dto.ResetPasswordRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/password")
@Tag(name = "Auth — Password", description = "Forgot / reset password (public)")
public class PasswordController {

    private final PasswordResetService passwordReset;

    public PasswordController(PasswordResetService passwordReset) {
        this.passwordReset = passwordReset;
    }

    @PostMapping("/forgot")
    @Operation(
            summary = "Request a password-reset link",
            description = "Always returns 204 — the response does not reveal whether the email exists. "
                    + "If it does, a reset link is emailed (valid 30 minutes).",
            security = {})
    public ResponseEntity<Void> forgot(@Valid @RequestBody ForgotPasswordRequest req) {
        passwordReset.requestReset(req.email());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/reset")
    @Operation(
            summary = "Reset the password using a token from the emailed link",
            description = "Returns 400 INVALID_OR_EXPIRED_TOKEN if the token is unknown, expired, or already used.",
            security = {})
    public ResponseEntity<Void> reset(@Valid @RequestBody ResetPasswordRequest req) {
        passwordReset.reset(req.token(), req.newPassword());
        return ResponseEntity.noContent().build();
    }
}
