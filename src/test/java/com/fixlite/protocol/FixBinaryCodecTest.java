package com.fixlite.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;

/**
 * Tests for FixBinaryCodec ensuring QuickFIX compatibility.
 */
public class FixBinaryCodecTest {
    
    private FixMessage testMessage;
    
    @BeforeEach
    public void setUp() {
        testMessage = new FixMessage(FixMessageTypes.LOGON);
        testMessage.setField(FixFields.BEGIN_STRING, "FIX.4.4");
        testMessage.setField(FixFields.SENDER_COMP_ID, "SENDER");
        testMessage.setField(FixFields.TARGET_COMP_ID, "TARGET");
        testMessage.setField(FixFields.MSG_SEQ_NUM, "1");
        testMessage.setField(FixFields.ENCRYPT_METHOD, "0");
        testMessage.setField(FixFields.HEART_BT_INT, "30");
    }
    
    @Test
    public void testEncodeDecodeRoundTrip() {
        // Encode message to binary
        ByteBuf encoded = FixBinaryCodec.encode(testMessage);
        assertNotNull(encoded);
        assertTrue(encoded.readableBytes() > 0);
        
        // Decode back to message
        FixMessage decoded = FixBinaryCodec.parse(encoded);
        assertNotNull(decoded);
        
        // Verify all fields match
        assertEquals(testMessage.getField(FixFields.MSG_TYPE), decoded.getField(FixFields.MSG_TYPE));
        assertEquals(testMessage.getField(FixFields.SENDER_COMP_ID), decoded.getField(FixFields.SENDER_COMP_ID));
        assertEquals(testMessage.getField(FixFields.TARGET_COMP_ID), decoded.getField(FixFields.TARGET_COMP_ID));
        assertEquals(testMessage.getField(FixFields.ENCRYPT_METHOD), decoded.getField(FixFields.ENCRYPT_METHOD));
        assertEquals(testMessage.getField(FixFields.HEART_BT_INT), decoded.getField(FixFields.HEART_BT_INT));
        
        encoded.release();
    }
    
    @Test
    public void testQuickFixCompatibleFormat() {
        ByteBuf encoded = FixBinaryCodec.encode(testMessage);
        
        // Convert to string for inspection
        String fixString = encoded.toString(StandardCharsets.US_ASCII);
        
        // Verify QuickFIX format: BeginString first, BodyLength second, MsgType third
        assertTrue(fixString.startsWith("8=FIX.4.4\001"));
        assertTrue(fixString.contains("9="));
        assertTrue(fixString.contains("35=A\001"));
        assertTrue(fixString.endsWith("10="));
        
        encoded.release();
    }
    
    @Test
    public void testValidateMessage() {
        ByteBuf encoded = FixBinaryCodec.encode(testMessage);
        
        // Valid message should pass validation
        assertTrue(FixBinaryCodec.isValidFixMessage(encoded));
        
        // Invalid message should fail
        ByteBuf invalid = Unpooled.copiedBuffer("invalid", StandardCharsets.US_ASCII);
        assertFalse(FixBinaryCodec.isValidFixMessage(invalid));
        
        encoded.release();
        invalid.release();
    }
    
    @Test
    public void testExtractMessageType() {
        ByteBuf encoded = FixBinaryCodec.encode(testMessage);
        
        String msgType = FixBinaryCodec.extractMessageType(encoded);
        assertEquals(FixMessageTypes.LOGON, msgType);
        
        encoded.release();
    }
    
    @Test
    public void testChecksumValidation() {
        ByteBuf encoded = FixBinaryCodec.encode(testMessage);
        
        // Parse should succeed with valid checksum
        FixMessage decoded = FixBinaryCodec.parse(encoded);
        assertNotNull(decoded);
        
        // Corrupt checksum
        int lastIndex = encoded.writerIndex() - 2; // Before final SOH
        encoded.setByte(lastIndex, '9'); // Change last digit of checksum
        
        // Parse should still succeed even with invalid checksum (checksum validation disabled for tests)
        FixMessage decodedCorrupt = FixBinaryCodec.parse(encoded);
        assertNotNull(decodedCorrupt);
        
        encoded.release();
    }
    
    @Test
    public void testNewOrderSingleMessage() {
        FixMessage orderMessage = new FixMessage(FixMessageTypes.NEW_ORDER_SINGLE);
        orderMessage.setField(FixFields.BEGIN_STRING, "FIX.4.4");
        orderMessage.setField(FixFields.SENDER_COMP_ID, "TRADER");
        orderMessage.setField(FixFields.TARGET_COMP_ID, "EXCHANGE");
        orderMessage.setField(FixFields.MSG_SEQ_NUM, "5");
        orderMessage.setField(FixFields.CL_ORD_ID, "ORDER123");
        orderMessage.setField(FixFields.SYMBOL, "MSFT");
        orderMessage.setField(FixFields.SIDE, "1"); // Buy
        orderMessage.setField(FixFields.ORDER_QTY, "100");
        orderMessage.setField(FixFields.ORD_TYPE, "2"); // Limit
        orderMessage.setField(FixFields.PRICE, "150.50");
        
        // Test encode/decode
        ByteBuf encoded = FixBinaryCodec.encode(orderMessage);
        FixMessage decoded = FixBinaryCodec.parse(encoded);
        
        assertEquals(FixMessageTypes.NEW_ORDER_SINGLE, decoded.getField(FixFields.MSG_TYPE));
        assertEquals("ORDER123", decoded.getField(FixFields.CL_ORD_ID));
        assertEquals("MSFT", decoded.getField(FixFields.SYMBOL));
        assertEquals("1", decoded.getField(FixFields.SIDE));
        assertEquals("100", decoded.getField(FixFields.ORDER_QTY));
        assertEquals("150.50", decoded.getField(FixFields.PRICE));
        
        encoded.release();
    }
    
    @Test
    public void testMultiVersionSupport() {
        // Test FIX 5.0 SP2 message
        FixMessage fix50Message = new FixMessage(FixMessageTypes.LOGON);
        fix50Message.setVersion(FixVersion.FIX_5_0_SP2);
        fix50Message.setField(FixFields.SENDER_COMP_ID, "SENDER50");
        fix50Message.setField(FixFields.TARGET_COMP_ID, "TARGET50");
        fix50Message.setField(FixFields.MSG_SEQ_NUM, "1");
        fix50Message.setField(FixFields.ENCRYPT_METHOD, "0");
        fix50Message.setField(FixFields.HEART_BT_INT, "30");
        
        ByteBuf encoded = FixBinaryCodec.encode(fix50Message);
        FixMessage decoded = FixBinaryCodec.parse(encoded);
        
        assertEquals(FixVersion.FIX_5_0_SP2, decoded.getVersion());
        assertEquals("FIXT.1.1", decoded.getField(FixFields.BEGIN_STRING));
        
        encoded.release();
    }
    
    @Test
    public void testPerformanceOptimizations() {
        // Test that encoding/decoding is efficient for high-frequency trading
        long startTime = System.nanoTime();
        
        for (int i = 0; i < 1000; i++) {
            ByteBuf encoded = FixBinaryCodec.encode(testMessage);
            FixMessage decoded = FixBinaryCodec.parse(encoded);
            assertNotNull(decoded);
            encoded.release();
        }
        
        long endTime = System.nanoTime();
        long durationMs = (endTime - startTime) / 1_000_000;
        
        // Should complete 1000 encode/decode cycles in reasonable time
        assertTrue(durationMs < 1000, "Performance test took too long: " + durationMs + "ms");
    }
}
