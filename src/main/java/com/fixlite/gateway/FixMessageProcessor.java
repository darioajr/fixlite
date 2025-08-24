package com.fixlite.gateway;

import com.fixlite.protocol.FixFields;
import com.fixlite.protocol.FixMessage;
import com.fixlite.protocol.FixMessageTypes;
import com.fixlite.session.FixSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Processes application-level FIX messages.
 * Handles order management and execution reporting.
 */
public class FixMessageProcessor {
    
    private static final Logger logger = LoggerFactory.getLogger(FixMessageProcessor.class);
    
    private final AtomicLong orderIdGenerator = new AtomicLong(1);
    private final AtomicLong execIdGenerator = new AtomicLong(1);
    
    /**
     * Process incoming FIX message
     */
    public void processMessage(FixSession session, FixMessage message) {
        String messageType = message.getMessageType();
        
        logger.info("Processing {} message for session: {}", messageType, session.getSessionId());
        
        switch (messageType) {
            case FixMessageTypes.NEW_ORDER_SINGLE:
                handleNewOrderSingle(session, message);
                break;
            case FixMessageTypes.ORDER_CANCEL_REQUEST:
                handleOrderCancelRequest(session, message);
                break;
            case FixMessageTypes.ORDER_STATUS_REQUEST:
                handleOrderStatusRequest(session, message);
                break;
            default:
                logger.warn("Unsupported message type: {}", messageType);
                sendBusinessReject(session, message, "Unsupported message type");
                break;
        }
    }
    
    private void handleNewOrderSingle(FixSession session, FixMessage order) {
        try {
            // Extract order fields
            String clOrdId = order.getField(FixFields.CL_ORD_ID);
            String symbol = order.getField(FixFields.SYMBOL);
            String side = order.getField(FixFields.SIDE);
            String orderQty = order.getField(FixFields.ORDER_QTY);
            String ordType = order.getField(FixFields.ORD_TYPE);
            String price = order.getField(FixFields.PRICE);
            String timeInForce = order.getField(FixFields.TIME_IN_FORCE);
            
            // Validate required fields
            if (clOrdId == null || symbol == null || side == null || orderQty == null || ordType == null) {
                sendOrderReject(session, order, "Missing required fields");
                return;
            }
            
            // Generate order ID
            String orderId = String.valueOf(orderIdGenerator.getAndIncrement());
            
            // Send acknowledgment (New)
            sendExecutionReport(session, order, orderId, "0", "0", "0", orderQty, "0");
            
            // Simulate order processing - in real implementation, this would interact with trading engine
            processOrderExecution(session, order, orderId, orderQty, price);
            
        } catch (Exception e) {
            logger.error("Error processing new order single", e);
            sendOrderReject(session, order, "Internal error");
        }
    }
    
    private void processOrderExecution(FixSession session, FixMessage originalOrder, String orderId, String orderQty, String price) {
        // Simulate partial or full execution
        String clOrdId = originalOrder.getField(FixFields.CL_ORD_ID);
        
        try {
            // Parse quantities
            double qty = Double.parseDouble(orderQty);
            double executedQty = qty; // Full execution for simplicity
            double remainingQty = 0;
            
            // Use provided price or simulate market price
            String execPrice = price != null ? price : "100.50";
            
            // Send execution report (Fill)
            sendExecutionReport(session, originalOrder, orderId, "F", "2", 
                              String.valueOf(executedQty), String.valueOf(executedQty), String.valueOf(remainingQty));
            
            logger.info("Order executed: {} shares of {} at {}", executedQty, 
                       originalOrder.getField(FixFields.SYMBOL), execPrice);
            
        } catch (NumberFormatException e) {
            logger.error("Invalid quantity format: {}", orderQty);
            sendOrderReject(session, originalOrder, "Invalid quantity");
        }
    }
    
    private void handleOrderCancelRequest(FixSession session, FixMessage cancelRequest) {
        String clOrdId = cancelRequest.getField(FixFields.CL_ORD_ID);
        String origClOrdId = cancelRequest.getField(371); // OrigClOrdID
        
        if (clOrdId == null || origClOrdId == null) {
            sendCancelReject(session, cancelRequest, "Missing required fields");
            return;
        }
        
        // For simplicity, reject all cancel requests
        sendCancelReject(session, cancelRequest, "Order not found or cannot be cancelled");
    }
    
    private void handleOrderStatusRequest(FixSession session, FixMessage statusRequest) {
        String clOrdId = statusRequest.getField(FixFields.CL_ORD_ID);
        
        if (clOrdId == null) {
            sendBusinessReject(session, statusRequest, "Missing ClOrdID");
            return;
        }
        
        // For simplicity, return unknown order status
        sendBusinessReject(session, statusRequest, "Order not found");
    }
    
    private void sendExecutionReport(FixSession session, FixMessage originalOrder, String orderId, 
                                   String execType, String ordStatus, String lastQty, String cumQty, String leavesQty) {
        FixMessage execReport = session.createMessageWithHeader(FixMessageTypes.EXECUTION_REPORT);
        
        // Copy relevant fields from original order
        execReport.setField(FixFields.CL_ORD_ID, originalOrder.getField(FixFields.CL_ORD_ID))
                  .setField(FixFields.ORDER_ID, orderId)
                  .setField(FixFields.EXEC_ID, String.valueOf(execIdGenerator.getAndIncrement()))
                  .setField(FixFields.EXEC_TYPE, execType)
                  .setField(FixFields.ORD_STATUS, ordStatus)
                  .setField(FixFields.SYMBOL, originalOrder.getField(FixFields.SYMBOL))
                  .setField(FixFields.SIDE, originalOrder.getField(FixFields.SIDE))
                  .setField(FixFields.ORDER_QTY, originalOrder.getField(FixFields.ORDER_QTY))
                  .setField(FixFields.LAST_QTY, lastQty)
                  .setField(FixFields.CUM_QTY, cumQty)
                  .setField(FixFields.LEAVES_QTY, leavesQty);
        
        // Add price if execution
        if ("F".equals(execType) && originalOrder.hasField(FixFields.PRICE)) {
            execReport.setField(FixFields.LAST_PX, originalOrder.getField(FixFields.PRICE))
                      .setField(FixFields.AVG_PX, originalOrder.getField(FixFields.PRICE));
        }
        
        session.sendMessage(execReport);
    }
    
    private void sendOrderReject(FixSession session, FixMessage originalOrder, String reason) {
        FixMessage execReport = session.createMessageWithHeader(FixMessageTypes.EXECUTION_REPORT);
        
        execReport.setField(FixFields.CL_ORD_ID, originalOrder.getField(FixFields.CL_ORD_ID))
                  .setField(FixFields.ORDER_ID, "NONE")
                  .setField(FixFields.EXEC_ID, String.valueOf(execIdGenerator.getAndIncrement()))
                  .setField(FixFields.EXEC_TYPE, "8") // Rejected
                  .setField(FixFields.ORD_STATUS, "8") // Rejected
                  .setField(FixFields.SYMBOL, originalOrder.getField(FixFields.SYMBOL))
                  .setField(FixFields.SIDE, originalOrder.getField(FixFields.SIDE))
                  .setField(FixFields.TEXT, reason);
        
        session.sendMessage(execReport);
    }
    
    private void sendCancelReject(FixSession session, FixMessage cancelRequest, String reason) {
        FixMessage cancelReject = session.createMessageWithHeader(FixMessageTypes.ORDER_CANCEL_REJECT);
        
        cancelReject.setField(FixFields.CL_ORD_ID, cancelRequest.getField(FixFields.CL_ORD_ID))
                   .setField(FixFields.ORDER_ID, "UNKNOWN")
                   .setField(FixFields.ORD_STATUS, "0") // New
                   .setField(FixFields.TEXT, reason);
        
        session.sendMessage(cancelReject);
    }
    
    private void sendBusinessReject(FixSession session, FixMessage originalMessage, String reason) {
        FixMessage businessReject = session.createMessageWithHeader(FixMessageTypes.BUSINESS_MESSAGE_REJECT);
        
        businessReject.setField(FixFields.REF_SEQ_NUM, originalMessage.getField(FixFields.MSG_SEQ_NUM))
                     .setField(FixFields.REF_MSG_TYPE, originalMessage.getMessageType())
                     .setField(FixFields.TEXT, reason);
        
        session.sendMessage(businessReject);
    }
}
