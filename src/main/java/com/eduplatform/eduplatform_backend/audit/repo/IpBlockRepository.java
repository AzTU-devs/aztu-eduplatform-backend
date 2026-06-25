package com.eduplatform.eduplatform_backend.audit.repo;

import com.eduplatform.eduplatform_backend.audit.domain.IpBlock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface IpBlockRepository extends JpaRepository<IpBlock, UUID> {

    Optional<IpBlock> findByIpAddress(String ipAddress);

    Page<IpBlock> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /** Count blocks that are still in effect (no expiry, or expiry in the future). */
    @Query("select count(b) from IpBlock b where b.expiresAt is null or b.expiresAt > :now")
    long countActive(@Param("now") Instant now);
}
