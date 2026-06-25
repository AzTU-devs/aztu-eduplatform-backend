package com.eduplatform.eduplatform_backend.analytics.service;

import com.eduplatform.eduplatform_backend.analytics.web.dto.AnalyticsOverviewDto;
import com.eduplatform.eduplatform_backend.common.enums.BookingStatus;
import com.eduplatform.eduplatform_backend.common.enums.CourseStatus;
import com.eduplatform.eduplatform_backend.common.enums.OrderStatus;
import com.eduplatform.eduplatform_backend.common.enums.TutorApprovalStatus;
import com.eduplatform.eduplatform_backend.common.enums.UserStatus;
import com.eduplatform.eduplatform_backend.course.repo.CourseRepository;
import com.eduplatform.eduplatform_backend.enrollment.repo.EnrollmentRepository;
import com.eduplatform.eduplatform_backend.identity.repo.UserRepository;
import com.eduplatform.eduplatform_backend.payment.repo.OrderRepository;
import com.eduplatform.eduplatform_backend.room.repo.RoomBookingRepository;
import com.eduplatform.eduplatform_backend.room.repo.RoomRepository;
import com.eduplatform.eduplatform_backend.tutor.repo.TutorProfileRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Read-only aggregation service backing the admin analytics overview.
 * Uses existing repositories' {@code count()} for simple totals and
 * {@link EntityManager} JPQL aggregate queries for filtered counts/sums.
 */
@Service
public class AnalyticsService {

    private final EntityManager em;

    private final UserRepository userRepository;
    private final TutorProfileRepository tutorProfileRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final RoomRepository roomRepository;
    private final OrderRepository orderRepository;

    public AnalyticsService(EntityManager em,
                            UserRepository userRepository,
                            TutorProfileRepository tutorProfileRepository,
                            CourseRepository courseRepository,
                            EnrollmentRepository enrollmentRepository,
                            RoomRepository roomRepository,
                            OrderRepository orderRepository) {
        this.em = em;
        this.userRepository = userRepository;
        this.tutorProfileRepository = tutorProfileRepository;
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.roomRepository = roomRepository;
        this.orderRepository = orderRepository;
    }

    @Transactional(readOnly = true)
    public AnalyticsOverviewDto overview() {
        long totalUsers = userRepository.count();
        long activeUsers = countUsersByStatus(UserStatus.ACTIVE);
        long lockedUsers = countUsersByStatus(UserStatus.LOCKED);

        long totalTutors = tutorProfileRepository.count();
        long pendingTutors = countTutorsByStatus(TutorApprovalStatus.PENDING);
        long approvedTutors = countTutorsByStatus(TutorApprovalStatus.APPROVED);

        long totalCourses = courseRepository.count();
        long publishedCourses = countCoursesByStatus(CourseStatus.PUBLISHED);
        long pendingCourses = countCoursesByStatus(CourseStatus.IN_REVIEW);
        long draftCourses = countCoursesByStatus(CourseStatus.DRAFT);

        long totalEnrollments = enrollmentRepository.count();

        long totalRooms = roomRepository.count();
        long pendingRoomBookings = countRoomBookingsByStatus(BookingStatus.PENDING);

        long totalOrders = orderRepository.count();
        long paidOrders = countOrdersByStatus(OrderStatus.PAID);
        BigDecimal revenue = revenueForStatus(OrderStatus.PAID);

        Map<String, Long> coursesByStatus = new LinkedHashMap<>();
        for (CourseStatus status : CourseStatus.values()) {
            coursesByStatus.put(status.name(), countCoursesByStatus(status));
        }

        Map<String, Long> enrollmentsLast7Days = enrollmentsLast7Days();

        return new AnalyticsOverviewDto(
                totalUsers, activeUsers, lockedUsers,
                totalTutors, pendingTutors, approvedTutors,
                totalCourses, publishedCourses, pendingCourses, draftCourses,
                totalEnrollments,
                totalRooms, pendingRoomBookings,
                totalOrders, paidOrders, revenue,
                coursesByStatus, enrollmentsLast7Days);
    }

    private long countUsersByStatus(UserStatus status) {
        return em.createQuery("select count(u) from User u where u.status = :s", Long.class)
                .setParameter("s", status)
                .getSingleResult();
    }

    private long countTutorsByStatus(TutorApprovalStatus status) {
        return em.createQuery("select count(t) from TutorProfile t where t.approvalStatus = :s", Long.class)
                .setParameter("s", status)
                .getSingleResult();
    }

    private long countCoursesByStatus(CourseStatus status) {
        return em.createQuery("select count(c) from Course c where c.status = :s", Long.class)
                .setParameter("s", status)
                .getSingleResult();
    }

    private long countRoomBookingsByStatus(BookingStatus status) {
        return em.createQuery("select count(b) from RoomBooking b where b.status = :s", Long.class)
                .setParameter("s", status)
                .getSingleResult();
    }

    private long countOrdersByStatus(OrderStatus status) {
        return em.createQuery("select count(o) from Order o where o.status = :s", Long.class)
                .setParameter("s", status)
                .getSingleResult();
    }

    private BigDecimal revenueForStatus(OrderStatus status) {
        return em.createQuery(
                        "select coalesce(sum(o.total), 0) from Order o where o.status = :s", BigDecimal.class)
                .setParameter("s", status)
                .getSingleResult();
    }

    /**
     * Daily enrollment counts for the last 7 days (including today), keyed by ISO date string.
     * Computed as a single grouped aggregate then back-filled so every day is present (zeros included).
     */
    private Map<String, Long> enrollmentsLast7Days() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        LocalDate startDate = today.minusDays(6);
        Instant since = startDate.atStartOfDay(ZoneOffset.UTC).toInstant();

        // Seed every day in range with 0 so the series is dense.
        Map<String, Long> series = new LinkedHashMap<>();
        for (int i = 0; i < 7; i++) {
            series.put(startDate.plusDays(i).toString(), 0L);
        }

        var rows = em.createQuery(
                        "select e.enrolledAt from Enrollment e where e.enrolledAt >= :since", Instant.class)
                .setParameter("since", since)
                .getResultList();

        for (Instant ts : rows) {
            if (ts == null) {
                continue;
            }
            String day = ts.atZone(ZoneOffset.UTC).toLocalDate().toString();
            series.computeIfPresent(day, (k, v) -> v + 1);
        }

        // Defensive: ignore anything that slipped outside the window.
        series.keySet().removeIf(day ->
                ChronoUnit.DAYS.between(LocalDate.parse(day), today) > 6
                        || LocalDate.parse(day).isAfter(today));

        return series;
    }
}
