package com.eduplatform.eduplatform_backend.identity.service;

import com.eduplatform.eduplatform_backend.common.error.Errors;
import com.eduplatform.eduplatform_backend.common.mail.MailService;
import com.eduplatform.eduplatform_backend.common.security.TokenHasher;
import com.eduplatform.eduplatform_backend.identity.domain.PasswordResetToken;
import com.eduplatform.eduplatform_backend.identity.domain.User;
import com.eduplatform.eduplatform_backend.identity.repo.PasswordResetTokenRepository;
import com.eduplatform.eduplatform_backend.identity.repo.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Forgot/reset password flow using single-use, hashed tokens. Raw tokens are only
 * ever returned in the emailed link; the DB stores their SHA-256 digest.
 */
@Service
public class PasswordResetService {

    private static final int TOKEN_BYTES = 32;
    private static final long TTL_MINUTES = 30;

    private final UserRepository users;
    private final PasswordResetTokenRepository tokens;
    private final PasswordEncoder encoder;
    private final MailService mail;

    public PasswordResetService(UserRepository users, PasswordResetTokenRepository tokens,
                                PasswordEncoder encoder, MailService mail) {
        this.users = users;
        this.tokens = tokens;
        this.encoder = encoder;
        this.mail = mail;
    }

    /**
     * Begin a reset. Silent no-op when the email is unknown (callers must not reveal
     * account existence). When the user exists, stores a hashed token and emails a link.
     */
    @Transactional
    public void requestReset(String email) {
        if (email == null || email.isBlank()) return;
        users.findByEmailIgnoreCase(email.trim()).ifPresent(user -> {
            String rawToken = TokenHasher.randomToken(TOKEN_BYTES);
            PasswordResetToken entity = PasswordResetToken.builder()
                    .id(UUID.randomUUID())
                    .userId(user.getId())
                    .tokenHash(TokenHasher.sha256Hex(rawToken))
                    .createdAt(Instant.now())
                    .expiresAt(Instant.now().plus(Duration.ofMinutes(TTL_MINUTES)))
                    .build();
            tokens.save(entity);

            String link = mail.publicBaseUrl() + "/reset-password?token=" + rawToken;
            mail.sendPasswordReset(user.getEmail(), link, TTL_MINUTES);
        });
    }

    /** Consume a token and set a new password. Throws on missing/expired/consumed tokens. */
    @Transactional
    public void reset(String rawToken, String newPassword) {
        if (rawToken == null || rawToken.isBlank()) {
            throw Errors.badRequest("INVALID_OR_EXPIRED_TOKEN", "Reset link is invalid or has expired");
        }
        PasswordResetToken token = tokens.findByTokenHash(TokenHasher.sha256Hex(rawToken.trim()))
                .orElseThrow(() -> Errors.badRequest("INVALID_OR_EXPIRED_TOKEN",
                        "Reset link is invalid or has expired"));

        if (token.getConsumedAt() != null || token.getExpiresAt().isBefore(Instant.now())) {
            throw Errors.badRequest("INVALID_OR_EXPIRED_TOKEN", "Reset link is invalid or has expired");
        }

        User user = users.findById(token.getUserId())
                .orElseThrow(() -> Errors.badRequest("INVALID_OR_EXPIRED_TOKEN",
                        "Reset link is invalid or has expired"));

        user.setPasswordHash(encoder.encode(newPassword));
        users.save(user);

        token.setConsumedAt(Instant.now());
        tokens.save(token);
    }
}
