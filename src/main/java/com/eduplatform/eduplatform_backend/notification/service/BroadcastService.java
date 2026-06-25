package com.eduplatform.eduplatform_backend.notification.service;

import com.eduplatform.eduplatform_backend.common.enums.NotificationChannel;
import com.eduplatform.eduplatform_backend.identity.domain.User;
import com.eduplatform.eduplatform_backend.identity.repo.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Fan-out helper for admin broadcasts. Targets every user (optionally filtered by
 * role) and dispatches one notification each via {@link NotificationDispatcher}.
 *
 * NOTE: this loads the full user table via {@code findAll()} and filters in memory.
 * That is acceptable for current volumes but should be batched/paged for very
 * large user bases.
 */
@Service
public class BroadcastService {

    private final UserRepository users;
    private final NotificationDispatcher dispatcher;

    public BroadcastService(UserRepository users, NotificationDispatcher dispatcher) {
        this.users = users;
        this.dispatcher = dispatcher;
    }

    /**
     * @param title   notification title
     * @param body    notification body
     * @param role    target role name (USER/TUTOR/ADMIN/SUPER_ADMIN) or {@code null} for everyone
     * @param channel delivery channel
     * @return number of recipients dispatched to
     */
    @Transactional
    public int broadcast(String title, String body, String role, NotificationChannel channel) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("kind", "BROADCAST");

        List<User> all = users.findAll();
        int count = 0;
        for (User user : all) {
            if (role != null && !hasRole(user, role)) {
                continue;
            }
            dispatcher.dispatch(user, "BROADCAST", title, body, payload, channel);
            count++;
        }
        return count;
    }

    private boolean hasRole(User user, String role) {
        return user.getUserRoles().stream()
                .map(ur -> ur.getRole().getCode())
                .anyMatch(code -> code != null && code.name().equals(role));
    }
}
