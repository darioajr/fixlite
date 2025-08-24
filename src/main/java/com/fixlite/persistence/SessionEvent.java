package com.fixlite.persistence;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

/**
 * Represents a FIX session event for Kafka persistence.
 * Immutable event object for event sourcing.
 */
public class SessionEvent {
    
    public enum EventType {
        SESSION_CREATED,
        LOGON_RECEIVED,
        LOGON_SENT,
        LOGOUT_RECEIVED,
        LOGOUT_SENT,
        HEARTBEAT_SENT,
        HEARTBEAT_RECEIVED,
        TEST_REQUEST_SENT,
        TEST_REQUEST_RECEIVED,
        SEQUENCE_RESET,
        SESSION_DISCONNECTED,
        SESSION_RECOVERED
    }
    
    private final String sessionId;
    private final EventType eventType;
    private final long timestamp;
    private final int sequenceNumber;
    private final String messageData;
    private final String gatewayId;
    private final String metadata;
    
    @JsonCreator
    public SessionEvent(
            @JsonProperty("sessionId") String sessionId,
            @JsonProperty("eventType") EventType eventType,
            @JsonProperty("timestamp") long timestamp,
            @JsonProperty("sequenceNumber") int sequenceNumber,
            @JsonProperty("messageData") String messageData,
            @JsonProperty("gatewayId") String gatewayId,
            @JsonProperty("metadata") String metadata) {
        this.sessionId = sessionId;
        this.eventType = eventType;
        this.timestamp = timestamp;
        this.sequenceNumber = sequenceNumber;
        this.messageData = messageData;
        this.gatewayId = gatewayId;
        this.metadata = metadata;
    }
    
    public static SessionEvent create(String sessionId, EventType eventType, int sequenceNumber, 
                                    String messageData, String gatewayId) {
        return new SessionEvent(sessionId, eventType, Instant.now().toEpochMilli(), 
                              sequenceNumber, messageData, gatewayId, null);
    }
    
    public static SessionEvent create(String sessionId, EventType eventType, int sequenceNumber, 
                                    String messageData, String gatewayId, String metadata) {
        return new SessionEvent(sessionId, eventType, Instant.now().toEpochMilli(), 
                              sequenceNumber, messageData, gatewayId, metadata);
    }
    
    public String getSessionId() {
        return sessionId;
    }
    
    public EventType getEventType() {
        return eventType;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public int getSequenceNumber() {
        return sequenceNumber;
    }
    
    public String getMessageData() {
        return messageData;
    }
    
    public String getGatewayId() {
        return gatewayId;
    }
    
    public String getMetadata() {
        return metadata;
    }
    
    @Override
    public String toString() {
        return "SessionEvent{" +
                "sessionId='" + sessionId + '\'' +
                ", eventType=" + eventType +
                ", timestamp=" + timestamp +
                ", sequenceNumber=" + sequenceNumber +
                ", gatewayId='" + gatewayId + '\'' +
                '}';
    }
}
