package com.eduplatform.eduplatform_backend.audit.repo;

import com.eduplatform.eduplatform_backend.audit.domain.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    Page<AuditLog> findAllByActorIdOrderByOccurredAtDesc(UUID actorId, Pageable pageable);

    Page<AuditLog> findAllByEntityTypeAndEntityIdOrderByOccurredAtDesc(String entityType, UUID entityId, Pageable pageable);

    Page<AuditLog> findAllByActionOrderByOccurredAtDesc(String action, Pageable pageable);
}
