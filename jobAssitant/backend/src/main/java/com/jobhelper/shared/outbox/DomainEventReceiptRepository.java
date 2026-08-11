package com.jobhelper.shared.outbox;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DomainEventReceiptRepository extends JpaRepository<DomainEventReceiptEntity, UUID> {
    Optional<DomainEventReceiptEntity> findByConsumerNameAndEventId(String consumerName, UUID eventId);

    boolean existsByConsumerNameAndEventId(String consumerName, UUID eventId);
}
