package com.eduplatform.eduplatform_backend.analytics.web.dto;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Aggregate platform metrics for the admin analytics dashboard.
 * Counts are computed from existing repositories / JPQL aggregate queries.
 */
public record AnalyticsOverviewDto(
        long totalUsers,
        long activeUsers,
        long lockedUsers,
        long totalTutors,
        long pendingTutors,
        long approvedTutors,
        long totalCourses,
        long publishedCourses,
        long pendingCourses,
        long draftCourses,
        long totalEnrollments,
        long totalRooms,
        long pendingRoomBookings,
        long totalOrders,
        long paidOrders,
        BigDecimal revenue,
        Map<String, Long> coursesByStatus,
        Map<String, Long> enrollmentsLast7Days
) {}
