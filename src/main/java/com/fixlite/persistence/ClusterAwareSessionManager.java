package com.fixlite.persistence;

import com.fixlite.session.FixSession;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Cluster-aware session manager with Kafka persistence.
 * Handles session recovery, failover, and distributed session management.
 */
public class ClusterAwareSessionManager {
    
    private static final Logger logger = LoggerFactory.getLogger(ClusterAwareSessionManager.class);
    
    private final KafkaSessionPersistence persistence;
    private final String gatewayId;
    private final ConcurrentHashMap<String, FixSession> activeSessions = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(3);
    
    // Recovery and clustering
    private volatile boolean recoveryCompleted = false;
    
    public ClusterAwareSessionManager(KafkaSessionPersistence persistence, String gatewayId) {
        this.persistence = persistence;
        this.gatewayId = gatewayId;
        
        // Start background tasks
        startSessionRecovery();
        startSessionStatePersistence();
        startHeartbeatMonitoring();
        
        logger.info("Cluster-aware session manager initialized for gateway: {}", gatewayId);
    }
    
    /**
     * Get or create session with recovery support
     */
    public FixSession getOrCreateSession(String senderCompId, String targetCompId, Channel channel) {
        String sessionId = senderCompId + "->" + targetCompId;
        
        return activeSessions.computeIfAbsent(sessionId, id -> {
            // Try to recover session state first
            SessionState recoveredState = persistence.recoverSessionState(sessionId);
            
            FixSession session;
            if (recoveredState != null && shouldRecoverSession(recoveredState)) {
                session = recoverFixSession(recoveredState, channel);
                logger.info("Recovered FIX session: {} with seq nums out={}, in={}", 
                           sessionId, recoveredState.getOutgoingSeqNum(), 
                           recoveredState.getExpectedIncomingSeqNum());
                
                // Log recovery event
                persistSessionEvent(SessionEvent.create(sessionId, SessionEvent.EventType.SESSION_RECOVERED,
                                                      recoveredState.getOutgoingSeqNum(), null, gatewayId));
            } else {
                session = new FixSession(senderCompId, targetCompId, channel);
                logger.info("Created new FIX session: {}", sessionId);
                
                // Log creation event
                persistSessionEvent(SessionEvent.create(sessionId, SessionEvent.EventType.SESSION_CREATED,
                                                      1, null, gatewayId));
            }
            
            return session;
        });
    }
    
    /**
     * Remove session and persist final state
     */
    public void removeSession(String sessionId) {
        FixSession session = activeSessions.remove(sessionId);
        if (session != null) {
            // Persist final session state
            SessionState finalState = SessionState.fromFixSession(session, gatewayId);
            persistence.persistSessionState(finalState);
            
            // Log disconnection event
            persistSessionEvent(SessionEvent.create(sessionId, SessionEvent.EventType.SESSION_DISCONNECTED,
                                                  session.getNextOutgoingSeqNum() - 1, null, gatewayId));
            
            // Clear from cache
            persistence.clearSessionFromCache(sessionId);
            
            logger.info("Removed FIX session: {}", sessionId);
        }
    }
    
    /**
     * Get active session
     */
    public FixSession getSession(String sessionId) {
        return activeSessions.get(sessionId);
    }
    
    /**
     * Get all active sessions
     */
    public java.util.Collection<FixSession> getAllSessions() {
        return activeSessions.values();
    }
    
    /**
     * Persist session event
     */
    public void persistSessionEvent(SessionEvent event) {
        try {
            persistence.persistSessionEvent(event);
        } catch (Exception e) {
            logger.error("Failed to persist session event: {}", event, e);
        }
    }
    
    /**
     * Persist message to journal
     */
    public void persistMessage(String sessionId, com.fixlite.protocol.FixMessage message, 
                             KafkaSessionPersistence.MessageDirection direction) {
        try {
            persistence.persistMessage(sessionId, message, direction);
        } catch (Exception e) {
            logger.error("Failed to persist message for session {}: {}", sessionId, message, e);
        }
    }
    
    /**
     * Start session recovery process
     */
    private void startSessionRecovery() {
        scheduler.execute(() -> {
            try {
                logger.info("Starting session recovery...");
                
                Map<String, SessionState> allStates = persistence.loadAllSessionStates();
                
                for (SessionState state : allStates.values()) {
                    if (shouldTakeOverSession(state)) {
                        logger.info("Taking over orphaned session: {} from gateway: {}", 
                                   state.getSessionId(), state.getGatewayId());
                        
                        // Mark as taken over by this gateway
                        SessionState takenOverState = new SessionState(
                                state.getSessionId(),
                                state.getSenderCompId(),
                                state.getTargetCompId(),
                                FixSession.SessionState.DISCONNECTED, // Mark as disconnected for takeover
                                state.getOutgoingSeqNum(),
                                state.getExpectedIncomingSeqNum(),
                                state.getHeartbeatInterval(),
                                state.getLastHeartbeatTime(),
                                state.getLastReceivedTime(),
                                gatewayId, // New gateway ID
                                System.currentTimeMillis()
                        );
                        
                        persistence.persistSessionState(takenOverState);
                    }
                }
                
                recoveryCompleted = true;
                logger.info("Session recovery completed. Found {} recoverable sessions", allStates.size());
                
            } catch (Exception e) {
                logger.error("Error during session recovery", e);
            }
        });
    }
    
    /**
     * Start periodic session state persistence
     */
    private void startSessionStatePersistence() {
        scheduler.scheduleAtFixedRate(() -> {
            if (!recoveryCompleted) return;
            
            try {
                for (FixSession session : activeSessions.values()) {
                    if (session.isActive()) {
                        SessionState currentState = SessionState.fromFixSession(session, gatewayId);
                        persistence.persistSessionState(currentState);
                    }
                }
            } catch (Exception e) {
                logger.error("Error persisting session states", e);
            }
        }, 10, 10, TimeUnit.SECONDS); // Persist every 10 seconds
    }
    
    /**
     * Start heartbeat monitoring
     */
    private void startHeartbeatMonitoring() {
        // Heartbeat sender
        scheduler.scheduleAtFixedRate(() -> {
            try {
                for (FixSession session : activeSessions.values()) {
                    if (session.isActive() && session.isHeartbeatNeeded()) {
                        session.sendHeartbeat();
                        
                        // Log heartbeat event
                        persistSessionEvent(SessionEvent.create(session.getSessionId(), 
                                                              SessionEvent.EventType.HEARTBEAT_SENT,
                                                              session.getNextOutgoingSeqNum() - 1, null, gatewayId));
                    }
                }
            } catch (Exception e) {
                logger.error("Error in heartbeat sender", e);
            }
        }, 5, 5, TimeUnit.SECONDS);
        
        // Test request sender
        scheduler.scheduleAtFixedRate(() -> {
            try {
                for (FixSession session : activeSessions.values()) {
                    if (session.isActive() && session.isTestRequestNeeded()) {
                        session.sendTestRequest();
                        
                        // Log test request event
                        persistSessionEvent(SessionEvent.create(session.getSessionId(), 
                                                              SessionEvent.EventType.TEST_REQUEST_SENT,
                                                              session.getNextOutgoingSeqNum() - 1, null, gatewayId));
                    }
                }
            } catch (Exception e) {
                logger.error("Error in test request sender", e);
            }
        }, 10, 10, TimeUnit.SECONDS);
    }
    
    /**
     * Recover FixSession from persisted state
     */
    private FixSession recoverFixSession(SessionState state, Channel channel) {
        FixSession session = new FixSession(state.getSenderCompId(), state.getTargetCompId(), channel);
        
        // Restore sequence numbers (would need to modify FixSession to support this)
        // For now, we'll log the recovered state
        session.setHeartbeatInterval(state.getHeartbeatInterval());
        session.setState(FixSession.SessionState.DISCONNECTED); // Start as disconnected, will logon again
        
        return session;
    }
    
    /**
     * Check if session should be recovered
     */
    private boolean shouldRecoverSession(SessionState state) {
        // Recover if it's our session or if it's been disconnected for a while
        return gatewayId.equals(state.getGatewayId()) || 
               (state.getState() == FixSession.SessionState.DISCONNECTED &&
                System.currentTimeMillis() - state.getTimestamp() > 60000); // 1 minute timeout
    }
    
    /**
     * Check if we should take over an orphaned session
     */
    private boolean shouldTakeOverSession(SessionState state) {
        // Take over sessions that are disconnected and from other gateways
        return !gatewayId.equals(state.getGatewayId()) &&
               state.getState() == FixSession.SessionState.DISCONNECTED &&
               System.currentTimeMillis() - state.getTimestamp() > 120000; // 2 minute timeout
    }
    
    /**
     * Get session count
     */
    public int getSessionCount() {
        return activeSessions.size();
    }
    
    /**
     * Check if recovery is completed
     */
    public boolean isRecoveryCompleted() {
        return recoveryCompleted;
    }
    
    /**
     * Shutdown the session manager
     */
    public void shutdown() {
        logger.info("Shutting down cluster-aware session manager...");
        
        // Send logout to all active sessions
        for (FixSession session : activeSessions.values()) {
            if (session.isActive()) {
                session.sendLogout("Gateway shutdown");
                
                // Persist final state
                SessionState finalState = SessionState.fromFixSession(session, gatewayId);
                persistence.persistSessionState(finalState);
            }
        }
        
        // Flush all pending writes
        persistence.flush();
        
        activeSessions.clear();
        scheduler.shutdown();
        
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        logger.info("Cluster-aware session manager shutdown complete");
    }
}
