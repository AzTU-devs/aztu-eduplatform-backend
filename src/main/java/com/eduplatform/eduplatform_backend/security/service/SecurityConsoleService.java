package com.eduplatform.eduplatform_backend.security.service;

import com.eduplatform.eduplatform_backend.audit.domain.IpBlock;
import com.eduplatform.eduplatform_backend.audit.domain.SecurityEvent;
import com.eduplatform.eduplatform_backend.audit.repo.IpBlockRepository;
import com.eduplatform.eduplatform_backend.audit.repo.SecurityEventRepository;
import com.eduplatform.eduplatform_backend.common.enums.UserStatus;
import com.eduplatform.eduplatform_backend.common.error.Errors;
import com.eduplatform.eduplatform_backend.identity.domain.User;
import com.eduplatform.eduplatform_backend.identity.repo.UserRepository;
import com.eduplatform.eduplatform_backend.security.web.dto.BlockIpRequest;
import com.eduplatform.eduplatform_backend.security.web.dto.IpBlockDto;
import com.eduplatform.eduplatform_backend.security.web.dto.SecurityEventDto;
import com.eduplatform.eduplatform_backend.security.web.dto.SecurityOverviewDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/** Backing service for the super-admin security console. */
@Service
public class SecurityConsoleService {

    private static final String LOGIN_FAILURE = "LOGIN_FAILURE";

    private final SecurityEventRepository securityEvents;
    private final IpBlockRepository ipBlocks;
    private final UserRepository users;

    public SecurityConsoleService(SecurityEventRepository securityEvents,
                                  IpBlockRepository ipBlocks,
                                  UserRepository users) {
        this.securityEvents = securityEvents;
        this.ipBlocks = ipBlocks;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public SecurityOverviewDto overview() {
        Instant since = Instant.now().minus(Duration.ofHours(24));
        long recentFailures = securityEvents.countByEventTypeSince(LOGIN_FAILURE, since);
        long activeBlocks = ipBlocks.countActive(Instant.now());
        long lockedAccounts = users.findAllByStatus(UserStatus.LOCKED).size();
        return new SecurityOverviewDto(recentFailures, activeBlocks, lockedAccounts);
    }

    @Transactional(readOnly = true)
    public Page<SecurityEventDto> events(String type, Pageable pageable) {
        Page<SecurityEvent> page = (type == null || type.isBlank())
                ? securityEvents.findAllByOrderByOccurredAtDesc(pageable)
                : securityEvents.findAllByEventTypeOrderByOccurredAtDesc(type.trim(), pageable);
        return page.map(SecurityEventDto::from);
    }

    @Transactional
    public IpBlockDto blockIp(BlockIpRequest req, UUID createdBy) {
        String ip = req.ipAddress().trim();
        ipBlocks.findByIpAddress(ip).ifPresent(existing -> {
            throw Errors.conflict("IP_ALREADY_BLOCKED", "This IP address is already blocked");
        });
        IpBlock block = IpBlock.builder()
                .id(UUID.randomUUID())
                .ipAddress(ip)
                .reason(req.reason())
                .createdBy(createdBy)
                .createdAt(Instant.now())
                .expiresAt(req.expiresAt())
                .build();
        ipBlocks.save(block);
        return IpBlockDto.from(block);
    }

    @Transactional
    public void unblockIp(UUID id) {
        if (!ipBlocks.existsById(id)) {
            throw Errors.notFound("IP_BLOCK_NOT_FOUND", "IP block does not exist");
        }
        ipBlocks.deleteById(id);
    }

    @Transactional
    public void unlockUser(UUID userId) {
        User user = users.findById(userId)
                .orElseThrow(() -> Errors.notFound("USER_NOT_FOUND", "User does not exist"));
        user.setStatus(UserStatus.ACTIVE);
        user.setFailedLogins((short) 0);
        users.save(user);
    }
}
