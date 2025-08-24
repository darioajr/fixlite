package com.fixlite.session;

import com.fixlite.protocol.FixFields;
import com.fixlite.protocol.FixMessage;
import com.fixlite.protocol.FixMessageTypes;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Represents a FIX session with sequence number management and state tracking.
 * Optimized for high-performance trading systems.
 */
public class FixSession {
    
    private static final Logger logger = LoggerFactory.getLogger(FixSession.class);
    private static final DateTimeFormatter SENDING_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd-HH:mm:ss.SSS");
    
    private final String sessionId;
    private final String senderCompId;
    private final String targetCompId;
    private final Channel channel;
    
    private final AtomicInteger outgoingSeqNum = new AtomicInteger(1);
    private final AtomicInteger expectedIncomingSeqNum = new AtomicInteger(1);
    private final AtomicReference<SessionState> state = new AtomicReference<>(SessionState.DISCONNECTED);
    
    private volatile int heartbeatInterval = 30; // seconds
    private volatile long lastHeartbeatTime = System.currentTimeMillis();
    private volatile long lastReceivedTime = System.currentTimeMillis();
    
    public enum SessionState {
        DISCONNECTED,
        CONNECTING,
        LOGON_SENT,
        LOGGED_ON,
        LOGOUT_SENT,
        DISCONNECTING
    }
    
    public FixSession(String senderCompId, String targetCompId, Channel channel) {
        this.senderCompId = senderCompId;
        this.targetCompId = targetCompId;
        this.channel = channel;
        this.sessionId = senderCompId + "->" + targetCompId;
    }
    
    /**
     * Get session identifier
     */
    public String getSessionId() {
        return sessionId;
    }
    
    /**
     * Get sender comp ID
     */
    public String getSenderCompId() {
        return senderCompId;
    }
    
    /**
     * Get target comp ID
     */
    public String getTargetCompId() {
        return targetCompId;
    }
    
    /**
     * Get current session state
     */
    public SessionState getState() {
        return state.get();
    }
    
    /**
     * Set session state
     */
    public void setState(SessionState newState) {
        SessionState oldState = state.getAndSet(newState);
        if (oldState != newState) {
            logger.info("Session {} state changed: {} -> {}", sessionId, oldState, newState);
        }
    }
    
    /**
     * Get next outgoing sequence number
     */
    public int getNextOutgoingSeqNum() {
        return outgoingSeqNum.getAndIncrement();
    }
    
    /**
     * Get expected incoming sequence number
     */
    public int getExpectedIncomingSeqNum() {
        return expectedIncomingSeqNum.get();
    }
    
    /**
     * Validate and update incoming sequence number
     */
    public boolean validateIncomingSeqNum(int receivedSeqNum) {
        int expected = expectedIncomingSeqNum.get();
        
        if (receivedSeqNum == expected) {
            expectedIncomingSeqNum.incrementAndGet();
            lastReceivedTime = System.currentTimeMillis();
            return true;
        } else if (receivedSeqNum < expected) {
            logger.warn("Session {}: Received duplicate message, expected {}, got {}", 
                       sessionId, expected, receivedSeqNum);
            return false;
        } else {
            logger.error("Session {}: Sequence gap detected, expected {}, got {}", 
                        sessionId, expected, receivedSeqNum);
            // TODO: Handle sequence gap (resend request)
            return false;
        }
    }
    
    /**
     * Create standard header for outgoing messages
     */
    public FixMessage createMessageWithHeader(String messageType) {
        FixMessage message = new FixMessage(messageType);
        
        message.setField(FixFields.BEGIN_STRING, "FIX.4.4")
               .setField(FixFields.SENDER_COMP_ID, senderCompId)
               .setField(FixFields.TARGET_COMP_ID, targetCompId)
               .setField(FixFields.MSG_SEQ_NUM, getNextOutgoingSeqNum())
               .setField(FixFields.SENDING_TIME, getCurrentTimestamp());
        
        return message;
    }
    
    /**
     * Send message through the channel
     */
    public void sendMessage(FixMessage message) {
        if (channel == null || !channel.isActive()) {
            logger.error("Cannot send message: channel is not active");
            return;
        }
        
        String encodedMessage = message.encode();
        logger.debug("Session {}: Sending message: {}", sessionId, encodedMessage);
        
        channel.writeAndFlush(encodedMessage);
        lastHeartbeatTime = System.currentTimeMillis();
    }
    
    /**
     * Send heartbeat message
     */
    public void sendHeartbeat() {
        FixMessage heartbeat = createMessageWithHeader(FixMessageTypes.HEARTBEAT);
        sendMessage(heartbeat);
    }
    
    /**
     * Send test request
     */
    public void sendTestRequest() {
        FixMessage testRequest = createMessageWithHeader(FixMessageTypes.TEST_REQUEST);
        testRequest.setField(FixFields.TEST_REQ_ID, String.valueOf(System.currentTimeMillis()));
        sendMessage(testRequest);
    }
    
    /**
     * Send logon message
     */
    public void sendLogon(int heartbeatInterval, String username, String password) {
        this.heartbeatInterval = heartbeatInterval;
        
        FixMessage logon = createMessageWithHeader(FixMessageTypes.LOGON);
        logon.setField(FixFields.ENCRYPT_METHOD, "0") // No encryption
             .setField(FixFields.HEART_BT_INT, heartbeatInterval);
        
        if (username != null && !username.isEmpty()) {
            logon.setField(FixFields.USERNAME, username);
        }
        
        if (password != null && !password.isEmpty()) {
            logon.setField(FixFields.PASSWORD, password);
        }
        
        setState(SessionState.LOGON_SENT);
        sendMessage(logon);
    }
    
    /**
     * Send logout message
     */
    public void sendLogout(String reason) {
        FixMessage logout = createMessageWithHeader(FixMessageTypes.LOGOUT);
        
        if (reason != null && !reason.isEmpty()) {
            logout.setField(FixFields.TEXT, reason);
        }
        
        setState(SessionState.LOGOUT_SENT);
        sendMessage(logout);
    }
    
    /**
     * Check if heartbeat is needed
     */
    public boolean isHeartbeatNeeded() {
        long now = System.currentTimeMillis();
        return (now - lastHeartbeatTime) > (heartbeatInterval * 1000L);
    }
    
    /**
     * Check if test request is needed (no messages received)
     */
    public boolean isTestRequestNeeded() {
        long now = System.currentTimeMillis();
        return (now - lastReceivedTime) > (heartbeatInterval * 1000L * 1.2); // 20% tolerance
    }
    
    /**
     * Get current timestamp in FIX format
     */
    private String getCurrentTimestamp() {
        return LocalDateTime.now().format(SENDING_TIME_FORMAT);
    }
    
    /**
     * Get heartbeat interval
     */
    public int getHeartbeatInterval() {
        return heartbeatInterval;
    }
    
    /**
     * Set heartbeat interval
     */
    public void setHeartbeatInterval(int heartbeatInterval) {
        this.heartbeatInterval = heartbeatInterval;
    }
    
    /**
     * Check if session is active
     */
    public boolean isActive() {
        return channel != null && channel.isActive() && state.get() == SessionState.LOGGED_ON;
    }
    
    @Override
    public String toString() {
        return "FixSession{" +
                "sessionId='" + sessionId + '\'' +
                ", state=" + state.get() +
                ", outgoingSeqNum=" + outgoingSeqNum.get() +
                ", expectedIncomingSeqNum=" + expectedIncomingSeqNum.get() +
                ", heartbeatInterval=" + heartbeatInterval +
                '}';
    }
}
