package com.jobhelper.shared.outbox;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "domain_event_receipt")
public class DomainEventReceiptEntity {
    @Id
    @Column(name = "receipt_id")
    private UUID receiptId;

    @Column(name = "consumer_name", nullable = false)
    private String consumerName;

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    public UUID getReceiptId() { return receiptId; }
    public void setReceiptId(UUID receiptId) { this.receiptId = receiptId; }
    public String getConsumerName() { return consumerName; }
    public void setConsumerName(String consumerName) { this.consumerName = consumerName; }
    public UUID getEventId() { return eventId; }
    public void setEventId(UUID eventId) { this.eventId = eventId; }
    public Instant getProcessedAt() { return processedAt; }
    public void setProcessedAt(Instant processedAt) { this.processedAt = processedAt; }
}
