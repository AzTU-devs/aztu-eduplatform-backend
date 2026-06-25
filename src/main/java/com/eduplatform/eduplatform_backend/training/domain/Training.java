package com.eduplatform.eduplatform_backend.training.domain;

import com.eduplatform.eduplatform_backend.common.domain.SoftDeletable;
import com.eduplatform.eduplatform_backend.tutor.domain.TutorProfile;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "trainings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE trainings SET deleted_at = now() WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class Training extends SoftDeletable {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tutor_id", nullable = false)
    private TutorProfile tutor;

    @Column(name = "title", length = 255)
    private String title;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "city", length = 255)
    private String city;

    @Column(name = "address_line", length = 255)
    private String addressLine;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "capacity", nullable = false)
    @Builder.Default
    private int capacity = 0;

    @Column(name = "enrolled_count", nullable = false)
    @Builder.Default
    private int enrolledCount = 0;

    @Column(name = "price", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal price = BigDecimal.ZERO;

    @Column(name = "currency", nullable = false, length = 3)
    @Builder.Default
    private String currency = "USD";

    /** One of DRAFT, PUBLISHED, CANCELLED, COMPLETED (see V8 CHECK constraint). */
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "DRAFT";
}
