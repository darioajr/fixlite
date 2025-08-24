package com.fixlite.protocol;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.StringJoiner;

/**
 * Represents a FIX message with fields and provides encoding/decoding capabilities.
 * Supports multiple FIX protocol versions and custom field definitions.
 * Optimized for GraalVM Native Image compatibility.
 */
public class FixMessage {
    
    public static final char FIELD_SEPARATOR = '\u0001'; // SOH character
    
    private final Map<Integer, String> fields = new LinkedHashMap<>();
    private String messageType;
    private FixVersion version = FixVersion.FIX_4_4; // Default version
    
    public FixMessage() {}
    
    public FixMessage(String messageType) {
        this.messageType = messageType;
        setField(FixFields.MSG_TYPE, messageType);
    }
    
    /**
     * Set a field value
     */
    public FixMessage setField(int tag, String value) {
        fields.put(tag, value);
        if (tag == FixFields.MSG_TYPE) {
            this.messageType = value;
        }
        return this;
    }
    
    /**
     * Set a field value with integer
     */
    public FixMessage setField(int tag, int value) {
        return setField(tag, String.valueOf(value));
    }
    
    /**
     * Set a field value with long
     */
    public FixMessage setField(int tag, long value) {
        return setField(tag, String.valueOf(value));
    }
    
    /**
     * Set a field value with double
     */
    public FixMessage setField(int tag, double value) {
        return setField(tag, String.valueOf(value));
    }
    
    /**
     * Set a field value with char
     */
    public FixMessage setField(int tag, char value) {
        return setField(tag, String.valueOf(value));
    }
    
    /**
     * Get field value
     */
    public String getField(int tag) {
        return fields.get(tag);
    }
    
    /**
     * Get field value as integer
     */
    public Integer getFieldAsInt(int tag) {
        String value = getField(tag);
        return value != null ? Integer.parseInt(value) : null;
    }
    
    /**
     * Get field value as long
     */
    public Long getFieldAsLong(int tag) {
        String value = getField(tag);
        return value != null ? Long.parseLong(value) : null;
    }
    
    /**
     * Get field value as double
     */
    public Double getFieldAsDouble(int tag) {
        String value = getField(tag);
        return value != null ? Double.parseDouble(value) : null;
    }
    
    /**
     * Get field value as char
     */
    public Character getFieldAsChar(int tag) {
        String value = getField(tag);
        return value != null && !value.isEmpty() ? value.charAt(0) : null;
    }
    
    /**
     * Check if field exists
     */
    public boolean hasField(int tag) {
        return fields.containsKey(tag);
    }
    
    /**
     * Get message type
     */
    public String getMessageType() {
        return messageType;
    }
    
    /**
     * Get FIX version
     */
    public FixVersion getVersion() {
        return version;
    }
    
    /**
     * Set FIX version
     */
    public void setVersion(FixVersion version) {
        this.version = version;
    }
    
    /**
     * Get all fields
     */
    public Map<Integer, String> getFields() {
        return new LinkedHashMap<>(fields);
    }
    
    /**
     * Encode message to FIX string format compatible with QuickFIX and other FIX engines.
     * Follows strict FIX protocol field ordering for maximum compatibility.
     */
    public String encode() {
        if (fields.isEmpty()) {
            throw new IllegalStateException("Cannot encode empty message");
        }
        
        StringBuilder message = new StringBuilder(512);
        
        // Step 1: Build header (BeginString, BodyLength placeholder, MsgType)
        String beginString = getField(FixFields.BEGIN_STRING);
        if (beginString == null) {
            beginString = version.getBeginString();
        }
        message.append(FixFields.BEGIN_STRING).append("=").append(beginString).append(FIELD_SEPARATOR);
        
        // Placeholder for BodyLength - will be calculated later
        int bodyLengthPos = message.length();
        message.append(FixFields.BODY_LENGTH).append("=");
        int bodyLengthValuePos = message.length();
        message.append("00000").append(FIELD_SEPARATOR); // Placeholder
        
        // Step 2: Add standard header fields in correct order
        addFieldIfPresent(message, FixFields.MSG_TYPE);
        addFieldIfPresent(message, FixFields.MSG_SEQ_NUM);
        addFieldIfPresent(message, FixFields.SENDER_COMP_ID);
        addFieldIfPresent(message, FixFields.TARGET_COMP_ID);
        addFieldIfPresent(message, FixFields.SENDING_TIME);
        
        // Step 3: Add session-level fields
        addFieldIfPresent(message, FixFields.ENCRYPT_METHOD);
        addFieldIfPresent(message, FixFields.HEART_BT_INT);
        addFieldIfPresent(message, FixFields.TEST_REQ_ID);
        addFieldIfPresent(message, FixFields.USERNAME);
        addFieldIfPresent(message, FixFields.PASSWORD);
        addFieldIfPresent(message, FixFields.RESET_SEQ_NUM_FLAG);
        
        // Step 4: Add application fields in tag number order
        fields.entrySet().stream()
            .filter(entry -> !isStandardHeaderField(entry.getKey()) && 
                           !isTrailerField(entry.getKey()) &&
                           !isSessionField(entry.getKey()))
            .sorted(Map.Entry.comparingByKey())
            .forEach(entry -> {
                message.append(entry.getKey()).append("=").append(entry.getValue()).append(FIELD_SEPARATOR);
            });
        
        // Step 5: Calculate body length (everything after BodyLength field)
        int bodyStart = bodyLengthValuePos + 6; // Position after "XXXXX\001"
        int bodyLength = message.length() - bodyStart;
        
        // Update BodyLength field
        String bodyLengthStr = String.valueOf(bodyLength);
        int padLength = 5 - bodyLengthStr.length();
        for (int i = 0; i < padLength; i++) {
            message.setCharAt(bodyLengthValuePos + i, '0');
        }
        for (int i = 0; i < bodyLengthStr.length(); i++) {
            message.setCharAt(bodyLengthValuePos + padLength + i, bodyLengthStr.charAt(i));
        }
        
        // Step 6: Calculate and add checksum (excludes checksum field itself)
        int checksum = calculateChecksum(message.toString());
        message.append(FixFields.CHECK_SUM).append("=").append(String.format("%03d", checksum)).append(FIELD_SEPARATOR);
        
        return message.toString();
    }
    
    /**
     * Adds field to message if present in fields map.
     */
    private void addFieldIfPresent(StringBuilder message, int tag) {
        String value = getField(tag);
        if (value != null) {
            message.append(tag).append("=").append(value).append(FIELD_SEPARATOR);
        }
    }
    
    /**
     * Checks if field is a standard header field.
     */
    private boolean isStandardHeaderField(int tag) {
        return tag == FixFields.BEGIN_STRING || 
               tag == FixFields.BODY_LENGTH || 
               tag == FixFields.MSG_TYPE ||
               tag == FixFields.MSG_SEQ_NUM ||
               tag == FixFields.SENDER_COMP_ID ||
               tag == FixFields.TARGET_COMP_ID ||
               tag == FixFields.SENDING_TIME;
    }
    
    /**
     * Checks if field is a session-level field.
     */
    private boolean isSessionField(int tag) {
        return tag == FixFields.ENCRYPT_METHOD ||
               tag == FixFields.HEART_BT_INT ||
               tag == FixFields.TEST_REQ_ID ||
               tag == FixFields.USERNAME ||
               tag == FixFields.PASSWORD ||
               tag == FixFields.RESET_SEQ_NUM_FLAG;
    }
    
    /**
     * Checks if field is a trailer field.
     */
    private boolean isTrailerField(int tag) {
        return tag == FixFields.CHECK_SUM;
    }
    
    /**
     * Calculate FIX checksum
     */
    private int calculateChecksum(String message) {
        int sum = 0;
        for (char c : message.toCharArray()) {
            sum += c;
        }
        return sum % 256;
    }
    
    @Override
    public String toString() {
        return "FixMessage{" +
                "messageType='" + messageType + '\'' +
                ", fields=" + fields +
                '}';
    }
}
