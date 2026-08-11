package com.jobhelper.shared.outbox;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.jobhelper.job.infrastructure.CanonicalJobEntity;
import com.jobhelper.job.infrastructure.CanonicalJobRepository;
import com.jobhelper.shared.pipeline.ActionProjector;

@Component
public class OutboxPublisher {
    public static final String CONSUMER = "ActionProjector";

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);

    private final OutboxRepository outboxRepository;
    private final DomainEventReceiptRepository receiptRepository;
    private final ActionProjector actionProjector;
    private final CanonicalJobRepository jobRepository;
    private final ObjectMapper objectMapper;

    public OutboxPublisher(
            OutboxRepository outboxRepository,
            DomainEventReceiptRepository receiptRepository,
            ActionProjector actionProjector,
            CanonicalJobRepository jobRepository,
            ObjectMapper objectMapper) {
        this.outboxRepository = outboxRepository;
        this.receiptRepository = receiptRepository;
        this.actionProjector = actionProjector;
        this.jobRepository = jobRepository;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelayString = "${jobhelper.outbox.poll-ms:5000}")
    @Transactional
    public void publishPending() {
        List<OutboxEventEntity> pending = outboxRepository.findUnpublished();
        for (OutboxEventEntity event : pending) {
            try {
                consumeOnce(event);
                event.setPublishedAt(Instant.now());
                outboxRepository.save(event);
            } catch (Exception ex) {
                log.warn("Outbox publish failed eventId={} type={}: {}",
                        event.getEventId(), event.getEventType(), ex.getMessage());
            }
        }
    }

    private void consumeOnce(OutboxEventEntity event) {
        if (receiptRepository.existsByConsumerNameAndEventId(CONSUMER, event.getEventId())) {
            return;
        }
        dispatch(event);
        DomainEventReceiptEntity receipt = new DomainEventReceiptEntity();
        receipt.setReceiptId(UUID.randomUUID());
        receipt.setConsumerName(CONSUMER);
        receipt.setEventId(event.getEventId());
        receipt.setProcessedAt(Instant.now());
        receiptRepository.save(receipt);
    }

    private void dispatch(OutboxEventEntity event) {
        String type = event.getEventType();
        if ("JOB_CREATED".equals(type)) {
            CanonicalJobEntity job = jobRepository.findById(event.getAggregateId()).orElse(null);
            if (job != null) {
                actionProjector.onJobCreated(job);
            }
            return;
        }
        if ("ROADMAP_TASK_ACTIONABLE".equals(type)) {
            JsonNode payload = readPayload(event.getPayload());
            String title = text(payload, "title", "Roadmap task");
            Instant dueAt = instant(payload, "dueAt");
            String roadmapId = text(payload, "roadmapId", event.getAggregateId().toString());
            UUID taskId = uuid(payload, "taskId", event.getAggregateId());
            actionProjector.onRoadmapTaskActionable(taskId, title, dueAt, roadmapId);
            return;
        }
        if ("PROFILE_READY".equals(type)) {
            actionProjector.onProfileReady();
        }
    }

    private JsonNode readPayload(String payload) {
        try {
            return objectMapper.readTree(payload == null ? "{}" : payload);
        } catch (Exception e) {
            try {
                return objectMapper.readTree("{}");
            } catch (Exception ignored) {
                return null;
            }
        }
    }

    private String text(JsonNode n, String field, String fallback) {
        if (n == null || n.get(field) == null || n.get(field).isNull()) {
            return fallback;
        }
        String v = n.get(field).asText();
        return v == null || v.isBlank() ? fallback : v;
    }

    private Instant instant(JsonNode n, String field) {
        if (n == null || n.get(field) == null || n.get(field).isNull()) {
            return null;
        }
        String v = n.get(field).asText();
        if (v == null || v.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(v);
        } catch (Exception e) {
            return null;
        }
    }

    private UUID uuid(JsonNode n, String field, UUID fallback) {
        if (n == null || n.get(field) == null || n.get(field).isNull()) {
            return fallback;
        }
        try {
            return UUID.fromString(n.get(field).asText());
        } catch (Exception e) {
            return fallback;
        }
    }
}
