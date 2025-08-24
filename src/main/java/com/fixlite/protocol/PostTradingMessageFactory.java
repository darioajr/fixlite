package com.fixlite.protocol;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Factory para criação de mensagens FIX específicas de pós-trading.
 * Inclui confirmações, alocações, settlement e posições.
 */
public class PostTradingMessageFactory {
    
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd-HH:mm:ss.SSS");
    
    /**
     * Cria mensagem de Trade Confirmation.
     */
    public static FixMessage createTradeConfirmation(FixVersion version, String senderCompId, 
                                                   String targetCompId, String confirmId, 
                                                   String tradeId, String symbol, String side, 
                                                   String quantity, String price) {
        FixMessageFactory factory = new FixMessageFactory(version, senderCompId, targetCompId);
        FixMessage confirmation = factory.createMessage(PostTradingMessageTypes.CONFIRMATION);
        confirmation.setField(PostTradingFields.CONFIRM_ID, confirmId);
        confirmation.setField(PostTradingFields.CONFIRM_TYPE, "1"); // Individual
        confirmation.setField(PostTradingFields.CONFIRM_STATUS, "1"); // Received
        confirmation.setField(PostTradingFields.CONFIRM_TRANS_TYPE, "0"); // New
        confirmation.setField(FixFields.TRADE_ID, tradeId);
        confirmation.setField(FixFields.SYMBOL, symbol);
        confirmation.setField(FixFields.SIDE, side);
        confirmation.setField(FixFields.LAST_QTY, quantity);
        confirmation.setField(FixFields.LAST_PX, price);
        confirmation.setField(PostTradingFields.TRADE_DATE, getCurrentDate());
        confirmation.setField(FixFields.SENDING_TIME, getCurrentTimestamp());
        
        return confirmation;
    }
    
    /**
     * Cria mensagem de Allocation Instruction.
     */
    public static FixMessage createAllocationInstruction(FixVersion version, String senderCompId,
                                                       String targetCompId, String allocId,
                                                       String symbol, String side, String quantity) {
        FixMessageFactory factory = new FixMessageFactory(version, senderCompId, targetCompId);
        FixMessage allocation = factory.createMessage(PostTradingMessageTypes.ALLOCATION_INSTRUCTION);
        allocation.setField(PostTradingFields.ALLOC_ID, allocId);
        allocation.setField(PostTradingFields.ALLOC_TRANS_TYPE, "0"); // New
        allocation.setField(PostTradingFields.ALLOC_TYPE, "1"); // Calculated
        allocation.setField(FixFields.SYMBOL, symbol);
        allocation.setField(FixFields.SIDE, side);
        allocation.setField(FixFields.SHARES, quantity);
        allocation.setField(PostTradingFields.TRADE_DATE, getCurrentDate());
        allocation.setField(FixFields.SENDING_TIME, getCurrentTimestamp());
        
        return allocation;
    }
    
    /**
     * Cria mensagem de Settlement Instructions.
     */
    public static FixMessage createSettlementInstructions(FixVersion version, String senderCompId,
                                                         String targetCompId, String settlInstId,
                                                         String symbol, String settlDate, String settlCurrency) {
        FixMessageFactory factory = new FixMessageFactory(version, senderCompId, targetCompId);
        FixMessage settlement = factory.createMessage(PostTradingMessageTypes.SETTLEMENT_INSTRUCTIONS);
        settlement.setField(PostTradingFields.SETTL_INST_ID, settlInstId);
        settlement.setField(PostTradingFields.SETTL_INST_TRANS_TYPE, "N"); // New
        settlement.setField(PostTradingFields.SETTL_INST_MODE, "1"); // Default
        settlement.setField(FixFields.SYMBOL, symbol);
        settlement.setField(PostTradingFields.SETTL_DATE, settlDate);
        settlement.setField(PostTradingFields.SETTL_CURRENCY, settlCurrency);
        settlement.setField(PostTradingFields.SETTL_TYPE, "0"); // Regular
        settlement.setField(FixFields.SENDING_TIME, getCurrentTimestamp());
        
        return settlement;
    }
    
    /**
     * Cria mensagem de Position Report.
     */
    public static FixMessage createPositionReport(FixVersion version, String senderCompId,
                                                 String targetCompId, String posReqId,
                                                 String symbol, String account, String longQty, String shortQty) {
        FixMessageFactory factory = new FixMessageFactory(version, senderCompId, targetCompId);
        FixMessage positionReport = factory.createMessage(PostTradingMessageTypes.POSITION_REPORT);
        positionReport.setField(PostTradingFields.POS_REQ_ID, posReqId);
        positionReport.setField(PostTradingFields.POS_REQ_RESULT, "0"); // Valid request
        positionReport.setField(FixFields.SYMBOL, symbol);
        positionReport.setField(FixFields.ACCOUNT, account);
        positionReport.setField(PostTradingFields.LONG_QTY, longQty);
        positionReport.setField(PostTradingFields.SHORT_QTY, shortQty);
        positionReport.setField(PostTradingFields.POS_QTY_STATUS, "0"); // Submitted
        positionReport.setField(PostTradingFields.CLEARING_BUSINESS_DATE, getCurrentDate());
        positionReport.setField(FixFields.SENDING_TIME, getCurrentTimestamp());
        
        return positionReport;
    }
    
    /**
     * Cria mensagem de Trade Capture Report.
     */
    public static FixMessage createTradeCaptureReport(FixVersion version, String senderCompId,
                                                    String targetCompId, String tradeReportId,
                                                    String symbol, String side, String quantity, 
                                                    String price, String tradeDate) {
        FixMessage tradeCapture = new FixMessage(PostTradingMessageTypes.TRADE_CAPTURE_REPORT);
        tradeCapture.setVersion(version);
        tradeCapture.setField(FixFields.SENDER_COMP_ID, senderCompId);
        tradeCapture.setField(FixFields.TARGET_COMP_ID, targetCompId);
        tradeCapture.setField(PostTradingFields.TRADE_REPORT_ID, tradeReportId);
        tradeCapture.setField(PostTradingFields.TRADE_REPORT_TYPE, "0"); // Submit
        tradeCapture.setField(PostTradingFields.TRADE_REPORT_TRANS_TYPE, "0"); // New
        tradeCapture.setField(FixFields.SYMBOL, symbol);
        tradeCapture.setField(FixFields.SIDE, side);
        tradeCapture.setField(FixFields.LAST_QTY, quantity);
        tradeCapture.setField(FixFields.LAST_PX, price);
        tradeCapture.setField(PostTradingFields.TRADE_DATE, tradeDate);
        tradeCapture.setField(PostTradingFields.MATCH_STATUS, "0"); // Compared, matched or affirmed
        tradeCapture.setField(PostTradingFields.TRADE_INPUT_SOURCE, "1"); // Trade input device
        tradeCapture.setField(FixFields.SENDING_TIME, getCurrentTimestamp());
        
        return tradeCapture;
    }
    
    /**
     * Cria mensagem de Collateral Assignment.
     */
    public static FixMessage createCollateralAssignment(FixVersion version, String senderCompId,
                                                       String targetCompId, String collateralAssignmentId,
                                                       String account, String collateralAmt, String currency) {
        FixMessage collateral = new FixMessage(PostTradingMessageTypes.COLLATERAL_ASSIGNMENT);
        collateral.setVersion(version);
        collateral.setField(FixFields.SENDER_COMP_ID, senderCompId);
        collateral.setField(FixFields.TARGET_COMP_ID, targetCompId);
        collateral.setField(PostTradingFields.COLLATERAL_ASSIGNMENT_ID, collateralAssignmentId);
        collateral.setField(FixFields.ACCOUNT, account);
        collateral.setField(PostTradingFields.SIDE_COLLATERAL_AMT, collateralAmt);
        collateral.setField(FixFields.CURRENCY, currency);
        collateral.setField(PostTradingFields.COLLATERAL_STATUS, "0"); // Unassigned
        collateral.setField(FixFields.SENDING_TIME, getCurrentTimestamp());
        
        return collateral;
    }
    
    /**
     * Cria mensagem de Margin Requirement Report.
     */
    public static FixMessage createMarginRequirementReport(FixVersion version, String senderCompId,
                                                          String targetCompId, String marginReqId,
                                                          String account, String marginAmt, String currency) {
        FixMessage marginReport = new FixMessage(PostTradingMessageTypes.MARGIN_REQUIREMENT_REPORT);
        marginReport.setVersion(version);
        marginReport.setField(FixFields.SENDER_COMP_ID, senderCompId);
        marginReport.setField(FixFields.TARGET_COMP_ID, targetCompId);
        marginReport.setField(PostTradingFields.MARGIN_REQ_ID, marginReqId);
        marginReport.setField(FixFields.ACCOUNT, account);
        marginReport.setField(PostTradingFields.MARGIN_AMT, marginAmt);
        marginReport.setField(PostTradingFields.MARGIN_AMT_TYPE, "1"); // Additional margin
        marginReport.setField(FixFields.CURRENCY, currency);
        marginReport.setField(PostTradingFields.CLEARING_BUSINESS_DATE, getCurrentDate());
        marginReport.setField(FixFields.SENDING_TIME, getCurrentTimestamp());
        
        return marginReport;
    }
    
    /**
     * Cria mensagem de Request for Positions.
     */
    public static FixMessage createRequestForPositions(FixVersion version, String senderCompId,
                                                      String targetCompId, String posReqId,
                                                      String account, String symbol) {
        FixMessage posRequest = new FixMessage(PostTradingMessageTypes.REQUEST_FOR_POSITIONS);
        posRequest.setVersion(version);
        posRequest.setField(FixFields.SENDER_COMP_ID, senderCompId);
        posRequest.setField(FixFields.TARGET_COMP_ID, targetCompId);
        posRequest.setField(PostTradingFields.POS_REQ_ID, posReqId);
        posRequest.setField(PostTradingFields.POS_REQ_TYPE, "0"); // Positions
        posRequest.setField(FixFields.ACCOUNT, account);
        if (symbol != null) {
            posRequest.setField(FixFields.SYMBOL, symbol);
        }
        posRequest.setField(PostTradingFields.CLEARING_BUSINESS_DATE, getCurrentDate());
        posRequest.setField(FixFields.SENDING_TIME, getCurrentTimestamp());
        
        return posRequest;
    }
    
    /**
     * Cria mensagem de Confirmation Ack.
     */
    public static FixMessage createConfirmationAck(FixVersion version, String senderCompId,
                                                  String targetCompId, String confirmId,
                                                  String confirmStatus) {
        FixMessage confirmAck = new FixMessage(PostTradingMessageTypes.CONFIRMATION_ACK);
        confirmAck.setVersion(version);
        confirmAck.setField(FixFields.SENDER_COMP_ID, senderCompId);
        confirmAck.setField(FixFields.TARGET_COMP_ID, targetCompId);
        confirmAck.setField(PostTradingFields.CONFIRM_ID, confirmId);
        confirmAck.setField(PostTradingFields.CONFIRM_STATUS, confirmStatus);
        confirmAck.setField(FixFields.SENDING_TIME, getCurrentTimestamp());
        
        return confirmAck;
    }
    
    /**
     * Cria mensagem de Allocation Report.
     */
    public static FixMessage createAllocationReport(FixVersion version, String senderCompId,
                                                   String targetCompId, String allocReportId,
                                                   String allocId, String allocStatus) {
        FixMessage allocReport = new FixMessage(PostTradingMessageTypes.ALLOCATION_REPORT);
        allocReport.setVersion(version);
        allocReport.setField(FixFields.SENDER_COMP_ID, senderCompId);
        allocReport.setField(FixFields.TARGET_COMP_ID, targetCompId);
        allocReport.setField(PostTradingFields.ALLOC_REPORT_ID, allocReportId);
        allocReport.setField(PostTradingFields.ALLOC_ID, allocId);
        allocReport.setField(PostTradingFields.ALLOC_STATUS, allocStatus);
        allocReport.setField(PostTradingFields.ALLOC_REPORT_TYPE, "2"); // Complete
        allocReport.setField(FixFields.SENDING_TIME, getCurrentTimestamp());
        
        return allocReport;
    }
    
    private static String getCurrentDate() {
        return LocalDate.now().format(DATE_FORMAT);
    }
    
    /**
     * Cria mensagem de Trade Capture Report Ack.
     */
    public static FixMessage createTradeCaptureReportAck(FixVersion version, String senderCompId,
                                                        String targetCompId, String tradeReportId,
                                                        String tradeReportStatus) {
        FixMessageFactory factory = new FixMessageFactory(version, senderCompId, targetCompId);
        FixMessage tradeCaptureAck = factory.createMessage(PostTradingMessageTypes.TRADE_CAPTURE_REPORT_ACK);
        tradeCaptureAck.setField(PostTradingFields.TRADE_REPORT_ID, tradeReportId);
        tradeCaptureAck.setField(PostTradingFields.TRADE_REPORT_STATUS, tradeReportStatus);
        tradeCaptureAck.setField(FixFields.SENDING_TIME, getCurrentTimestamp());
        
        return tradeCaptureAck;
    }
    
    /**
     * Cria mensagem de Collateral Response.
     */
    public static FixMessage createCollateralResponse(FixVersion version, String senderCompId,
                                                     String targetCompId, String collateralAssignmentId,
                                                     String collateralStatus) {
        FixMessageFactory factory = new FixMessageFactory(version, senderCompId, targetCompId);
        FixMessage collateralResponse = factory.createMessage(PostTradingMessageTypes.COLLATERAL_RESPONSE);
        collateralResponse.setField(PostTradingFields.COLLATERAL_ASSIGNMENT_ID, collateralAssignmentId);
        collateralResponse.setField(PostTradingFields.COLLATERAL_STATUS, collateralStatus);
        collateralResponse.setField(FixFields.SENDING_TIME, getCurrentTimestamp());
        
        return collateralResponse;
    }
    
    /**
     * Cria mensagem de Settlement Obligation Report.
     */
    public static FixMessage createSettlementObligationReport(FixVersion version, String senderCompId,
                                                             String targetCompId, String settlInstId,
                                                             String settlStatus) {
        FixMessageFactory factory = new FixMessageFactory(version, senderCompId, targetCompId);
        FixMessage settlObligationReport = factory.createMessage(PostTradingMessageTypes.SETTLEMENT_OBLIGATION_REPORT);
        settlObligationReport.setField(PostTradingFields.SETTL_INST_ID, settlInstId);
        settlObligationReport.setField(PostTradingFields.SETTL_STATUS, settlStatus);
        settlObligationReport.setField(FixFields.SENDING_TIME, getCurrentTimestamp());
        
        return settlObligationReport;
    }
    
    private static String getCurrentTimestamp() {
        return LocalDateTime.now().format(DATETIME_FORMAT);
    }
}
