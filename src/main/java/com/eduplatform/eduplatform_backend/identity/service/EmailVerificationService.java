package com.eduplatform.eduplatform_backend.identity.service;

import com.eduplatform.eduplatform_backend.common.error.Errors;
import com.eduplatform.eduplatform_backend.common.mail.MailService;
import com.eduplatform.eduplatform_backend.common.security.TokenHasher;
import com.eduplatform.eduplatform_backend.identity.domain.EmailVerificationToken;
import com.eduplatform.eduplatform_backend.identity.domain.User;
import com.eduplatform.eduplatform_backend.identity.repo.EmailVerificationTokenRepository;
import com.eduplatform.eduplatform_backend.identity.repo.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Email-address verification via single-use, hashed tokens. The raw token is only
 * ever returned in the emailed link; the DB stores its SHA-256 digest.
 */
@Service
public class EmailVerificationService {

    private static final int TOKEN_BYTES = 32;
    private static final long TTL_MINUTES = 60 * 24;

    private final UserRepository users;
    private final EmailVerificationTokenRepository tokens;
    private final MailService mail;

    public EmailVerificationService(UserRepository users, EmailVerificationTokenRepository tokens,
                                    MailService mail) {
        this.users = users;
        this.tokens = tokens;
        this.mail = mail;
    }

    /** Issue a fresh verification token for the user and email the verify link. */
    @Transactional
    public void issueAndSend(User user) {
        if (user == null) return;
        String rawToken = TokenHasher.randomToken(TOKEN_BYTES);
        EmailVerificationToken entity = EmailVerificationToken.builder()
                .id(UUID.randomUUID())
                .userId(user.getId())
                .tokenHash(TokenHasher.sha256Hex(rawToken))
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plus(Duration.ofMinutes(TTL_MINUTES)))
                .build();
        tokens.save(entity);

        String link = mail.publicBaseUrl() + "/verify-email?token=" + rawToken;
        mail.sendEmailVerification(user.getEmail(), link, TTL_MINUTES);
    }

    /** Issue a fresh verification token for the given user id (e.g. resend for current user). */
    @Transactional
    public void issueAndSend(UUID userId) {
        User user = users.findById(userId)
                .orElseThrow(() -> Errors.notFound("USER_NOT_FOUND", "User does not exist"));
        issueAndSend(user);
    }

    /** Consume a token and mark the user's email verified. Throws on invalid/expired/consumed. */
    @Transactional
    public void verify(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw Errors.badRequest("INVALID_OR_EXPIRED_TOKEN", "Verification link is invalid or has expired");
        }
        EmailVerificationToken token = tokens.findByTokenHash(TokenHasher.sha256Hex(rawToken.trim()))
                .orElseThrow(() -> Errors.badRequest("INVALID_OR_EXPIRED_TOKEN",
                        "Verification link is invalid or has expired"));

        if (token.getConsumedAt() != null || token.getExpiresAt().isBefore(Instant.now())) {
            throw Errors.badRequest("INVALID_OR_EXPIRED_TOKEN", "Verification link is invalid or has expired");
        }

        User user = users.findById(token.getUserId())
                .orElseThrow(() -> Errors.badRequest("INVALID_OR_EXPIRED_TOKEN",
                        "Verification link is invalid or has expired"));

        user.setEmailVerifiedAt(Instant.now());
        users.save(user);

        token.setConsumedAt(Instant.now());
        tokens.save(token);
    }
}
