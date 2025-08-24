package com.fixlite.gateway;

import com.fixlite.session.FixSession;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Manages FIX sessions and handles heartbeat monitoring.
 * Thread-safe implementation for high-performance trading systems.
 */
public class FixSessionManager {
    
    private static final Logger logger = LoggerFactory.getLogger(FixSessionManager.class);
    
    private final ConcurrentHashMap<String, FixSession> sessions = new ConcurrentHashMap<>();
    private final ScheduledExecutorService heartbeatScheduler = Executors.newScheduledThreadPool(2);
    
    public FixSessionManager() {
        // Start heartbeat monitoring
        startHeartbeatMonitoring();
    }
    
    /**
     * Get or create a FIX session
     */
    public FixSession getOrCreateSession(String senderCompId, String targetCompId, Channel channel) {
        String sessionId = senderCompId + "->" + targetCompId;
        
        return sessions.computeIfAbsent(sessionId, id -> {
            FixSession session = new FixSession(senderCompId, targetCompId, channel);
            logger.info("Created new FIX session: {}", sessionId);
            return session;
        });
    }
    
    /**
     * Get existing session by ID
     */
    public FixSession getSession(String sessionId) {
        return sessions.get(sessionId);
    }
    
    /**
     * Remove session
     */
    public void removeSession(String sessionId) {
        FixSession removed = sessions.remove(sessionId);
        if (removed != null) {
            logger.info("Removed FIX session: {}", sessionId);
        }
    }
    
    /**
     * Get all active sessions
     */
    public java.util.Collection<FixSession> getAllSessions() {
        return sessions.values();
    }
    
    /**
     * Get session count
     */
    public int getSessionCount() {
        return sessions.size();
    }
    
    /**
     * Start heartbeat monitoring for all sessions
     */
    private void startHeartbeatMonitoring() {
        // Heartbeat sender - sends heartbeats when needed
        heartbeatScheduler.scheduleAtFixedRate(() -> {
            try {
                for (FixSession session : sessions.values()) {
                    if (session.isActive() && session.isHeartbeatNeeded()) {
                        session.sendHeartbeat();
                        logger.debug("Sent heartbeat for session: {}", session.getSessionId());
                    }
                }
            } catch (Exception e) {
                logger.error("Error in heartbeat sender", e);
            }
        }, 5, 5, TimeUnit.SECONDS);
        
        // Test request sender - sends test requests when no messages received
        heartbeatScheduler.scheduleAtFixedRate(() -> {
            try {
                for (FixSession session : sessions.values()) {
                    if (session.isActive() && session.isTestRequestNeeded()) {
                        session.sendTestRequest();
                        logger.warn("Sent test request for session: {}", session.getSessionId());
                    }
                }
            } catch (Exception e) {
                logger.error("Error in test request sender", e);
            }
        }, 10, 10, TimeUnit.SECONDS);
        
        logger.info("Heartbeat monitoring started");
    }
    
    /**
     * Shutdown the session manager
     */
    public void shutdown() {
        logger.info("Shutting down session manager...");
        
        // Send logout to all active sessions
        for (FixSession session : sessions.values()) {
            if (session.isActive()) {
                session.sendLogout("Server shutdown");
            }
        }
        
        sessions.clear();
        heartbeatScheduler.shutdown();
        
        try {
            if (!heartbeatScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                heartbeatScheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            heartbeatScheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        logger.info("Session manager shutdown complete");
    }
}
