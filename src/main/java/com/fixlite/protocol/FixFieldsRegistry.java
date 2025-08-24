package com.fixlite.protocol;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for FIX fields across different protocol versions.
 * Manages field definitions, validation, and version-specific behavior.
 */
public class FixFieldsRegistry {
    
    private final Map<FixVersion, Map<Integer, FieldDefinition>> versionFields;
    private final Map<String, FieldDefinition> customFields;
    
    public FixFieldsRegistry() {
        this.versionFields = new ConcurrentHashMap<>();
        this.customFields = new ConcurrentHashMap<>();
        initializeStandardFields();
    }
    
    /**
     * Field definition containing metadata about a FIX field.
     */
    public static class FieldDefinition {
        private final int tag;
        private final String name;
        private final FieldType type;
        private final boolean required;
        private final Set<FixVersion> supportedVersions;
        private final String description;
        
        public FieldDefinition(int tag, String name, FieldType type, boolean required, 
                             Set<FixVersion> supportedVersions, String description) {
            this.tag = tag;
            this.name = name;
            this.type = type;
            this.required = required;
            this.supportedVersions = new HashSet<>(supportedVersions);
            this.description = description;
        }
        
        // Getters
        public int getTag() { return tag; }
        public String getName() { return name; }
        public FieldType getType() { return type; }
        public boolean isRequired() { return required; }
        public Set<FixVersion> getSupportedVersions() { return supportedVersions; }
        public String getDescription() { return description; }
        
        public boolean isSupportedIn(FixVersion version) {
            return supportedVersions.contains(version) || supportedVersions.contains(FixVersion.CUSTOM);
        }
    }
    
    /**
     * FIX field data types.
     */
    public enum FieldType {
        INT, FLOAT, STRING, CHAR, BOOLEAN, UTCTIMESTAMP, UTCDATEONLY, UTCTIMEONLY,
        LENGTH, SEQNUM, NUMINGROUP, PERCENTAGE, PRICE, QTY, CURRENCY, EXCHANGE,
        LOCALMKTDATE, DATA, MULTIPLEVALUESTRING, COUNTRY, TZTIMEONLY, TZTIMESTAMP
    }
    
    /**
     * Gets field definition for a specific version.
     */
    public FieldDefinition getFieldDefinition(int tag, FixVersion version) {
        Map<Integer, FieldDefinition> fields = versionFields.get(version);
        if (fields != null) {
            FieldDefinition field = fields.get(tag);
            if (field != null && field.isSupportedIn(version)) {
                return field;
            }
        }
        
        // Check custom fields
        String customKey = version.name() + "_" + tag;
        return customFields.get(customKey);
    }
    
    /**
     * Registers a custom field for a specific version.
     */
    public void registerCustomField(FixVersion version, FieldDefinition fieldDef) {
        String customKey = version.name() + "_" + fieldDef.getTag();
        customFields.put(customKey, fieldDef);
        
        // Also add to version-specific map
        versionFields.computeIfAbsent(version, k -> new ConcurrentHashMap<>())
                    .put(fieldDef.getTag(), fieldDef);
    }
    
    /**
     * Validates if a field is supported in the given version.
     */
    public boolean isFieldSupported(int tag, FixVersion version) {
        return getFieldDefinition(tag, version) != null;
    }
    
    /**
     * Gets all fields supported in a version.
     */
    public Map<Integer, FieldDefinition> getFieldsForVersion(FixVersion version) {
        return versionFields.getOrDefault(version, Collections.emptyMap());
    }
    
    /**
     * Initialize standard FIX fields for all supported versions.
     */
    private void initializeStandardFields() {
        // FIX 4.4 Fields
        initializeFix44Fields();
        
        // FIX 5.0 SP2 Fields (includes 4.4 + new fields)
        initializeFix50SP2Fields();
    }
    
    private void initializeFix44Fields() {
        Set<FixVersion> fix44Versions = Set.of(FixVersion.FIX_4_4, FixVersion.FIX_5_0_SP2);
        Map<Integer, FieldDefinition> fix44Fields = new ConcurrentHashMap<>();
        
        // Standard Header Fields
        fix44Fields.put(8, new FieldDefinition(8, "BeginString", FieldType.STRING, true, fix44Versions, "Protocol version"));
        fix44Fields.put(9, new FieldDefinition(9, "BodyLength", FieldType.LENGTH, true, fix44Versions, "Message body length"));
        fix44Fields.put(35, new FieldDefinition(35, "MsgType", FieldType.STRING, true, fix44Versions, "Message type"));
        fix44Fields.put(34, new FieldDefinition(34, "MsgSeqNum", FieldType.SEQNUM, true, fix44Versions, "Message sequence number"));
        fix44Fields.put(49, new FieldDefinition(49, "SenderCompID", FieldType.STRING, true, fix44Versions, "Sender company ID"));
        fix44Fields.put(56, new FieldDefinition(56, "TargetCompID", FieldType.STRING, true, fix44Versions, "Target company ID"));
        fix44Fields.put(52, new FieldDefinition(52, "SendingTime", FieldType.UTCTIMESTAMP, true, fix44Versions, "Sending time"));
        fix44Fields.put(10, new FieldDefinition(10, "CheckSum", FieldType.STRING, true, fix44Versions, "Message checksum"));
        
        // Session Level Fields
        fix44Fields.put(98, new FieldDefinition(98, "EncryptMethod", FieldType.INT, false, fix44Versions, "Encryption method"));
        fix44Fields.put(108, new FieldDefinition(108, "HeartBtInt", FieldType.INT, false, fix44Versions, "Heartbeat interval"));
        fix44Fields.put(112, new FieldDefinition(112, "TestReqID", FieldType.STRING, false, fix44Versions, "Test request ID"));
        fix44Fields.put(553, new FieldDefinition(553, "Username", FieldType.STRING, false, fix44Versions, "Username"));
        fix44Fields.put(554, new FieldDefinition(554, "Password", FieldType.STRING, false, fix44Versions, "Password"));
        fix44Fields.put(141, new FieldDefinition(141, "ResetSeqNumFlag", FieldType.BOOLEAN, false, fix44Versions, "Reset sequence number flag"));
        
        // Application Level Fields
        fix44Fields.put(11, new FieldDefinition(11, "ClOrdID", FieldType.STRING, false, fix44Versions, "Client order ID"));
        fix44Fields.put(37, new FieldDefinition(37, "OrderID", FieldType.STRING, false, fix44Versions, "Order ID"));
        fix44Fields.put(17, new FieldDefinition(17, "ExecID", FieldType.STRING, false, fix44Versions, "Execution ID"));
        fix44Fields.put(150, new FieldDefinition(150, "ExecType", FieldType.CHAR, false, fix44Versions, "Execution type"));
        fix44Fields.put(39, new FieldDefinition(39, "OrdStatus", FieldType.CHAR, false, fix44Versions, "Order status"));
        fix44Fields.put(55, new FieldDefinition(55, "Symbol", FieldType.STRING, false, fix44Versions, "Symbol"));
        fix44Fields.put(54, new FieldDefinition(54, "Side", FieldType.CHAR, false, fix44Versions, "Side"));
        fix44Fields.put(38, new FieldDefinition(38, "OrderQty", FieldType.QTY, false, fix44Versions, "Order quantity"));
        fix44Fields.put(40, new FieldDefinition(40, "OrdType", FieldType.CHAR, false, fix44Versions, "Order type"));
        fix44Fields.put(44, new FieldDefinition(44, "Price", FieldType.PRICE, false, fix44Versions, "Price"));
        fix44Fields.put(59, new FieldDefinition(59, "TimeInForce", FieldType.CHAR, false, fix44Versions, "Time in force"));
        fix44Fields.put(32, new FieldDefinition(32, "LastQty", FieldType.QTY, false, fix44Versions, "Last quantity"));
        fix44Fields.put(31, new FieldDefinition(31, "LastPx", FieldType.PRICE, false, fix44Versions, "Last price"));
        fix44Fields.put(151, new FieldDefinition(151, "LeavesQty", FieldType.QTY, false, fix44Versions, "Leaves quantity"));
        fix44Fields.put(14, new FieldDefinition(14, "CumQty", FieldType.QTY, false, fix44Versions, "Cumulative quantity"));
        fix44Fields.put(6, new FieldDefinition(6, "AvgPx", FieldType.PRICE, false, fix44Versions, "Average price"));
        
        // Reject Fields
        fix44Fields.put(45, new FieldDefinition(45, "RefSeqNum", FieldType.SEQNUM, false, fix44Versions, "Reference sequence number"));
        fix44Fields.put(371, new FieldDefinition(371, "RefTagID", FieldType.INT, false, fix44Versions, "Reference tag ID"));
        fix44Fields.put(372, new FieldDefinition(372, "RefMsgType", FieldType.STRING, false, fix44Versions, "Reference message type"));
        fix44Fields.put(373, new FieldDefinition(373, "SessionRejectReason", FieldType.INT, false, fix44Versions, "Session reject reason"));
        fix44Fields.put(58, new FieldDefinition(58, "Text", FieldType.STRING, false, fix44Versions, "Text"));
        
        versionFields.put(FixVersion.FIX_4_4, fix44Fields);
    }
    
    private void initializeFix50SP2Fields() {
        // Start with FIX 4.4 fields
        Map<Integer, FieldDefinition> fix50Fields = new ConcurrentHashMap<>(versionFields.get(FixVersion.FIX_4_4));
        Set<FixVersion> fix50Versions = Set.of(FixVersion.FIX_5_0_SP2);
        
        // Add FIX 5.0 SP2 specific fields
        fix50Fields.put(1128, new FieldDefinition(1128, "ApplVerID", FieldType.STRING, false, fix50Versions, "Application version ID"));
        fix50Fields.put(1129, new FieldDefinition(1129, "CstmApplVerID", FieldType.STRING, false, fix50Versions, "Custom application version ID"));
        fix50Fields.put(1137, new FieldDefinition(1137, "DefaultApplVerID", FieldType.STRING, false, fix50Versions, "Default application version ID"));
        fix50Fields.put(1409, new FieldDefinition(1409, "SessionStatus", FieldType.INT, false, fix50Versions, "Session status"));
        fix50Fields.put(1410, new FieldDefinition(1410, "SessionRejectReason", FieldType.INT, false, fix50Versions, "Session reject reason"));
        
        // Enhanced order fields
        fix50Fields.put(1, new FieldDefinition(1, "Account", FieldType.STRING, false, fix50Versions, "Account"));
        fix50Fields.put(526, new FieldDefinition(526, "SecondaryClOrdID", FieldType.STRING, false, fix50Versions, "Secondary client order ID"));
        fix50Fields.put(1080, new FieldDefinition(1080, "RefOrderID", FieldType.STRING, false, fix50Versions, "Reference order ID"));
        fix50Fields.put(1081, new FieldDefinition(1081, "RefOrderIDSource", FieldType.CHAR, false, fix50Versions, "Reference order ID source"));
        
        // Component block fields
        fix50Fields.put(453, new FieldDefinition(453, "NoPartyIDs", FieldType.NUMINGROUP, false, fix50Versions, "Number of party IDs"));
        fix50Fields.put(448, new FieldDefinition(448, "PartyID", FieldType.STRING, false, fix50Versions, "Party ID"));
        fix50Fields.put(447, new FieldDefinition(447, "PartyIDSource", FieldType.CHAR, false, fix50Versions, "Party ID source"));
        fix50Fields.put(452, new FieldDefinition(452, "PartyRole", FieldType.INT, false, fix50Versions, "Party role"));
        
        versionFields.put(FixVersion.FIX_5_0_SP2, fix50Fields);
    }
}
