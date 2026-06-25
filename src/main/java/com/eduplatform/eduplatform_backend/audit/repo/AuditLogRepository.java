package com.eduplatform.eduplatform_backend.audit.repo;

import com.eduplatform.eduplatform_backend.audit.domain.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    Page<AuditLog> findAllByActorIdOrderByOccurredAtDesc(UUID actorId, Pageable pageable);

    Page<AuditLog> findAllByEntityTypeAndEntityIdOrderByOccurredAtDesc(String entityType, UUID entityId, Pageable pageable);

    Page<AuditLog> findAllByActionOrderByOccurredAtDesc(String action, Pageable pageable);

    /**
     * Paged, newest-first listing with optional {@code action} / {@code entityType} /
     * {@code actorId} filters. A {@code null} filter skips that predicate.
     */
    @Query("""
           select l from AuditLog l
           where (:action is null or l.action = :action)
             and (:entityType is null or l.entityType = :entityType)
             and (:actorId is null or l.actorId = :actorId)
           order by l.occurredAt desc
           """)
    Page<AuditLog> search(@Param("action") String action,
                          @Param("entityType") String entityType,
                          @Param("actorId") UUID actorId,
                          Pageable pageable);
}
