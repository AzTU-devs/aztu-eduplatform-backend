package com.eduplatform.eduplatform_backend.audit.repo;

import com.eduplatform.eduplatform_backend.audit.domain.SecurityEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SecurityEventRepository extends JpaRepository<SecurityEvent, UUID> {

    Page<SecurityEvent> findAllByUserIdOrderByOccurredAtDesc(UUID userId, Pageable pageable);

    Page<SecurityEvent> findAllByEventTypeOrderByOccurredAtDesc(String eventType, Pageable pageable);
}
