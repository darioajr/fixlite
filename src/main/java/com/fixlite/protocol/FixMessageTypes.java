package com.fixlite.protocol;

/**
 * FIX message type constants.
 * Defines the standard FIX message types used in trading systems.
 */
public final class FixMessageTypes {
    
    // Session Level Messages
    public static final String HEARTBEAT = "0";
    public static final String TEST_REQUEST = "1";
    public static final String RESEND_REQUEST = "2";
    public static final String REJECT = "3";
    public static final String SEQUENCE_RESET = "4";
    public static final String LOGOUT = "5";
    public static final String LOGON = "A";
    
    // Application Level Messages
    public static final String NEW_ORDER_SINGLE = "D";
    public static final String EXECUTION_REPORT = "8";
    public static final String ORDER_CANCEL_REQUEST = "F";
    public static final String ORDER_CANCEL_REJECT = "9";
    public static final String ORDER_STATUS_REQUEST = "H";
    public static final String BUSINESS_MESSAGE_REJECT = "j";
    
    // Market Data Messages
    public static final String MARKET_DATA_REQUEST = "V";
    public static final String MARKET_DATA_SNAPSHOT = "W";
    public static final String MARKET_DATA_INCREMENTAL_REFRESH = "X";
    public static final String MARKET_DATA_REQUEST_REJECT = "Y";
    
    private FixMessageTypes() {
        // Utility class
    }
}
