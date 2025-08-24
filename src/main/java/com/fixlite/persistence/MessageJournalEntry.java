package com.fixlite.persistence;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

/**
 * Represents a FIX message journal entry for Kafka persistence.
 * Used for message replay and audit trail.
 */
public class MessageJournalEntry {
    
    private final String sessionId;
    private final String messageData;
    private final String messageType;
    private final Integer sequenceNumber;
    private final KafkaSessionPersistence.MessageDirection direction;
    private final String gatewayId;
    private final long timestamp;
    
    @JsonCreator
    public MessageJournalEntry(
            @JsonProperty("sessionId") String sessionId,
            @JsonProperty("messageData") String messageData,
            @JsonProperty("messageType") String messageType,
            @JsonProperty("sequenceNumber") Integer sequenceNumber,
            @JsonProperty("direction") KafkaSessionPersistence.MessageDirection direction,
            @JsonProperty("gatewayId") String gatewayId,
            @JsonProperty("timestamp") long timestamp) {
        this.sessionId = sessionId;
        this.messageData = messageData;
        this.messageType = messageType;
        this.sequenceNumber = sequenceNumber;
        this.direction = direction;
        this.gatewayId = gatewayId;
        this.timestamp = timestamp;
    }
    
    public MessageJournalEntry(String sessionId, String messageData, String messageType,
                             Integer sequenceNumber, KafkaSessionPersistence.MessageDirection direction,
                             String gatewayId) {
        this(sessionId, messageData, messageType, sequenceNumber, direction, gatewayId, 
             Instant.now().toEpochMilli());
    }
    
    public String getSessionId() {
        return sessionId;
    }
    
    public String getMessageData() {
        return messageData;
    }
    
    public String getMessageType() {
        return messageType;
    }
    
    public Integer getSequenceNumber() {
        return sequenceNumber;
    }
    
    public KafkaSessionPersistence.MessageDirection getDirection() {
        return direction;
    }
    
    public String getGatewayId() {
        return gatewayId;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    @Override
    public String toString() {
        return "MessageJournalEntry{" +
                "sessionId='" + sessionId + '\'' +
                ", messageType='" + messageType + '\'' +
                ", sequenceNumber=" + sequenceNumber +
                ", direction=" + direction +
                ", gatewayId='" + gatewayId + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
