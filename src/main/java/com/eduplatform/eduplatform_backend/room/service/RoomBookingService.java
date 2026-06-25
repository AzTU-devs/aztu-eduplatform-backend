package com.eduplatform.eduplatform_backend.room.service;

import com.eduplatform.eduplatform_backend.common.enums.BookingDecision;
import com.eduplatform.eduplatform_backend.common.enums.BookingStatus;
import com.eduplatform.eduplatform_backend.common.error.Errors;
import com.eduplatform.eduplatform_backend.identity.domain.User;
import com.eduplatform.eduplatform_backend.identity.repo.UserRepository;
import com.eduplatform.eduplatform_backend.room.domain.Room;
import com.eduplatform.eduplatform_backend.room.domain.RoomBooking;
import com.eduplatform.eduplatform_backend.room.domain.RoomBookingApproval;
import com.eduplatform.eduplatform_backend.room.repo.RoomBookingApprovalRepository;
import com.eduplatform.eduplatform_backend.room.repo.RoomBookingRepository;
import com.eduplatform.eduplatform_backend.room.web.dto.BookingCreateRequest;
import com.eduplatform.eduplatform_backend.tutor.domain.TutorProfile;
import com.eduplatform.eduplatform_backend.tutor.repo.TutorProfileRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
public class RoomBookingService {

    private final RoomBookingRepository bookings;
    private final RoomBookingApprovalRepository approvals;
    private final RoomService rooms;
    private final TutorProfileRepository tutors;
    private final UserRepository users;

    public RoomBookingService(RoomBookingRepository bookings, RoomBookingApprovalRepository approvals,
                              RoomService rooms, TutorProfileRepository tutors, UserRepository users) {
        this.bookings = bookings;
        this.approvals = approvals;
        this.rooms = rooms;
        this.tutors = tutors;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public Page<RoomBooking> listByStatus(BookingStatus status, Pageable pageable) {
        return bookings.findAllByStatus(status, pageable);
    }

    /** Bookings owned by the calling tutor, optionally filtered by status. */
    @Transactional(readOnly = true)
    public Page<RoomBooking> listMine(UUID userId, BookingStatus status, Pageable pageable) {
        TutorProfile tutor = tutors.findByUserId(userId)
                .orElseThrow(() -> Errors.forbidden("NOT_A_TUTOR", "Only tutors have room bookings"));
        Page<RoomBooking> page = (status == null)
                ? bookings.findAllByTutorId(tutor.getId(), pageable)
                : bookings.findAllByTutorIdAndStatus(tutor.getId(), status, pageable);
        // Initialise lazy associations the web mapper reads, before the session closes
        // (open-in-view=false).
        page.forEach(this::touchMappedAssociations);
        return page;
    }

    /** Tutor cancels one of their own bookings. */
    @Transactional
    public RoomBooking cancelOwn(UUID bookingId, UUID userId) {
        TutorProfile tutor = tutors.findByUserId(userId)
                .orElseThrow(() -> Errors.forbidden("NOT_A_TUTOR", "Only tutors can cancel room bookings"));
        RoomBooking booking = bookings.findById(bookingId)
                .orElseThrow(() -> Errors.notFound("BOOKING_NOT_FOUND", "Booking does not exist"));
        if (!booking.getTutor().getId().equals(tutor.getId())) {
            throw Errors.forbidden("NOT_BOOKING_OWNER", "Booking belongs to another tutor");
        }
        if (booking.getStatus() != BookingStatus.PENDING && booking.getStatus() != BookingStatus.APPROVED) {
            throw Errors.unprocessable("BOOKING_NOT_CANCELLABLE",
                    "Only pending or approved bookings can be cancelled");
        }
        // Moving to CANCELLED is safe against the APPROVED-overlap exclusion constraint.
        booking.setStatus(BookingStatus.CANCELLED);
        bookings.save(booking);
        touchMappedAssociations(booking);
        return booking;
    }

    /** Force-initialise the lazy associations the {@code RoomMapper} dereferences. */
    private void touchMappedAssociations(RoomBooking booking) {
        booking.getRoom().getName();
        if (booking.getOfflineCourse() != null) {
            booking.getOfflineCourse().getCourseId();
        }
        booking.getTutor().getId();
    }

    @Transactional
    public RoomBooking requestBooking(UUID userId, BookingCreateRequest req) {
        TutorProfile tutor = tutors.findByUserId(userId)
                .orElseThrow(() -> Errors.forbidden("NOT_A_TUTOR", "Only tutors can request room bookings"));
        Room room = rooms.get(req.roomId());

        if (!req.endsAt().isAfter(req.startsAt())) {
            throw Errors.badRequest("INVALID_RANGE", "endsAt must be after startsAt");
        }
        if (!bookings.findOverlappingApproved(room.getId(), req.startsAt(), req.endsAt()).isEmpty()) {
            throw Errors.conflict("ROOM_TIME_TAKEN", "Requested time overlaps an existing approved booking");
        }

        double hours = Duration.between(req.startsAt(), req.endsAt()).toMinutes() / 60.0;
        java.math.BigDecimal fee = room.getHourlyRate().multiply(java.math.BigDecimal.valueOf(hours))
                .setScale(2, java.math.RoundingMode.HALF_UP);

        RoomBooking booking = RoomBooking.builder()
                .room(room)
                .tutor(tutor)
                .startsAt(req.startsAt())
                .endsAt(req.endsAt())
                .recurrenceRule(req.recurrenceRule())
                .status(BookingStatus.PENDING)
                .totalFee(fee)
                .currency(room.getCurrency())
                .build();
        booking.setId(UUID.randomUUID());
        return bookings.save(booking);
    }

    @Transactional
    public RoomBooking decide(UUID bookingId, UUID adminUserId, BookingDecision decision, String note) {
        RoomBooking booking = bookings.findById(bookingId)
                .orElseThrow(() -> Errors.notFound("BOOKING_NOT_FOUND", "Booking does not exist"));
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw Errors.conflict("INVALID_BOOKING_TRANSITION",
                    "Booking is not pending and cannot receive a decision");
        }
        User admin = users.findById(adminUserId)
                .orElseThrow(() -> new IllegalStateException("Admin user vanished mid-request"));

        booking.setStatus(decision == BookingDecision.APPROVED ? BookingStatus.APPROVED : BookingStatus.REJECTED);
        bookings.save(booking);

        RoomBookingApproval row = RoomBookingApproval.builder()
                .booking(booking)
                .decision(decision)
                .decisionNote(note)
                .decidedBy(admin)
                .decidedAt(Instant.now())
                .build();
        row.setId(UUID.randomUUID());
        approvals.save(row);
        return booking;
    }
}
