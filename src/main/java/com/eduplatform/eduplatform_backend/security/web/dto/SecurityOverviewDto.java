package com.eduplatform.eduplatform_backend.security.web.dto;

/** High-level security posture for the super-admin console. */
public record SecurityOverviewDto(
        long recentLoginFailures,
        long activeIpBlocks,
        long lockedAccounts
) {}
