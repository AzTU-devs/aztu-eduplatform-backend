package com.eduplatform.eduplatform_backend.audit.repo;

import com.eduplatform.eduplatform_backend.audit.domain.SecurityEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.UUID;

@Repository
public interface SecurityEventRepository extends JpaRepository<SecurityEvent, UUID> {

    Page<SecurityEvent> findAllByUserIdOrderByOccurredAtDesc(UUID userId, Pageable pageable);

    Page<SecurityEvent> findAllByEventTypeOrderByOccurredAtDesc(String eventType, Pageable pageable);

    Page<SecurityEvent> findAllByOrderByOccurredAtDesc(Pageable pageable);

    /** Count events of a given type since {@code since} (e.g. failed logins in the last 24h). */
    @Query("select count(e) from SecurityEvent e where e.eventType = :type and e.occurredAt >= :since")
    long countByEventTypeSince(@Param("type") String eventType, @Param("since") Instant since);
}
