package com.fixlite.protocol;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Factory for creating FIX messages with version-specific behavior.
 * Handles message creation, validation, and formatting for different FIX protocol versions.
 */
public class FixMessageFactory {
    
    private final FixVersion version;
    private final FixFieldsRegistry fieldsRegistry;
    private final AtomicInteger sequenceNumber;
    private final String senderCompId;
    private final String targetCompId;
    
    private static final DateTimeFormatter FIX_TIMESTAMP_FORMAT = 
        DateTimeFormatter.ofPattern("yyyyMMdd-HH:mm:ss.SSS");
    
    public FixMessageFactory(FixVersion version, String senderCompId, String targetCompId) {
        this.version = version;
        this.fieldsRegistry = new FixFieldsRegistry();
        this.sequenceNumber = new AtomicInteger(1);
        this.senderCompId = senderCompId;
        this.targetCompId = targetCompId;
    }
    
    /**
     * Creates a new FIX message with standard header fields populated.
     */
    public FixMessage createMessage(String messageType) {
        FixMessage message = new FixMessage(messageType);
        message.setVersion(version);
        
        // Set standard header fields
        message.setField(FixFields.BEGIN_STRING, version.getBeginString());
        message.setField(FixFields.MSG_TYPE, messageType);
        message.setField(FixFields.MSG_SEQ_NUM, sequenceNumber.getAndIncrement());
        message.setField(FixFields.SENDER_COMP_ID, senderCompId);
        message.setField(FixFields.TARGET_COMP_ID, targetCompId);
        message.setField(FixFields.SENDING_TIME, getCurrentTimestamp());
        
        // Version-specific fields
        if (version == FixVersion.FIX_5_0_SP2) {
            message.setField(1128, "9"); // ApplVerID for FIX 5.0 SP2
        }
        
        return message;
    }
    
    /**
     * Creates a Logon message.
     */
    public FixMessage createLogon(int heartbeatInterval, String username, String password) {
        FixMessage message = createMessage(FixMessageTypes.LOGON);
        message.setField(FixFields.ENCRYPT_METHOD, 0); // None
        message.setField(FixFields.HEART_BT_INT, heartbeatInterval);
        
        if (username != null) {
            message.setField(FixFields.USERNAME, username);
        }
        if (password != null) {
            message.setField(FixFields.PASSWORD, password);
        }
        
        return message;
    }
    
    /**
     * Creates a Logout message.
     */
    public FixMessage createLogout(String text) {
        FixMessage message = createMessage(FixMessageTypes.LOGOUT);
        if (text != null) {
            message.setField(FixFields.TEXT, text);
        }
        return message;
    }
    
    /**
     * Creates a Heartbeat message.
     */
    public FixMessage createHeartbeat(String testReqId) {
        FixMessage message = createMessage(FixMessageTypes.HEARTBEAT);
        if (testReqId != null) {
            message.setField(FixFields.TEST_REQ_ID, testReqId);
        }
        return message;
    }
    
    /**
     * Creates a Test Request message.
     */
    public FixMessage createTestRequest(String testReqId) {
        FixMessage message = createMessage(FixMessageTypes.TEST_REQUEST);
        message.setField(FixFields.TEST_REQ_ID, testReqId);
        return message;
    }
    
    /**
     * Creates a New Order Single message.
     */
    public FixMessage createNewOrderSingle(String clOrdId, String symbol, char side, 
                                         double quantity, char ordType, Double price) {
        FixMessage message = createMessage(FixMessageTypes.NEW_ORDER_SINGLE);
        message.setField(FixFields.CL_ORD_ID, clOrdId);
        message.setField(FixFields.SYMBOL, symbol);
        message.setField(FixFields.SIDE, side);
        message.setField(FixFields.ORDER_QTY, quantity);
        message.setField(FixFields.ORD_TYPE, ordType);
        
        if (price != null && ordType == '2') { // Limit order
            message.setField(FixFields.PRICE, price);
        }
        
        // Default time in force
        message.setField(FixFields.TIME_IN_FORCE, '0'); // Day
        
        return message;
    }
    
    /**
     * Creates an Execution Report message.
     */
    public FixMessage createExecutionReport(String orderId, String execId, char execType, 
                                          char ordStatus, String clOrdId, String symbol, 
                                          char side, double leavesQty, double cumQty) {
        FixMessage message = createMessage(FixMessageTypes.EXECUTION_REPORT);
        message.setField(FixFields.ORDER_ID, orderId);
        message.setField(FixFields.EXEC_ID, execId);
        message.setField(FixFields.EXEC_TYPE, execType);
        message.setField(FixFields.ORD_STATUS, ordStatus);
        message.setField(FixFields.CL_ORD_ID, clOrdId);
        message.setField(FixFields.SYMBOL, symbol);
        message.setField(FixFields.SIDE, side);
        message.setField(FixFields.LEAVES_QTY, leavesQty);
        message.setField(FixFields.CUM_QTY, cumQty);
        
        return message;
    }
    
    /**
     * Creates a Reject message.
     */
    public FixMessage createReject(int refSeqNum, String refMsgType, int sessionRejectReason, String text) {
        FixMessage message = createMessage(FixMessageTypes.REJECT);
        message.setField(FixFields.REF_SEQ_NUM, refSeqNum);
        message.setField(FixFields.REF_MSG_TYPE, refMsgType);
        message.setField(FixFields.SESSION_REJECT_REASON, sessionRejectReason);
        
        if (text != null) {
            message.setField(FixFields.TEXT, text);
        }
        
        return message;
    }
    
    /**
     * Validates a message against the current FIX version.
     */
    public ValidationResult validateMessage(FixMessage message) {
        ValidationResult result = new ValidationResult();
        
        // Check required header fields
        if (!message.hasField(FixFields.BEGIN_STRING)) {
            result.addError("Missing required field: BeginString (8)");
        }
        if (!message.hasField(FixFields.MSG_TYPE)) {
            result.addError("Missing required field: MsgType (35)");
        }
        if (!message.hasField(FixFields.MSG_SEQ_NUM)) {
            result.addError("Missing required field: MsgSeqNum (34)");
        }
        if (!message.hasField(FixFields.SENDER_COMP_ID)) {
            result.addError("Missing required field: SenderCompID (49)");
        }
        if (!message.hasField(FixFields.TARGET_COMP_ID)) {
            result.addError("Missing required field: TargetCompID (56)");
        }
        if (!message.hasField(FixFields.SENDING_TIME)) {
            result.addError("Missing required field: SendingTime (52)");
        }
        
        // Validate version compatibility
        String beginString = message.getField(FixFields.BEGIN_STRING);
        if (beginString != null && !beginString.equals(version.getBeginString())) {
            result.addError("BeginString mismatch. Expected: " + version.getBeginString() + 
                          ", Found: " + beginString);
        }
        
        // Validate fields against registry
        for (Integer tag : message.getFields().keySet()) {
            if (!fieldsRegistry.isFieldSupported(tag, version)) {
                result.addWarning("Field " + tag + " is not supported in " + version.name());
            }
        }
        
        return result;
    }
    
    /**
     * Gets the current sequence number.
     */
    public int getCurrentSequenceNumber() {
        return sequenceNumber.get();
    }
    
    /**
     * Sets the sequence number.
     */
    public void setSequenceNumber(int seqNum) {
        sequenceNumber.set(seqNum);
    }
    
    /**
     * Gets the FIX version.
     */
    public FixVersion getVersion() {
        return version;
    }
    
    /**
     * Gets the fields registry.
     */
    public FixFieldsRegistry getFieldsRegistry() {
        return fieldsRegistry;
    }
    
    private String getCurrentTimestamp() {
        return LocalDateTime.now().format(FIX_TIMESTAMP_FORMAT);
    }
    
    /**
     * Validation result containing errors and warnings.
     */
    public static class ValidationResult {
        private final java.util.List<String> errors = new java.util.ArrayList<>();
        private final java.util.List<String> warnings = new java.util.ArrayList<>();
        
        public void addError(String error) {
            errors.add(error);
        }
        
        public void addWarning(String warning) {
            warnings.add(warning);
        }
        
        public boolean isValid() {
            return errors.isEmpty();
        }
        
        public java.util.List<String> getErrors() {
            return errors;
        }
        
        public java.util.List<String> getWarnings() {
            return warnings;
        }
    }
}
