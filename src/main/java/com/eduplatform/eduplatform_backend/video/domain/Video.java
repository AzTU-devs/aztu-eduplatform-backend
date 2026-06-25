package com.eduplatform.eduplatform_backend.video.domain;

import com.eduplatform.eduplatform_backend.common.domain.SoftDeletable;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.util.UUID;

@Entity
@Table(name = "videos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE videos SET deleted_at = now() WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class Video extends SoftDeletable {

    @Column(name = "owner_user_id", columnDefinition = "uuid")
    private UUID ownerUserId;

    @Column(name = "title", length = 255)
    private String title;

    @Column(name = "storage", nullable = false, length = 20)
    @Builder.Default
    private String storage = "LOCAL";

    @Column(name = "bucket", length = 255)
    private String bucket;

    @Column(name = "object_key", length = 512)
    private String objectKey;

    @Column(name = "mime_type", length = 120)
    private String mimeType;

    @Column(name = "byte_size", nullable = false)
    @Builder.Default
    private long byteSize = 0L;

    @Column(name = "duration_sec")
    private Integer durationSec;

    /** One of PENDING, UPLOADED, PROCESSING, READY, FAILED (see V8 CHECK constraint). */
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "PENDING";
}
