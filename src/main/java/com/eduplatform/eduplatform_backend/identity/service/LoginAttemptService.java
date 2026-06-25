package com.eduplatform.eduplatform_backend.identity.service;

import com.eduplatform.eduplatform_backend.common.enums.UserStatus;
import com.eduplatform.eduplatform_backend.identity.domain.User;
import com.eduplatform.eduplatform_backend.identity.repo.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Persists failed-login bookkeeping (counter + lockout) in its own transaction.
 *
 * <p>The login flow throws {@code INVALID_CREDENTIALS} on a bad password, which would
 * roll back the surrounding {@code @Transactional} — and with it any failed-login
 * counter update. Running this in a {@link Propagation#REQUIRES_NEW} transaction
 * guarantees the increment and lockout survive that rollback.</p>
 */
@Service
public class LoginAttemptService {

    private final UserRepository users;

    public LoginAttemptService(UserRepository users) {
        this.users = users;
    }

    /**
     * Increment the user's failed-login counter; lock the account once it reaches
     * {@code threshold}. Returns the new attempt count (0 if the user vanished).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int recordFailure(UUID userId, int threshold) {
        User user = users.findById(userId).orElse(null);
        if (user == null) return 0;
        int attempts = user.getFailedLogins() + 1;
        user.setFailedLogins((short) attempts);
        if (attempts >= threshold) {
            user.setStatus(UserStatus.LOCKED);
        }
        users.save(user);
        return attempts;
    }
}
