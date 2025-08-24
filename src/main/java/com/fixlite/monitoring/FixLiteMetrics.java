package com.fixlite.monitoring;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Metrics collection for FixLite Gateway.
 * Tracks performance and operational metrics for monitoring.
 */
public class FixLiteMetrics {
    
    // Session metrics
    private final AtomicLong activeSessions = new AtomicLong(0);
    private final LongAdder totalSessionsCreated = new LongAdder();
    private final LongAdder totalLogons = new LongAdder();
    private final LongAdder totalLogouts = new LongAdder();
    private final LongAdder sessionRecoveries = new LongAdder();
    
    // Message metrics
    private final LongAdder messagesReceived = new LongAdder();
    private final LongAdder messagesSent = new LongAdder();
    private final LongAdder heartbeatsSent = new LongAdder();
    private final LongAdder heartbeatsReceived = new LongAdder();
    private final LongAdder testRequestsSent = new LongAdder();
    private final LongAdder testRequestsReceived = new LongAdder();
    
    // Order metrics
    private final LongAdder ordersReceived = new LongAdder();
    private final LongAdder ordersExecuted = new LongAdder();
    private final LongAdder ordersRejected = new LongAdder();
    private final LongAdder executionReportsSent = new LongAdder();
    
    // Error metrics
    private final LongAdder parseErrors = new LongAdder();
    private final LongAdder sequenceErrors = new LongAdder();
    private final LongAdder connectionErrors = new LongAdder();
    private final LongAdder persistenceErrors = new LongAdder();
    
    // Latency tracking (simple histogram)
    private final Map<String, LatencyTracker> latencyTrackers = new ConcurrentHashMap<>();
    
    // Kafka metrics
    private final LongAdder kafkaMessagesPersisted = new LongAdder();
    private final LongAdder kafkaPersistenceErrors = new LongAdder();
    private final AtomicLong lastKafkaPersistTime = new AtomicLong(0);
    
    public FixLiteMetrics() {
        // Initialize latency trackers
        latencyTrackers.put("message_processing", new LatencyTracker());
        latencyTrackers.put("order_processing", new LatencyTracker());
        latencyTrackers.put("kafka_persistence", new LatencyTracker());
    }
    
    // Session metrics
    public void incrementActiveSessions() { activeSessions.incrementAndGet(); }
    public void decrementActiveSessions() { activeSessions.decrementAndGet(); }
    public void incrementSessionsCreated() { totalSessionsCreated.increment(); }
    public void incrementLogons() { totalLogons.increment(); }
    public void incrementLogouts() { totalLogouts.increment(); }
    public void incrementSessionRecoveries() { sessionRecoveries.increment(); }
    
    // Message metrics
    public void incrementMessagesReceived() { messagesReceived.increment(); }
    public void incrementMessagesSent() { messagesSent.increment(); }
    public void incrementHeartbeatsSent() { heartbeatsSent.increment(); }
    public void incrementHeartbeatsReceived() { heartbeatsReceived.increment(); }
    public void incrementTestRequestsSent() { testRequestsSent.increment(); }
    public void incrementTestRequestsReceived() { testRequestsReceived.increment(); }
    
    // Order metrics
    public void incrementOrdersReceived() { ordersReceived.increment(); }
    public void incrementOrdersExecuted() { ordersExecuted.increment(); }
    public void incrementOrdersRejected() { ordersRejected.increment(); }
    public void incrementExecutionReportsSent() { executionReportsSent.increment(); }
    
    // Error metrics
    public void incrementParseErrors() { parseErrors.increment(); }
    public void incrementSequenceErrors() { sequenceErrors.increment(); }
    public void incrementConnectionErrors() { connectionErrors.increment(); }
    public void incrementPersistenceErrors() { persistenceErrors.increment(); }
    
    // Kafka metrics
    public void incrementKafkaMessagesPersisted() { 
        kafkaMessagesPersisted.increment(); 
        lastKafkaPersistTime.set(System.currentTimeMillis());
    }
    public void incrementKafkaPersistenceErrors() { kafkaPersistenceErrors.increment(); }
    
    // Latency tracking
    public void recordLatency(String operation, long latencyNanos) {
        LatencyTracker tracker = latencyTrackers.get(operation);
        if (tracker != null) {
            tracker.record(latencyNanos);
        }
    }
    
    // Getters
    public long getActiveSessions() { return activeSessions.get(); }
    public long getTotalSessionsCreated() { return totalSessionsCreated.sum(); }
    public long getTotalLogons() { return totalLogons.sum(); }
    public long getTotalLogouts() { return totalLogouts.sum(); }
    public long getSessionRecoveries() { return sessionRecoveries.sum(); }
    
    public long getMessagesReceived() { return messagesReceived.sum(); }
    public long getMessagesSent() { return messagesSent.sum(); }
    public long getHeartbeatsSent() { return heartbeatsSent.sum(); }
    public long getHeartbeatsReceived() { return heartbeatsReceived.sum(); }
    public long getTestRequestsSent() { return testRequestsSent.sum(); }
    public long getTestRequestsReceived() { return testRequestsReceived.sum(); }
    
    public long getOrdersReceived() { return ordersReceived.sum(); }
    public long getOrdersExecuted() { return ordersExecuted.sum(); }
    public long getOrdersRejected() { return ordersRejected.sum(); }
    public long getExecutionReportsSent() { return executionReportsSent.sum(); }
    
    public long getParseErrors() { return parseErrors.sum(); }
    public long getSequenceErrors() { return sequenceErrors.sum(); }
    public long getConnectionErrors() { return connectionErrors.sum(); }
    public long getPersistenceErrors() { return persistenceErrors.sum(); }
    
    public long getKafkaMessagesPersisted() { return kafkaMessagesPersisted.sum(); }
    public long getKafkaPersistenceErrors() { return kafkaPersistenceErrors.sum(); }
    public long getLastKafkaPersistTime() { return lastKafkaPersistTime.get(); }
    
    public LatencyStats getLatencyStats(String operation) {
        LatencyTracker tracker = latencyTrackers.get(operation);
        return tracker != null ? tracker.getStats() : null;
    }
    
    /**
     * Get all metrics as a formatted string
     */
    public String getMetricsReport() {
        StringBuilder report = new StringBuilder();
        report.append("=== FixLite Gateway Metrics ===\n");
        
        report.append("\n--- Session Metrics ---\n");
        report.append(String.format("Active Sessions: %d\n", getActiveSessions()));
        report.append(String.format("Total Sessions Created: %d\n", getTotalSessionsCreated()));
        report.append(String.format("Total Logons: %d\n", getTotalLogons()));
        report.append(String.format("Total Logouts: %d\n", getTotalLogouts()));
        report.append(String.format("Session Recoveries: %d\n", getSessionRecoveries()));
        
        report.append("\n--- Message Metrics ---\n");
        report.append(String.format("Messages Received: %d\n", getMessagesReceived()));
        report.append(String.format("Messages Sent: %d\n", getMessagesSent()));
        report.append(String.format("Heartbeats Sent: %d\n", getHeartbeatsSent()));
        report.append(String.format("Heartbeats Received: %d\n", getHeartbeatsReceived()));
        report.append(String.format("Test Requests Sent: %d\n", getTestRequestsSent()));
        report.append(String.format("Test Requests Received: %d\n", getTestRequestsReceived()));
        
        report.append("\n--- Order Metrics ---\n");
        report.append(String.format("Orders Received: %d\n", getOrdersReceived()));
        report.append(String.format("Orders Executed: %d\n", getOrdersExecuted()));
        report.append(String.format("Orders Rejected: %d\n", getOrdersRejected()));
        report.append(String.format("Execution Reports Sent: %d\n", getExecutionReportsSent()));
        
        report.append("\n--- Error Metrics ---\n");
        report.append(String.format("Parse Errors: %d\n", getParseErrors()));
        report.append(String.format("Sequence Errors: %d\n", getSequenceErrors()));
        report.append(String.format("Connection Errors: %d\n", getConnectionErrors()));
        report.append(String.format("Persistence Errors: %d\n", getPersistenceErrors()));
        
        report.append("\n--- Kafka Metrics ---\n");
        report.append(String.format("Kafka Messages Persisted: %d\n", getKafkaMessagesPersisted()));
        report.append(String.format("Kafka Persistence Errors: %d\n", getKafkaPersistenceErrors()));
        report.append(String.format("Last Kafka Persist Time: %d\n", getLastKafkaPersistTime()));
        
        report.append("\n--- Latency Metrics ---\n");
        for (Map.Entry<String, LatencyTracker> entry : latencyTrackers.entrySet()) {
            LatencyStats stats = entry.getValue().getStats();
            report.append(String.format("%s: avg=%.2fμs, min=%.2fμs, max=%.2fμs, count=%d\n",
                    entry.getKey(), 
                    stats.getAverageNanos() / 1000.0,
                    stats.getMinNanos() / 1000.0,
                    stats.getMaxNanos() / 1000.0,
                    stats.getCount()));
        }
        
        return report.toString();
    }
    
    /**
     * Simple latency tracker
     */
    private static class LatencyTracker {
        private final LongAdder count = new LongAdder();
        private final LongAdder totalNanos = new LongAdder();
        private volatile long minNanos = Long.MAX_VALUE;
        private volatile long maxNanos = Long.MIN_VALUE;
        
        public void record(long latencyNanos) {
            count.increment();
            totalNanos.add(latencyNanos);
            
            // Update min/max (not thread-safe but good enough for monitoring)
            if (latencyNanos < minNanos) {
                minNanos = latencyNanos;
            }
            if (latencyNanos > maxNanos) {
                maxNanos = latencyNanos;
            }
        }
        
        public LatencyStats getStats() {
            long cnt = count.sum();
            if (cnt == 0) {
                return new LatencyStats(0, 0, 0, 0);
            }
            
            long total = totalNanos.sum();
            long avg = total / cnt;
            
            return new LatencyStats(avg, minNanos, maxNanos, cnt);
        }
    }
    
    /**
     * Latency statistics
     */
    public static class LatencyStats {
        private final long averageNanos;
        private final long minNanos;
        private final long maxNanos;
        private final long count;
        
        public LatencyStats(long averageNanos, long minNanos, long maxNanos, long count) {
            this.averageNanos = averageNanos;
            this.minNanos = minNanos;
            this.maxNanos = maxNanos;
            this.count = count;
        }
        
        public long getAverageNanos() { return averageNanos; }
        public long getMinNanos() { return minNanos; }
        public long getMaxNanos() { return maxNanos; }
        public long getCount() { return count; }
    }
}
