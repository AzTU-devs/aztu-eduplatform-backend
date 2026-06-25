package com.eduplatform.eduplatform_backend.common.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Production-ready, config-driven mail sender with a safe development fallback.
 *
 * <p>Sending is enabled only when SMTP credentials are present
 * ({@code spring.mail.username} non-blank) <em>and</em> a {@link JavaMailSender}
 * bean was auto-configured. Otherwise every message is logged instead of sent,
 * so local/dev environments keep working without an SMTP server — OTPs and reset
 * links appear in the application log.</p>
 *
 * <p>Failures never propagate: a send error is logged and swallowed so that the
 * calling flow (signup, password reset, notifications) is not aborted by a
 * transient mail outage. The OTP/link is always logged at INFO when sending is
 * disabled and at DEBUG otherwise, so it can be recovered in non-prod.</p>
 */
@Service
public class MailService {

    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    private final ObjectProvider<JavaMailSender> senderProvider;
    private final String username;
    private final String from;
    private final String publicBaseUrl;

    public MailService(ObjectProvider<JavaMailSender> senderProvider,
                       @Value("${spring.mail.username:}") String username,
                       @Value("${app.mail.from:no-reply@eduplatform.local}") String from,
                       @Value("${app.frontend.public-url:http://localhost:3000}") String publicBaseUrl) {
        this.senderProvider = senderProvider;
        this.username = username;
        this.from = from;
        this.publicBaseUrl = publicBaseUrl;
    }

    /** True when real SMTP delivery is configured. */
    public boolean isEnabled() {
        return username != null && !username.isBlank() && senderProvider.getIfAvailable() != null;
    }

    /** Base URL of the public frontend, for building deep links in emails. */
    public String publicBaseUrl() {
        return publicBaseUrl;
    }

    /** Send a plain-text email. Best-effort: errors are logged, never thrown. */
    public void send(String to, String subject, String body) {
        if (!isEnabled()) {
            log.info("[mail:disabled] to={} subject=\"{}\"\n{}", to, subject, body);
            return;
        }
        JavaMailSender sender = senderProvider.getIfAvailable();
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(from);
            msg.setTo(to);
            msg.setSubject(subject);
            msg.setText(body);
            sender.send(msg);
            log.debug("[mail:sent] to={} subject=\"{}\"", to, subject);
        } catch (Exception ex) {
            log.warn("[mail:failed] to={} subject=\"{}\": {}", to, subject, ex.getMessage());
        }
    }

    /** Send a one-time passcode for the given purpose (e.g. "tutor registration"). */
    public void sendOtp(String to, String otp, String purpose, long ttlMinutes) {
        String subject = "Your AzTU EduPlatform verification code";
        String body = """
                Your verification code for %s is:

                    %s

                This code expires in %d minutes. If you did not request it, ignore this email.
                """.formatted(purpose, otp, ttlMinutes);
        if (!isEnabled()) {
            // Surface the code prominently in dev so signup flows are testable.
            log.info("[mail:disabled] OTP for {} ({}) = {} (valid {} min)", to, purpose, otp, ttlMinutes);
            return;
        }
        send(to, subject, body);
    }

    /** Send a password-reset link. {@code link} is the full reset URL. */
    public void sendPasswordReset(String to, String link, long ttlMinutes) {
        String subject = "Reset your AzTU EduPlatform password";
        String body = """
                We received a request to reset your password.

                Open the link below to choose a new one (valid %d minutes):

                    %s

                If you did not request this, you can safely ignore this email.
                """.formatted(ttlMinutes, link);
        if (!isEnabled()) {
            log.info("[mail:disabled] password reset link for {} = {}", to, link);
            return;
        }
        send(to, subject, body);
    }

    /** Send an email-verification link. {@code link} is the full verify URL. */
    public void sendEmailVerification(String to, String link, long ttlMinutes) {
        String subject = "Verify your AzTU EduPlatform email";
        String body = """
                Welcome to AzTU EduPlatform! Please confirm your email address.

                Open the link below to verify (valid %d minutes):

                    %s
                """.formatted(ttlMinutes, link);
        if (!isEnabled()) {
            log.info("[mail:disabled] email verification link for {} = {}", to, link);
            return;
        }
        send(to, subject, body);
    }
}
