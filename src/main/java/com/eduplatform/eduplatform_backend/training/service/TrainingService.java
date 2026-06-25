package com.eduplatform.eduplatform_backend.training.service;

import com.eduplatform.eduplatform_backend.common.error.Errors;
import com.eduplatform.eduplatform_backend.training.domain.Training;
import com.eduplatform.eduplatform_backend.training.repo.TrainingRepository;
import com.eduplatform.eduplatform_backend.training.web.dto.TrainingUpsertRequest;
import com.eduplatform.eduplatform_backend.tutor.domain.TutorProfile;
import com.eduplatform.eduplatform_backend.tutor.repo.TutorProfileRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Tutor-owned trainings. Tutor-facing operations are scoped to the caller's
 * {@link TutorProfile}; the public catalog reads PUBLISHED trainings.
 */
@Service
public class TrainingService {

    private final TrainingRepository repo;
    private final TutorProfileRepository tutors;

    public TrainingService(TrainingRepository repo, TutorProfileRepository tutors) {
        this.repo = repo;
        this.tutors = tutors;
    }

    @Transactional(readOnly = true)
    public Page<Training> listMine(UUID userId, Pageable pageable) {
        TutorProfile profile = requireTutor(userId);
        return repo.findAllByTutorIdOrderByCreatedAtDesc(profile.getId(), pageable);
    }

    @Transactional(readOnly = true)
    public Training getMine(UUID id, UUID userId) {
        TutorProfile profile = requireTutor(userId);
        return ownedOrThrow(id, profile);
    }

    @Transactional
    public Training create(TrainingUpsertRequest req, UUID userId) {
        TutorProfile profile = requireTutor(userId);
        Training t = Training.builder()
                .tutor(profile)
                .title(req.title())
                .description(req.description())
                .city(req.city())
                .addressLine(req.addressLine())
                .startDate(req.startDate())
                .endDate(req.endDate())
                .capacity(req.capacity() == null ? 0 : req.capacity())
                .price(req.price() == null ? BigDecimal.ZERO : req.price())
                .currency(req.currency() == null ? "USD" : req.currency())
                .status(req.status() == null ? "DRAFT" : req.status())
                .build();
        t.setId(UUID.randomUUID());
        return repo.save(t);
    }

    @Transactional
    public Training update(UUID id, TrainingUpsertRequest req, UUID userId) {
        TutorProfile profile = requireTutor(userId);
        Training t = ownedOrThrow(id, profile);
        if (req.title() != null) t.setTitle(req.title());
        if (req.description() != null) t.setDescription(req.description());
        if (req.city() != null) t.setCity(req.city());
        if (req.addressLine() != null) t.setAddressLine(req.addressLine());
        if (req.startDate() != null) t.setStartDate(req.startDate());
        if (req.endDate() != null) t.setEndDate(req.endDate());
        if (req.capacity() != null) t.setCapacity(req.capacity());
        if (req.price() != null) t.setPrice(req.price());
        if (req.currency() != null) t.setCurrency(req.currency());
        if (req.status() != null) t.setStatus(req.status());
        return repo.save(t);
    }

    @Transactional
    public void delete(UUID id, UUID userId) {
        TutorProfile profile = requireTutor(userId);
        repo.delete(ownedOrThrow(id, profile));   // @SQLDelete → soft delete
    }

    @Transactional(readOnly = true)
    public Page<Training> listPublic(Pageable pageable) {
        return repo.findAllByStatusOrderByCreatedAtDesc("PUBLISHED", pageable);
    }

    private TutorProfile requireTutor(UUID userId) {
        return tutors.findByUserId(userId)
                .orElseThrow(() -> Errors.forbidden("NOT_A_TUTOR", "Caller has no tutor profile"));
    }

    private Training ownedOrThrow(UUID id, TutorProfile profile) {
        Training t = repo.findById(id)
                .orElseThrow(() -> Errors.notFound("TRAINING_NOT_FOUND", "Training does not exist"));
        UUID ownerId = t.getTutor() == null ? null : t.getTutor().getId();
        if (!profile.getId().equals(ownerId)) {
            throw Errors.forbidden("TRAINING_FORBIDDEN", "You do not own this training");
        }
        return t;
    }
}
