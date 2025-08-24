package com.fixlite.persistence;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Properties;

/**
 * Kafka configuration for RPO=0 (zero data loss) requirements.
 * Optimized for high-frequency trading systems.
 */
public class KafkaConfig {
    
    // Topic names
    public static final String SESSION_EVENTS_TOPIC = "fixlite.session.events";
    public static final String MESSAGE_JOURNAL_TOPIC = "fixlite.message.journal";
    public static final String SESSION_STATE_TOPIC = "fixlite.session.state";
    
    // Consumer groups
    public static final String SESSION_RECOVERY_GROUP = "fixlite-session-recovery";
    public static final String MESSAGE_PROCESSOR_GROUP = "fixlite-message-processor";
    
    private final String bootstrapServers;
    private final String gatewayId;
    
    public KafkaConfig(String bootstrapServers, String gatewayId) {
        this.bootstrapServers = bootstrapServers;
        this.gatewayId = gatewayId;
    }
    
    /**
     * Producer configuration for RPO=0 (zero data loss)
     */
    public Properties getProducerConfig() {
        Properties props = new Properties();
        
        // Connection
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.CLIENT_ID_CONFIG, gatewayId + "-producer");
        
        // Serialization
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        
        // RPO=0 Configuration - Critical for zero data loss
        props.put(ProducerConfig.ACKS_CONFIG, "all"); // Wait for all replicas
        props.put(ProducerConfig.RETRIES_CONFIG, Integer.MAX_VALUE); // Infinite retries
        props.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 1); // Ensure ordering
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true); // Prevent duplicates
        
        // Performance tuning for low latency
        props.put(ProducerConfig.LINGER_MS_CONFIG, 0); // Send immediately
        props.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384); // Small batches
        props.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432); // 32MB buffer
        
        // Timeout configurations
        props.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 30000);
        props.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 120000);
        
        // Compression for better throughput
        props.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "lz4");
        
        return props;
    }
    
    /**
     * Consumer configuration for session recovery
     */
    public Properties getConsumerConfig(String groupId) {
        Properties props = new Properties();
        
        // Connection
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.CLIENT_ID_CONFIG, gatewayId + "-consumer-" + groupId);
        
        // Serialization
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        
        // Consistency settings
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false); // Manual commit for consistency
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest"); // Read from beginning
        props.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed"); // Only committed messages
        
        // Performance settings
        props.put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG, 1); // Fetch immediately
        props.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, 100); // Max wait 100ms
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 1000); // Process in batches
        
        // Session timeout
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 30000);
        props.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 10000);
        
        return props;
    }
    
    /**
     * Kafka Streams configuration
     */
    public Properties getStreamsConfig(String applicationId) {
        Properties props = new Properties();
        
        props.put("application.id", applicationId);
        props.put("bootstrap.servers", bootstrapServers);
        props.put("client.id", gatewayId + "-streams");
        
        // Processing guarantees
        props.put("processing.guarantee", "exactly_once_v2");
        props.put("replication.factor", 3);
        props.put("min.insync.replicas", 2);
        
        // Performance
        props.put("cache.max.bytes.buffering", 10 * 1024 * 1024); // 10MB
        props.put("commit.interval.ms", 100); // Commit every 100ms
        
        return props;
    }
    
    public String getBootstrapServers() {
        return bootstrapServers;
    }
    
    public String getGatewayId() {
        return gatewayId;
    }
}
