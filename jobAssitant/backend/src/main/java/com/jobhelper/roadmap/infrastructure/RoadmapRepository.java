package com.jobhelper.roadmap.infrastructure;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RoadmapRepository extends JpaRepository<RoadmapEntity, UUID> {
    Optional<RoadmapEntity> findFirstByStatusOrderByUpdatedAtDesc(String status);
}
