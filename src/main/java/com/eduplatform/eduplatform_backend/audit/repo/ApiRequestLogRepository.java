package com.eduplatform.eduplatform_backend.audit.repo;

import com.eduplatform.eduplatform_backend.audit.domain.ApiRequestLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ApiRequestLogRepository extends JpaRepository<ApiRequestLog, UUID> {

    /**
     * Paged, newest-first listing with optional {@code status} / {@code method}
     * filters. A {@code null} filter skips that predicate.
     */
    @Query("""
           select l from ApiRequestLog l
           where (:status is null or l.statusCode = :status)
             and (:method is null or l.method = :method)
           order by l.occurredAt desc
           """)
    Page<ApiRequestLog> search(@Param("status") Integer status,
                               @Param("method") String method,
                               Pageable pageable);
}
