package com.fixlite.protocol;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for FixMessage class with multi-version support.
 */
public class FixMessageTest {
    
    @Test
    public void testCreateMessage() {
        FixMessage message = new FixMessage(FixMessageTypes.LOGON);
        
        assertEquals(FixMessageTypes.LOGON, message.getMessageType());
        assertEquals(FixMessageTypes.LOGON, message.getField(FixFields.MSG_TYPE));
        assertEquals(FixVersion.FIX_4_4, message.getVersion()); // Default version
    }
    
    @Test
    public void testSetAndGetFields() {
        FixMessage message = new FixMessage();
        
        message.setField(FixFields.SENDER_COMP_ID, "CLIENT");
        message.setField(FixFields.TARGET_COMP_ID, "SERVER");
        message.setField(FixFields.MSG_SEQ_NUM, 1);
        
        assertEquals("CLIENT", message.getField(FixFields.SENDER_COMP_ID));
        assertEquals("SERVER", message.getField(FixFields.TARGET_COMP_ID));
        assertEquals(Integer.valueOf(1), message.getFieldAsInt(FixFields.MSG_SEQ_NUM));
    }
    
    @Test
    public void testVersionSupport() {
        FixMessage message = new FixMessage();
        
        // Test FIX 4.4
        message.setVersion(FixVersion.FIX_4_4);
        assertEquals(FixVersion.FIX_4_4, message.getVersion());
        
        // Test FIX 5.0 SP2
        message.setVersion(FixVersion.FIX_5_0_SP2);
        assertEquals(FixVersion.FIX_5_0_SP2, message.getVersion());
        
        // Test Custom
        message.setVersion(FixVersion.CUSTOM);
        assertEquals(FixVersion.CUSTOM, message.getVersion());
    }
    
    @Test
    public void testDataTypeFields() {
        FixMessage message = new FixMessage();
        
        // Test double field
        message.setField(FixFields.PRICE, 123.45);
        assertEquals(Double.valueOf(123.45), message.getFieldAsDouble(FixFields.PRICE));
        
        // Test char field
        message.setField(FixFields.SIDE, '1');
        assertEquals(Character.valueOf('1'), message.getFieldAsChar(FixFields.SIDE));
        
        // Test long field
        message.setField(FixFields.ORDER_QTY, 1000L);
        assertEquals(Long.valueOf(1000L), message.getFieldAsLong(FixFields.ORDER_QTY));
    }
    
    @Test
    public void testEncodeMessage() {
        FixMessage message = new FixMessage(FixMessageTypes.HEARTBEAT);
        message.setField(FixFields.BEGIN_STRING, "FIX.4.4")
               .setField(FixFields.SENDER_COMP_ID, "CLIENT")
               .setField(FixFields.TARGET_COMP_ID, "SERVER")
               .setField(FixFields.MSG_SEQ_NUM, 1)
               .setField(FixFields.SENDING_TIME, "20231201-10:30:00.000");
        
        String encoded = message.encode();
        
        assertNotNull(encoded);
        assertTrue(encoded.contains("8=FIX.4.4"));
        assertTrue(encoded.contains("35=0"));
        assertTrue(encoded.contains("49=CLIENT"));
        assertTrue(encoded.contains("56=SERVER"));
        assertTrue(encoded.contains("34=1"));
        assertTrue(encoded.contains("10="));
    }
    
    @Test
    public void testHasField() {
        FixMessage message = new FixMessage();
        message.setField(FixFields.SYMBOL, "AAPL");
        
        assertTrue(message.hasField(FixFields.SYMBOL));
        assertFalse(message.hasField(FixFields.PRICE));
    }
}
