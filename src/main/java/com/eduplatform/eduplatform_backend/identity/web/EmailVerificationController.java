package com.eduplatform.eduplatform_backend.identity.web;

import com.eduplatform.eduplatform_backend.common.security.AuthenticatedPrincipal;
import com.eduplatform.eduplatform_backend.common.security.CurrentUser;
import com.eduplatform.eduplatform_backend.identity.service.EmailVerificationService;
import com.eduplatform.eduplatform_backend.identity.web.dto.VerifyEmailRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/email")
@Tag(name = "Auth — Email Verification", description = "Verify email address / resend verification link")
public class EmailVerificationController {

    private final EmailVerificationService emailVerification;

    public EmailVerificationController(EmailVerificationService emailVerification) {
        this.emailVerification = emailVerification;
    }

    @PostMapping("/verify")
    @Operation(
            summary = "Verify an email address using a token from the emailed link",
            description = "Returns 400 INVALID_OR_EXPIRED_TOKEN if the token is unknown, expired, or already used.",
            security = {})
    public ResponseEntity<Void> verify(@Valid @RequestBody VerifyEmailRequest req) {
        emailVerification.verify(req.token());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/resend")
    @Operation(
            summary = "Resend a verification link to the current user",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<Void> resend(@CurrentUser AuthenticatedPrincipal me) {
        emailVerification.issueAndSend(me.userId());
        return ResponseEntity.noContent().build();
    }
}
