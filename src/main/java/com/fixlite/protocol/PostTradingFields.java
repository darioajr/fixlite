package com.fixlite.protocol;

/**
 * Campos FIX específicos para ambiente pós-trading.
 * Inclui campos para clearing, settlement, posições e colateral.
 */
public class PostTradingFields {
    
    // Trade Confirmation Fields
    public static final int CONFIRM_ID = 859;              // ConfirmID
    public static final int CONFIRM_TYPE = 773;            // ConfirmType
    public static final int CONFIRM_STATUS = 665;          // ConfirmStatus
    public static final int CONFIRM_TRANS_TYPE = 666;      // ConfirmTransType
    public static final int CONFIRM_REF_ID = 772;          // ConfirmRefID
    public static final int CONFIRM_REQ_ID = 859;          // ConfirmReqID
    
    // Allocation Fields
    public static final int ALLOC_ID = 70;                 // AllocID
    public static final int ALLOC_TRANS_TYPE = 71;         // AllocTransType
    public static final int ALLOC_TYPE = 626;              // AllocType
    public static final int ALLOC_STATUS = 87;             // AllocStatus
    public static final int ALLOC_REJ_CODE = 88;           // AllocRejCode
    public static final int ALLOC_REPORT_ID = 755;         // AllocReportID
    public static final int ALLOC_REPORT_TYPE = 794;       // AllocReportType
    public static final int ALLOC_ACCOUNT = 79;            // AllocAccount
    public static final int ALLOC_QTY = 80;                // AllocQty
    public static final int ALLOC_PRICE = 366;             // AllocPrice
    
    // Settlement Fields
    public static final int SETTL_TYPE = 63;               // SettlType
    public static final int SETTL_DATE = 64;               // SettlDate
    public static final int SETTL_CURRENCY = 120;          // SettlCurrency
    public static final int SETTL_INST_ID = 162;           // SettlInstID
    public static final int SETTL_INST_TRANS_TYPE = 163;   // SettlInstTransType
    public static final int SETTL_INST_REF_ID = 214;       // SettlInstRefID
    public static final int SETTL_INST_MODE = 160;         // SettlInstMode
    public static final int SETTL_INST_SOURCE = 165;       // SettlInstSource
    public static final int SETTL_LOCATION = 166;          // SettlLocation
    public static final int SETTL_DELIVERY_TYPE = 172;     // SettlDeliveryType
    public static final int SETTL_DEPOSITORY = 173;        // SettlDepository
    
    // Position Fields
    public static final int POS_MAINT_RPT_ID = 721;        // PosMaintRptID
    public static final int POS_REQ_ID = 710;              // PosReqID
    public static final int POS_REQ_TYPE = 724;            // PosReqType
    public static final int POS_REQ_RESULT = 728;          // PosReqResult
    public static final int POS_TYPE = 703;                // PosType
    public static final int LONG_QTY = 704;                // LongQty
    public static final int SHORT_QTY = 705;               // ShortQty
    public static final int POS_QTY_STATUS = 706;          // PosQtyStatus
    public static final int POS_AMT = 708;                 // PosAmt
    public static final int POS_AMT_TYPE = 707;            // PosAmtType
    public static final int CLEARING_BUSINESS_DATE = 715;  // ClearingBusinessDate
    
    // Trade Capture Fields
    public static final int TRADE_REPORT_ID = 571;         // TradeReportID
    public static final int TRADE_REPORT_TYPE = 856;       // TradeReportType
    public static final int TRADE_REPORT_TRANS_TYPE = 487; // TradeReportTransType
    public static final int TRADE_REQUEST_ID = 568;        // TradeRequestID
    public static final int TRADE_REQUEST_TYPE = 569;      // TradeRequestType
    public static final int TRADE_REPORT_REF_ID = 572;     // TradeReportRefID
    public static final int MATCH_STATUS = 573;            // MatchStatus
    public static final int MATCH_TYPE = 574;              // MatchType
    public static final int TRADE_INPUT_SOURCE = 578;      // TradeInputSource
    public static final int TRADE_INPUT_DEVICE = 579;      // TradeInputDevice
    public static final int TRADE_DATE = 75;               // TradeDate
    public static final int TRADE_REPORTING_INDICATOR = 852; // TradeReportingIndicator
    
    // Collateral Fields
    public static final int COLLATERAL_REQ_ID = 894;       // CollateralReqID
    public static final int COLLATERAL_ASSIGNMENT_ID = 902; // CollateralAssignmentID
    public static final int COLLATERAL_RESPONSE_ID = 916;  // CollateralResponseID
    public static final int COLLATERAL_REPORT_ID = 908;    // CollateralReportID
    public static final int COLLATERAL_INQUIRY_ID = 909;   // CollateralInquiryID
    public static final int COLLATERAL_STATUS = 910;       // CollateralStatus
    public static final int TOTAL_NET_VALUE = 900;         // TotalNetValue
    public static final int CASH_OUT_STANDING = 901;       // CashOutstanding
    public static final int SIDE_COLLATERAL_AMT = 1001;    // SideCollateralAmt
    
    // Margin Fields
    public static final int MARGIN_REQ_ID = 1635;          // MarginReqID
    public static final int MARGIN_AMT = 1645;             // MarginAmt
    public static final int MARGIN_AMT_TYPE = 1644;        // MarginAmtType
    public static final int MARGIN_RATIO = 898;            // MarginRatio
    public static final int MARGIN_EXCESS = 899;           // MarginExcess
    public static final int TOTAL_MARGIN_AMT = 1645;       // TotalMarginAmt
    
    // Risk Management Fields
    public static final int RISK_LIMIT_TYPE = 1530;        // RiskLimitType
    public static final int RISK_LIMIT_AMT = 1531;         // RiskLimitAmt
    public static final int RISK_LIMIT_UTILIZATION = 1532; // RiskLimitUtilization
    public static final int RISK_WARNING_LEVEL = 1533;     // RiskWarningLevel
    
    // Regulatory Reporting Fields
    public static final int REGULATORY_TRADE_ID = 1903;    // RegulatoryTradeID
    public static final int REGULATORY_LEG_TRADE_ID = 1907; // RegulatoryLegTradeID
    public static final int REGULATORY_TRADE_DATE = 2332;  // RegulatoryTradeDate
    public static final int REGULATORY_TRADE_TIME = 2333;  // RegulatoryTradeTime
    public static final int TRADE_REPORTING_TRANS_TYPE = 487; // TradeReportingTransType
    
    // Additional Fields
    public static final int TRADE_REPORT_STATUS = 939;     // TradeReportStatus
    public static final int SETTL_STATUS = 1188;           // SettlStatus
    
    // Clearing Fields
    public static final int CLEARING_FIRM = 439;           // ClearingFirm
    public static final int CLEARING_ACCOUNT = 440;        // ClearingAccount
    public static final int CLEARING_INSTRUCTION = 577;    // ClearingInstruction
    public static final int CLEARING_FEE_INDICATOR = 635;  // ClearingFeeIndicator
    
    // Cross Margining Fields
    public static final int CROSS_MARGIN_REQ_ID = 2000;    // CrossMarginReqID (custom)
    public static final int CROSS_MARGIN_RPT_ID = 2001;    // CrossMarginRptID (custom)
    public static final int PORTFOLIO_ID = 2002;           // PortfolioID (custom)
    public static final int CROSS_MARGIN_AMT = 2003;       // CrossMarginAmt (custom)
    
    /**
     * Verifica se um campo é específico de pós-trading.
     */
    public static boolean isPostTradingField(int tag) {
        return (tag >= 70 && tag <= 88) ||      // Allocation fields
               (tag >= 160 && tag <= 173) ||    // Settlement fields
               (tag >= 439 && tag <= 440) ||    // Clearing fields
               (tag >= 487 && tag <= 487) ||    // Trade reporting
               (tag >= 568 && tag <= 579) ||    // Trade capture
               (tag >= 635 && tag <= 635) ||    // Clearing fee
               (tag >= 665 && tag <= 666) ||    // Confirm fields
               (tag >= 703 && tag <= 728) ||    // Position fields
               (tag >= 772 && tag <= 773) ||    // Confirm fields
               (tag >= 852 && tag <= 859) ||    // Trade reporting
               (tag >= 894 && tag <= 916) ||    // Collateral fields
               (tag >= 1530 && tag <= 1533) ||  // Risk fields
               (tag >= 1635 && tag <= 1645) ||  // Margin fields
               (tag >= 1903 && tag <= 1907) ||  // Regulatory fields
               (tag >= 2000 && tag <= 2003) ||  // Cross margin (custom)
               (tag >= 2332 && tag <= 2333);    // Regulatory time fields
    }
    
    /**
     * Retorna descrição do campo pós-trading.
     */
    public static String getFieldDescription(int tag) {
        switch (tag) {
            case CONFIRM_ID: return "Confirmation ID";
            case CONFIRM_TYPE: return "Confirmation Type";
            case CONFIRM_STATUS: return "Confirmation Status";
            case ALLOC_ID: return "Allocation ID";
            case ALLOC_TRANS_TYPE: return "Allocation Transaction Type";
            case ALLOC_QTY: return "Allocation Quantity";
            case SETTL_DATE: return "Settlement Date";
            case SETTL_CURRENCY: return "Settlement Currency";
            case POS_REQ_ID: return "Position Request ID";
            case LONG_QTY: return "Long Quantity";
            case SHORT_QTY: return "Short Quantity";
            case TRADE_REPORT_ID: return "Trade Report ID";
            case TRADE_DATE: return "Trade Date";
            case COLLATERAL_REQ_ID: return "Collateral Request ID";
            case MARGIN_AMT: return "Margin Amount";
            case CLEARING_FIRM: return "Clearing Firm";
            case CLEARING_ACCOUNT: return "Clearing Account";
            default: return "Post-Trading Field " + tag;
        }
    }
}
