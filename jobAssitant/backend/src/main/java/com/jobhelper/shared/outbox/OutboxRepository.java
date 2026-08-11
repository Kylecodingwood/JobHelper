package com.jobhelper.shared.outbox;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface OutboxRepository extends JpaRepository<OutboxEventEntity, UUID> {
    @Query("select e from OutboxEventEntity e where e.publishedAt is null order by e.createdAt asc")
    List<OutboxEventEntity> findUnpublished();
}
