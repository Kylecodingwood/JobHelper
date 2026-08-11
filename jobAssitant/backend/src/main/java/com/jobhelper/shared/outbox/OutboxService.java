package com.jobhelper.shared.outbox;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OutboxService {
    private final OutboxRepository outboxRepository;

    public OutboxService(OutboxRepository outboxRepository) {
        this.outboxRepository = outboxRepository;
    }

    @Transactional
    public UUID append(String sourceDomain, String eventType, String aggregateType, UUID aggregateId, String payload) {
        OutboxEventEntity e = new OutboxEventEntity();
        e.setEventId(UUID.randomUUID());
        e.setSourceDomain(sourceDomain);
        e.setEventType(eventType);
        e.setAggregateType(aggregateType);
        e.setAggregateId(aggregateId);
        e.setPayload(payload == null ? "{}" : payload);
        e.setCreatedAt(Instant.now());
        outboxRepository.save(e);
        return e.getEventId();
    }
}
