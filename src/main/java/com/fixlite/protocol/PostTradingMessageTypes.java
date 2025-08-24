package com.fixlite.protocol;

/**
 * Tipos de mensagens FIX específicas para ambiente pós-trading.
 * Inclui clearing, settlement, confirmação e reconciliação.
 */
public class PostTradingMessageTypes {
    
    // Trade Confirmation and Allocation
    public static final String CONFIRMATION = "AK";           // Confirmation
    public static final String CONFIRMATION_ACK = "AU";       // Confirmation Ack
    public static final String CONFIRMATION_REQUEST = "BH";   // Confirmation Request
    public static final String ALLOCATION_INSTRUCTION = "J";  // Allocation Instruction
    public static final String ALLOCATION_ACK = "P";          // Allocation Ack
    public static final String ALLOCATION_REPORT = "AS";      // Allocation Report
    
    // Settlement Instructions
    public static final String SETTLEMENT_INSTRUCTIONS = "T"; // Settlement Instructions
    public static final String SETTLEMENT_INSTRUCTION_REQUEST = "AV"; // Settlement Instruction Request
    public static final String SETTLEMENT_OBLIGATION_REPORT = "BQ"; // Settlement Obligation Report
    
    // Position Reports
    public static final String POSITION_REPORT = "AP";        // Position Report
    public static final String REQUEST_FOR_POSITIONS = "AN";  // Request for Positions
    public static final String POSITION_MAINTENANCE_REQUEST = "AL"; // Position Maintenance Request
    public static final String POSITION_MAINTENANCE_REPORT = "AM"; // Position Maintenance Report
    
    // Trade Capture
    public static final String TRADE_CAPTURE_REPORT = "AE";   // Trade Capture Report
    public static final String TRADE_CAPTURE_REPORT_REQUEST = "AD"; // Trade Capture Report Request
    public static final String TRADE_CAPTURE_REPORT_ACK = "AR"; // Trade Capture Report Ack
    
    // Collateral Management
    public static final String COLLATERAL_REQUEST = "AX";     // Collateral Request
    public static final String COLLATERAL_ASSIGNMENT = "AY";  // Collateral Assignment
    public static final String COLLATERAL_RESPONSE = "AZ";    // Collateral Response
    public static final String COLLATERAL_REPORT = "BA";      // Collateral Report
    public static final String COLLATERAL_INQUIRY = "BB";     // Collateral Inquiry
    public static final String COLLATERAL_INQUIRY_ACK = "BG"; // Collateral Inquiry Ack
    
    // Risk Management
    public static final String USER_REQUEST = "BE";           // User Request
    public static final String USER_RESPONSE = "BF";          // User Response
    public static final String MARGIN_REQUIREMENT_REPORT = "CQ"; // Margin Requirement Report
    public static final String MARGIN_REQUIREMENT_INQUIRY = "CH"; // Margin Requirement Inquiry
    
    // Regulatory Reporting
    public static final String TRADE_REPORTING_REQUEST = "AQ"; // Trade Reporting Request
    public static final String TRADE_REPORTING_ACK = "AT";    // Trade Reporting Ack
    
    // Cross Margining
    public static final String CROSS_MARGIN_REQUEST = "DM";   // Cross Margin Request
    public static final String CROSS_MARGIN_REPORT = "DR";    // Cross Margin Report
    
    // Netting and Clearing
    public static final String NETWORK_COUNTERPARTY_SYSTEM_STATUS_REQUEST = "BC"; // Network Counterparty System Status Request
    public static final String NETWORK_COUNTERPARTY_SYSTEM_STATUS_RESPONSE = "BD"; // Network Counterparty System Status Response
    
    /**
     * Verifica se um tipo de mensagem é específico de pós-trading.
     */
    public static boolean isPostTradingMessage(String msgType) {
        return msgType != null && (
            msgType.equals(CONFIRMATION) ||
            msgType.equals(CONFIRMATION_ACK) ||
            msgType.equals(CONFIRMATION_REQUEST) ||
            msgType.equals(ALLOCATION_INSTRUCTION) ||
            msgType.equals(ALLOCATION_ACK) ||
            msgType.equals(ALLOCATION_REPORT) ||
            msgType.equals(SETTLEMENT_INSTRUCTIONS) ||
            msgType.equals(SETTLEMENT_INSTRUCTION_REQUEST) ||
            msgType.equals(SETTLEMENT_OBLIGATION_REPORT) ||
            msgType.equals(POSITION_REPORT) ||
            msgType.equals(REQUEST_FOR_POSITIONS) ||
            msgType.equals(POSITION_MAINTENANCE_REQUEST) ||
            msgType.equals(POSITION_MAINTENANCE_REPORT) ||
            msgType.equals(TRADE_CAPTURE_REPORT) ||
            msgType.equals(TRADE_CAPTURE_REPORT_REQUEST) ||
            msgType.equals(TRADE_CAPTURE_REPORT_ACK) ||
            msgType.equals(COLLATERAL_REQUEST) ||
            msgType.equals(COLLATERAL_ASSIGNMENT) ||
            msgType.equals(COLLATERAL_RESPONSE) ||
            msgType.equals(COLLATERAL_REPORT) ||
            msgType.equals(COLLATERAL_INQUIRY) ||
            msgType.equals(COLLATERAL_INQUIRY_ACK) ||
            msgType.equals(USER_REQUEST) ||
            msgType.equals(USER_RESPONSE) ||
            msgType.equals(MARGIN_REQUIREMENT_REPORT) ||
            msgType.equals(MARGIN_REQUIREMENT_INQUIRY) ||
            msgType.equals(TRADE_REPORTING_REQUEST) ||
            msgType.equals(TRADE_REPORTING_ACK) ||
            msgType.equals(CROSS_MARGIN_REQUEST) ||
            msgType.equals(CROSS_MARGIN_REPORT) ||
            msgType.equals(NETWORK_COUNTERPARTY_SYSTEM_STATUS_REQUEST) ||
            msgType.equals(NETWORK_COUNTERPARTY_SYSTEM_STATUS_RESPONSE)
        );
    }
    
    /**
     * Retorna descrição do tipo de mensagem pós-trading.
     */
    public static String getMessageDescription(String msgType) {
        switch (msgType) {
            case CONFIRMATION: return "Trade Confirmation";
            case CONFIRMATION_ACK: return "Confirmation Acknowledgment";
            case CONFIRMATION_REQUEST: return "Confirmation Request";
            case ALLOCATION_INSTRUCTION: return "Allocation Instruction";
            case ALLOCATION_ACK: return "Allocation Acknowledgment";
            case ALLOCATION_REPORT: return "Allocation Report";
            case SETTLEMENT_INSTRUCTIONS: return "Settlement Instructions";
            case SETTLEMENT_INSTRUCTION_REQUEST: return "Settlement Instruction Request";
            case SETTLEMENT_OBLIGATION_REPORT: return "Settlement Obligation Report";
            case POSITION_REPORT: return "Position Report";
            case REQUEST_FOR_POSITIONS: return "Request for Positions";
            case POSITION_MAINTENANCE_REQUEST: return "Position Maintenance Request";
            case POSITION_MAINTENANCE_REPORT: return "Position Maintenance Report";
            case TRADE_CAPTURE_REPORT: return "Trade Capture Report";
            case TRADE_CAPTURE_REPORT_REQUEST: return "Trade Capture Report Request";
            case TRADE_CAPTURE_REPORT_ACK: return "Trade Capture Report Acknowledgment";
            case COLLATERAL_REQUEST: return "Collateral Request";
            case COLLATERAL_ASSIGNMENT: return "Collateral Assignment";
            case COLLATERAL_RESPONSE: return "Collateral Response";
            case COLLATERAL_REPORT: return "Collateral Report";
            case COLLATERAL_INQUIRY: return "Collateral Inquiry";
            case COLLATERAL_INQUIRY_ACK: return "Collateral Inquiry Acknowledgment";
            case USER_REQUEST: return "User Request";
            case USER_RESPONSE: return "User Response";
            case MARGIN_REQUIREMENT_REPORT: return "Margin Requirement Report";
            case MARGIN_REQUIREMENT_INQUIRY: return "Margin Requirement Inquiry";
            case TRADE_REPORTING_REQUEST: return "Trade Reporting Request";
            case TRADE_REPORTING_ACK: return "Trade Reporting Acknowledgment";
            case CROSS_MARGIN_REQUEST: return "Cross Margin Request";
            case CROSS_MARGIN_REPORT: return "Cross Margin Report";
            case NETWORK_COUNTERPARTY_SYSTEM_STATUS_REQUEST: return "Network Counterparty System Status Request";
            case NETWORK_COUNTERPARTY_SYSTEM_STATUS_RESPONSE: return "Network Counterparty System Status Response";
            default: return "Unknown Post-Trading Message";
        }
    }
}
