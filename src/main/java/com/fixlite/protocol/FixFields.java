package com.fixlite.protocol;

/**
 * FIX protocol field definitions.
 * Contains commonly used FIX field tags as constants.
 */
public final class FixFields {
    
    // Standard Header Fields
    public static final int BEGIN_STRING = 8;
    public static final int BODY_LENGTH = 9;
    public static final int MSG_TYPE = 35;
    public static final int MSG_SEQ_NUM = 34;
    public static final int SENDER_COMP_ID = 49;
    public static final int TARGET_COMP_ID = 56;
    public static final int SENDING_TIME = 52;
    public static final int CHECK_SUM = 10;
    
    // Session Level Fields
    public static final int ENCRYPT_METHOD = 98;
    public static final int HEART_BT_INT = 108;
    public static final int TEST_REQ_ID = 112;
    public static final int USERNAME = 553;
    public static final int PASSWORD = 554;
    public static final int RESET_SEQ_NUM_FLAG = 141;
    
    // Application Level Fields
    public static final int CL_ORD_ID = 11;
    public static final int ORDER_ID = 37;
    public static final int EXEC_ID = 17;
    public static final int EXEC_TYPE = 150;
    public static final int ORD_STATUS = 39;
    public static final int SYMBOL = 55;
    public static final int SIDE = 54;
    public static final int ORDER_QTY = 38;
    public static final int ORD_TYPE = 40;
    public static final int PRICE = 44;
    public static final int TIME_IN_FORCE = 59;
    public static final int LAST_QTY = 32;
    public static final int LAST_PX = 31;
    public static final int LEAVES_QTY = 151;
    public static final int CUM_QTY = 14;
    public static final int AVG_PX = 6;
    
    // Reject Fields
    public static final int REF_SEQ_NUM = 45;
    public static final int REF_TAG_ID = 371;
    public static final int REF_MSG_TYPE = 372;
    public static final int SESSION_REJECT_REASON = 373;
    public static final int TEXT = 58;
    
    // Additional Fields for Post-Trading
    public static final int TRADE_ID = 1003;
    public static final int SHARES = 53;
    public static final int ACCOUNT = 1;
    public static final int CURRENCY = 15;
    public static final int DEFAULT_APPL_VER_ID = 1137;
    public static final int APPLICATION_SYSTEM_NAME = 1603;
    
    private FixFields() {
        // Utility class
    }
}
