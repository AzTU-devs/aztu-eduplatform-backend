package com.eduplatform.eduplatform_backend.tutor.service;

import com.eduplatform.eduplatform_backend.catalog.domain.Category;
import com.eduplatform.eduplatform_backend.catalog.repo.CategoryRepository;
import com.eduplatform.eduplatform_backend.common.enums.ApprovalStatus;
import com.eduplatform.eduplatform_backend.common.enums.BookingDecision;
import com.eduplatform.eduplatform_backend.common.enums.RoleCode;
import com.eduplatform.eduplatform_backend.common.enums.TutorApprovalStatus;
import com.eduplatform.eduplatform_backend.common.error.Errors;
import com.eduplatform.eduplatform_backend.identity.domain.Role;
import com.eduplatform.eduplatform_backend.identity.domain.User;
import com.eduplatform.eduplatform_backend.identity.domain.UserRole;
import com.eduplatform.eduplatform_backend.identity.domain.UserRoleId;
import com.eduplatform.eduplatform_backend.identity.repo.RoleRepository;
import com.eduplatform.eduplatform_backend.identity.repo.UserRepository;
import com.eduplatform.eduplatform_backend.tutor.domain.TutorApprovalRequest;
import com.eduplatform.eduplatform_backend.tutor.domain.TutorProfile;
import com.eduplatform.eduplatform_backend.tutor.repo.TutorApprovalRequestRepository;
import com.eduplatform.eduplatform_backend.tutor.repo.TutorProfileRepository;
import com.eduplatform.eduplatform_backend.tutor.web.dto.ApprovalDecisionRequest;
import com.eduplatform.eduplatform_backend.tutor.web.dto.TutorApplyRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Service
public class TutorService {

    private final TutorProfileRepository profiles;
    private final TutorApprovalRequestRepository approvals;
    private final UserRepository users;
    private final RoleRepository roles;
    private final CategoryRepository categories;

    public TutorService(TutorProfileRepository profiles, TutorApprovalRequestRepository approvals,
                        UserRepository users, RoleRepository roles, CategoryRepository categories) {
        this.profiles = profiles;
        this.approvals = approvals;
        this.users = users;
        this.roles = roles;
        this.categories = categories;
    }

    @Transactional
    public TutorProfile apply(UUID userId, TutorApplyRequest req) {
        if (profiles.existsByUserId(userId)) {
            throw Errors.conflict("TUTOR_PROFILE_EXISTS", "You already have a tutor profile; submit a new approval request instead");
        }
        User user = users.findById(userId)
                .orElseThrow(() -> Errors.notFound("USER_NOT_FOUND", "User does not exist"));

        Set<Category> expertise = new HashSet<>();
        for (UUID catId : req.categoryIds()) {
            expertise.add(categories.findById(catId)
                    .orElseThrow(() -> Errors.badRequest("INVALID_CATEGORY", "Unknown category: " + catId)));
        }

        TutorProfile profile = TutorProfile.builder()
                .user(user)
                .headline(req.headline())
                .bio(req.bio())
                .yearsExperience(req.yearsExperience())
                .websiteUrl(req.websiteUrl())
                .linkedinUrl(req.linkedinUrl())
                .approvalStatus(TutorApprovalStatus.PENDING)
                .expertises(expertise)
                .build();
        profile.setId(UUID.randomUUID());
        profile = profiles.save(profile);

        TutorApprovalRequest reqRow = TutorApprovalRequest.builder()
                .tutor(profile)
                .status(ApprovalStatus.PENDING)
                .submittedAt(Instant.now())
                .build();
        reqRow.setId(UUID.randomUUID());
        approvals.save(reqRow);

        return profile;
    }

    @Transactional(readOnly = true)
    public TutorProfile myProfile(UUID userId) {
        TutorProfile p = profiles.findByUserId(userId)
                .orElseThrow(() -> Errors.notFound("TUTOR_PROFILE_NOT_FOUND",
                        "You have not applied to become a tutor yet"));
        initForMapping(p);
        return p;
    }

    /** Public tutor profile: visible only when the profile exists and is APPROVED. */
    @Transactional(readOnly = true)
    public TutorProfile publicProfile(UUID tutorId) {
        TutorProfile p = profiles.findById(tutorId)
                .filter(t -> t.getApprovalStatus() == TutorApprovalStatus.APPROVED)
                .orElseThrow(() -> Errors.notFound("TUTOR_PROFILE_NOT_FOUND", "Tutor does not exist"));
        initForMapping(p);
        return p;
    }

    @Transactional(readOnly = true)
    public Page<TutorProfile> listByStatus(TutorApprovalStatus status, Pageable pageable) {
        Page<TutorProfile> page = profiles.findAllByApprovalStatus(status, pageable);
        page.forEach(TutorService::initForMapping);
        return page;
    }

    /** Touch lazy associations the mapper reads (user display name + expertise ids) before the session closes. */
    private static void initForMapping(TutorProfile p) {
        if (p.getUser() != null) p.getUser().getFirstName();   // init lazy user proxy
        p.getExpertises().size();                              // init lazy ManyToMany
    }

    @Transactional
    public TutorProfile decide(UUID tutorId, UUID adminId, ApprovalDecisionRequest req) {
        TutorProfile tutor = profiles.findById(tutorId)
                .orElseThrow(() -> Errors.notFound("TUTOR_PROFILE_NOT_FOUND", "Tutor does not exist"));

        boolean approved = req.decision() == BookingDecision.APPROVED;
        Instant now = Instant.now();

        tutor.setApprovalStatus(approved ? TutorApprovalStatus.APPROVED : TutorApprovalStatus.REJECTED);
        tutor.setApprovedAt(approved ? now : null);
        tutor.setApprovedBy(approved ? adminId : null);
        tutor.setRejectionReason(approved ? null : req.note());
        profiles.save(tutor);

        TutorApprovalRequest pending = approvals.findAllByTutorIdOrderBySubmittedAtDesc(tutorId).stream()
                .filter(a -> a.getStatus() == ApprovalStatus.PENDING)
                .findFirst()
                .orElseGet(() -> {
                    TutorApprovalRequest r = TutorApprovalRequest.builder()
                            .tutor(tutor)
                            .status(ApprovalStatus.PENDING)
                            .submittedAt(now)
                            .build();
                    r.setId(UUID.randomUUID());
                    return approvals.save(r);
                });
        pending.setStatus(approved ? ApprovalStatus.APPROVED : ApprovalStatus.REJECTED);
        pending.setDecisionNote(req.note());
        pending.setDecidedAt(now);
        // We don't fetch the User entity here to keep the SQL light; the user reference is preserved by FK.
        approvals.save(pending);

        if (approved) {
            grantTutorRole(tutor.getUser());
        }
        initForMapping(tutor);
        return tutor;
    }

    private void grantTutorRole(User user) {
        Role tutorRole = roles.findByCode(RoleCode.TUTOR)
                .orElseThrow(() -> new IllegalStateException("Role TUTOR missing from seed data"));
        boolean already = user.getUserRoles().stream()
                .anyMatch(ur -> ur.getRole().getCode() == RoleCode.TUTOR);
        if (already) return;

        UserRole link = UserRole.builder()
                .id(new UserRoleId(user.getId(), tutorRole.getId()))
                .user(user)
                .role(tutorRole)
                .grantedAt(Instant.now())
                .build();
        user.getUserRoles().add(link);
        users.save(user);
    }
}
