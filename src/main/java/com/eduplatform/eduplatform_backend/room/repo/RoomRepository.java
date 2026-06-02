package com.eduplatform.eduplatform_backend.room.repo;

import com.eduplatform.eduplatform_backend.common.enums.RoomStatus;
import com.eduplatform.eduplatform_backend.room.domain.Room;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface RoomRepository extends JpaRepository<Room, UUID> {

    Page<Room> findAllByStatus(RoomStatus status, Pageable pageable);

    /**
     * True when an active (non-deleted) room already uses this building + number.
     * Mirrors the partial unique index {@code uq_rooms_building_number}.
     * {@code excludeId} lets update() ignore the row being edited (pass {@code null} on create).
     * The {@code @SQLRestriction} on {@link Room} already scopes this to non-deleted rows.
     */
    @Query("""
           select case when count(r) > 0 then true else false end
           from Room r
           where r.roomNumber = :roomNumber
             and ((:building is null and r.building is null) or r.building = :building)
             and (:excludeId is null or r.id <> :excludeId)
           """)
    boolean existsActiveByBuildingAndRoomNumber(@Param("building") String building,
                                                @Param("roomNumber") String roomNumber,
                                                @Param("excludeId") UUID excludeId);
}
