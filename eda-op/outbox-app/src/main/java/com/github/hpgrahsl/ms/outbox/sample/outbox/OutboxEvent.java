package com.github.hpgrahsl.ms.outbox.sample.outbox;

import org.hibernate.annotations.JdbcTypeCode;

import jakarta.annotation.Nonnull;
import jakarta.persistence.*;

import java.util.UUID;

@Entity
public class OutboxEvent {

    @Id
    @GeneratedValue
    @JdbcTypeCode(java.sql.Types.VARCHAR)
    private UUID id;

    @Nonnull
    private String aggregateType;

    @Nonnull
    private String aggregateId;

    @Nonnull
    private String type;

    @Nonnull
    private Long timestamp;

    @Nonnull
    @Column(length = 1048576) //e.g. 1 MB max
    private String payload;

    private OutboxEvent() {
    }

    public OutboxEvent(String aggregateType, String aggregateId, String type, String payload, Long timestamp) {
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.type = type;
        this.payload = payload;
        this.timestamp = timestamp;
    }

    public static OutboxEvent from(Outboxable event) {
        return new OutboxEvent(
                event.getAggregateType(),
                event.getAggregateId(),
                event.getType(),
                event.getPayload(),
                event.getTimestamp()
        );
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getAggregateId() {
        return aggregateId;
    }

    public void setAggregateId(String aggregateId) {
        this.aggregateId = aggregateId;
    }

    public String getAggregateType() {
        return aggregateType;
    }

    public void setAggregateType(String aggregateType) {
        this.aggregateType = aggregateType;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }
}
