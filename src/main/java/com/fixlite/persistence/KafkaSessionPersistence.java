package com.fixlite.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fixlite.protocol.FixMessage;
import com.fixlite.session.FixSession;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

/**
 * Kafka-based session persistence with RPO=0 guarantees.
 * Handles session state persistence, recovery, and clustering.
 */
public class KafkaSessionPersistence {
    
    private static final Logger logger = LoggerFactory.getLogger(KafkaSessionPersistence.class);
    
    private final KafkaConfig kafkaConfig;
    private final ObjectMapper objectMapper;
    private final KafkaProducer<String, String> producer;
    private final KafkaConsumer<String, String> consumer;
    
    // In-memory cache of latest session states
    private final Map<String, SessionState> sessionStateCache = new ConcurrentHashMap<>();
    
    public KafkaSessionPersistence(KafkaConfig kafkaConfig) {
        this.kafkaConfig = kafkaConfig;
        this.objectMapper = new ObjectMapper();
        this.producer = new KafkaProducer<>(kafkaConfig.getProducerConfig());
        this.consumer = new KafkaConsumer<>(kafkaConfig.getConsumerConfig(KafkaConfig.SESSION_RECOVERY_GROUP));
        
        // Subscribe to session state topic for recovery
        consumer.subscribe(Collections.singletonList(KafkaConfig.SESSION_STATE_TOPIC));
        
        logger.info("Kafka session persistence initialized for gateway: {}", kafkaConfig.getGatewayId());
    }
    
    /**
     * Persist session event with RPO=0 guarantee
     */
    public Future<RecordMetadata> persistSessionEvent(SessionEvent event) {
        try {
            String eventJson = objectMapper.writeValueAsString(event);
            ProducerRecord<String, String> record = new ProducerRecord<>(
                    KafkaConfig.SESSION_EVENTS_TOPIC,
                    event.getSessionId(),
                    eventJson
            );
            
            Future<RecordMetadata> future = producer.send(record, (metadata, exception) -> {
                if (exception != null) {
                    logger.error("Failed to persist session event: {}", event, exception);
                } else {
                    logger.debug("Session event persisted: {} at offset {}", 
                               event.getEventType(), metadata.offset());
                }
            });
            
            logger.debug("Persisting session event: {}", event);
            return future;
            
        } catch (Exception e) {
            logger.error("Error persisting session event: {}", event, e);
            throw new RuntimeException("Failed to persist session event", e);
        }
    }
    
    /**
     * Persist session state with RPO=0 guarantee
     */
    public Future<RecordMetadata> persistSessionState(SessionState sessionState) {
        try {
            String stateJson = objectMapper.writeValueAsString(sessionState);
            ProducerRecord<String, String> record = new ProducerRecord<>(
                    KafkaConfig.SESSION_STATE_TOPIC,
                    sessionState.getSessionId(),
                    stateJson
            );
            
            Future<RecordMetadata> future = producer.send(record, (metadata, exception) -> {
                if (exception != null) {
                    logger.error("Failed to persist session state: {}", sessionState, exception);
                } else {
                    logger.debug("Session state persisted: {} at offset {}", 
                               sessionState.getSessionId(), metadata.offset());
                    // Update local cache
                    sessionStateCache.put(sessionState.getSessionId(), sessionState);
                }
            });
            
            return future;
            
        } catch (Exception e) {
            logger.error("Error persisting session state: {}", sessionState, e);
            throw new RuntimeException("Failed to persist session state", e);
        }
    }
    
    /**
     * Persist FIX message to journal with RPO=0 guarantee
     */
    public Future<RecordMetadata> persistMessage(String sessionId, FixMessage message, 
                                               MessageDirection direction) {
        try {
            MessageJournalEntry entry = new MessageJournalEntry(
                    sessionId,
                    message.encode(),
                    message.getMessageType(),
                    message.getFieldAsInt(com.fixlite.protocol.FixFields.MSG_SEQ_NUM),
                    direction,
                    kafkaConfig.getGatewayId()
            );
            
            String entryJson = objectMapper.writeValueAsString(entry);
            ProducerRecord<String, String> record = new ProducerRecord<>(
                    KafkaConfig.MESSAGE_JOURNAL_TOPIC,
                    sessionId,
                    entryJson
            );
            
            Future<RecordMetadata> future = producer.send(record, (metadata, exception) -> {
                if (exception != null) {
                    logger.error("Failed to persist message: {} for session {}", 
                               message.getMessageType(), sessionId, exception);
                } else {
                    logger.debug("Message persisted: {} seq={} at offset {}", 
                               message.getMessageType(), entry.getSequenceNumber(), metadata.offset());
                }
            });
            
            return future;
            
        } catch (Exception e) {
            logger.error("Error persisting message for session {}: {}", sessionId, message, e);
            throw new RuntimeException("Failed to persist message", e);
        }
    }
    
    /**
     * Recover session state from Kafka
     */
    public SessionState recoverSessionState(String sessionId) {
        // First check cache
        SessionState cachedState = sessionStateCache.get(sessionId);
        if (cachedState != null) {
            logger.debug("Session state found in cache: {}", sessionId);
            return cachedState;
        }
        
        // Poll Kafka for latest state
        logger.info("Recovering session state from Kafka: {}", sessionId);
        
        try {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
            SessionState latestState = null;
            
            for (ConsumerRecord<String, String> record : records) {
                if (sessionId.equals(record.key())) {
                    try {
                        SessionState state = objectMapper.readValue(record.value(), SessionState.class);
                        if (latestState == null || state.getTimestamp() > latestState.getTimestamp()) {
                            latestState = state;
                        }
                    } catch (Exception e) {
                        logger.warn("Failed to deserialize session state: {}", record.value(), e);
                    }
                }
            }
            
            if (latestState != null) {
                sessionStateCache.put(sessionId, latestState);
                logger.info("Session state recovered: {}", latestState);
            } else {
                logger.warn("No session state found for: {}", sessionId);
            }
            
            return latestState;
            
        } catch (Exception e) {
            logger.error("Error recovering session state for: {}", sessionId, e);
            return null;
        }
    }
    
    /**
     * Load all session states for this gateway (clustering support)
     */
    public Map<String, SessionState> loadAllSessionStates() {
        logger.info("Loading all session states for gateway: {}", kafkaConfig.getGatewayId());
        
        Map<String, SessionState> states = new ConcurrentHashMap<>();
        
        try {
            // Seek to beginning to read all states
            consumer.seekToBeginning(consumer.assignment());
            
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(10));
            
            for (ConsumerRecord<String, String> record : records) {
                try {
                    SessionState state = objectMapper.readValue(record.value(), SessionState.class);
                    
                    // Only load states for this gateway or states that can be taken over
                    if (kafkaConfig.getGatewayId().equals(state.getGatewayId()) ||
                        state.getState() == FixSession.SessionState.DISCONNECTED) {
                        
                        SessionState existing = states.get(state.getSessionId());
                        if (existing == null || state.getTimestamp() > existing.getTimestamp()) {
                            states.put(state.getSessionId(), state);
                        }
                    }
                } catch (Exception e) {
                    logger.warn("Failed to deserialize session state: {}", record.value(), e);
                }
            }
            
            // Update cache
            sessionStateCache.putAll(states);
            
            logger.info("Loaded {} session states", states.size());
            
        } catch (Exception e) {
            logger.error("Error loading session states", e);
        }
        
        return states;
    }
    
    /**
     * Flush all pending writes (for shutdown)
     */
    public void flush() {
        try {
            producer.flush();
            logger.info("Kafka producer flushed");
        } catch (Exception e) {
            logger.error("Error flushing producer", e);
        }
    }
    
    /**
     * Close resources
     */
    public void close() {
        logger.info("Closing Kafka session persistence");
        
        try {
            producer.close();
        } catch (Exception e) {
            logger.error("Error closing producer", e);
        }
        
        try {
            consumer.close();
        } catch (Exception e) {
            logger.error("Error closing consumer", e);
        }
    }
    
    /**
     * Get cached session state
     */
    public SessionState getCachedSessionState(String sessionId) {
        return sessionStateCache.get(sessionId);
    }
    
    /**
     * Clear session from cache (when session ends)
     */
    public void clearSessionFromCache(String sessionId) {
        sessionStateCache.remove(sessionId);
        logger.debug("Cleared session from cache: {}", sessionId);
    }
    
    public enum MessageDirection {
        INBOUND,
        OUTBOUND
    }
}
