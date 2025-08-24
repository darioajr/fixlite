package com.fixlite.persistence;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fixlite.session.FixSession;

import java.time.Instant;

/**
 * Represents the current state of a FIX session for persistence.
 * Used for session recovery and clustering.
 */
public class SessionState {
    
    private final String sessionId;
    private final String senderCompId;
    private final String targetCompId;
    private final FixSession.SessionState state;
    private final int outgoingSeqNum;
    private final int expectedIncomingSeqNum;
    private final int heartbeatInterval;
    private final long lastHeartbeatTime;
    private final long lastReceivedTime;
    private final String gatewayId;
    private final long timestamp;
    
    @JsonCreator
    public SessionState(
            @JsonProperty("sessionId") String sessionId,
            @JsonProperty("senderCompId") String senderCompId,
            @JsonProperty("targetCompId") String targetCompId,
            @JsonProperty("state") FixSession.SessionState state,
            @JsonProperty("outgoingSeqNum") int outgoingSeqNum,
            @JsonProperty("expectedIncomingSeqNum") int expectedIncomingSeqNum,
            @JsonProperty("heartbeatInterval") int heartbeatInterval,
            @JsonProperty("lastHeartbeatTime") long lastHeartbeatTime,
            @JsonProperty("lastReceivedTime") long lastReceivedTime,
            @JsonProperty("gatewayId") String gatewayId,
            @JsonProperty("timestamp") long timestamp) {
        this.sessionId = sessionId;
        this.senderCompId = senderCompId;
        this.targetCompId = targetCompId;
        this.state = state;
        this.outgoingSeqNum = outgoingSeqNum;
        this.expectedIncomingSeqNum = expectedIncomingSeqNum;
        this.heartbeatInterval = heartbeatInterval;
        this.lastHeartbeatTime = lastHeartbeatTime;
        this.lastReceivedTime = lastReceivedTime;
        this.gatewayId = gatewayId;
        this.timestamp = timestamp;
    }
    
    public static SessionState fromFixSession(FixSession session, String gatewayId) {
        return new SessionState(
                session.getSessionId(),
                session.getSenderCompId(),
                session.getTargetCompId(),
                session.getState(),
                session.getNextOutgoingSeqNum() - 1, // Current seq num
                session.getExpectedIncomingSeqNum(),
                session.getHeartbeatInterval(),
                System.currentTimeMillis(), // lastHeartbeatTime - would need to be tracked
                System.currentTimeMillis(), // lastReceivedTime - would need to be tracked
                gatewayId,
                Instant.now().toEpochMilli()
        );
    }
    
    public String getSessionId() {
        return sessionId;
    }
    
    public String getSenderCompId() {
        return senderCompId;
    }
    
    public String getTargetCompId() {
        return targetCompId;
    }
    
    public FixSession.SessionState getState() {
        return state;
    }
    
    public int getOutgoingSeqNum() {
        return outgoingSeqNum;
    }
    
    public int getExpectedIncomingSeqNum() {
        return expectedIncomingSeqNum;
    }
    
    public int getHeartbeatInterval() {
        return heartbeatInterval;
    }
    
    public long getLastHeartbeatTime() {
        return lastHeartbeatTime;
    }
    
    public long getLastReceivedTime() {
        return lastReceivedTime;
    }
    
    public String getGatewayId() {
        return gatewayId;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    @Override
    public String toString() {
        return "SessionState{" +
                "sessionId='" + sessionId + '\'' +
                ", state=" + state +
                ", outgoingSeqNum=" + outgoingSeqNum +
                ", expectedIncomingSeqNum=" + expectedIncomingSeqNum +
                ", gatewayId='" + gatewayId + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
