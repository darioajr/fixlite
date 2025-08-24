package com.fixlite.protocol;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Custom FIX model for defining proprietary message types and fields.
 * Allows extending standard FIX protocol with custom implementations.
 */
public class CustomFixModel {
    
    private final String modelName;
    private final FixVersion baseVersion;
    private final Map<String, CustomMessageType> customMessageTypes;
    private final Map<Integer, CustomFieldDefinition> customFields;
    private final Set<String> supportedMessageTypes;
    
    public CustomFixModel(String modelName, FixVersion baseVersion) {
        this.modelName = modelName;
        this.baseVersion = baseVersion;
        this.customMessageTypes = new ConcurrentHashMap<>();
        this.customFields = new ConcurrentHashMap<>();
        this.supportedMessageTypes = new HashSet<>();
        
        // Initialize with standard message types
        initializeStandardMessageTypes();
    }
    
    /**
     * Custom message type definition.
     */
    public static class CustomMessageType {
        private final String msgType;
        private final String name;
        private final String description;
        private final Set<Integer> requiredFields;
        private final Set<Integer> optionalFields;
        private final Map<String, Object> properties;
        
        public CustomMessageType(String msgType, String name, String description) {
            this.msgType = msgType;
            this.name = name;
            this.description = description;
            this.requiredFields = new HashSet<>();
            this.optionalFields = new HashSet<>();
            this.properties = new HashMap<>();
        }
        
        public CustomMessageType addRequiredField(int tag) {
            requiredFields.add(tag);
            return this;
        }
        
        public CustomMessageType addOptionalField(int tag) {
            optionalFields.add(tag);
            return this;
        }
        
        public CustomMessageType setProperty(String key, Object value) {
            properties.put(key, value);
            return this;
        }
        
        // Getters
        public String getMsgType() { return msgType; }
        public String getName() { return name; }
        public String getDescription() { return description; }
        public Set<Integer> getRequiredFields() { return requiredFields; }
        public Set<Integer> getOptionalFields() { return optionalFields; }
        public Map<String, Object> getProperties() { return properties; }
    }
    
    /**
     * Custom field definition extending standard field registry.
     */
    public static class CustomFieldDefinition {
        private final int tag;
        private final String name;
        private final FixFieldsRegistry.FieldType type;
        private final String description;
        private final Set<String> validValues;
        private final Map<String, Object> properties;
        
        public CustomFieldDefinition(int tag, String name, FixFieldsRegistry.FieldType type, String description) {
            this.tag = tag;
            this.name = name;
            this.type = type;
            this.description = description;
            this.validValues = new HashSet<>();
            this.properties = new HashMap<>();
        }
        
        public CustomFieldDefinition addValidValue(String value) {
            validValues.add(value);
            return this;
        }
        
        public CustomFieldDefinition setProperty(String key, Object value) {
            properties.put(key, value);
            return this;
        }
        
        // Getters
        public int getTag() { return tag; }
        public String getName() { return name; }
        public FixFieldsRegistry.FieldType getType() { return type; }
        public String getDescription() { return description; }
        public Set<String> getValidValues() { return validValues; }
        public Map<String, Object> getProperties() { return properties; }
        
        public boolean isValidValue(String value) {
            return validValues.isEmpty() || validValues.contains(value);
        }
    }
    
    /**
     * Adds a custom message type to the model.
     */
    public CustomFixModel addMessageType(CustomMessageType messageType) {
        customMessageTypes.put(messageType.getMsgType(), messageType);
        supportedMessageTypes.add(messageType.getMsgType());
        return this;
    }
    
    /**
     * Adds a custom field definition to the model.
     */
    public CustomFixModel addField(CustomFieldDefinition fieldDef) {
        customFields.put(fieldDef.getTag(), fieldDef);
        return this;
    }
    
    /**
     * Creates a custom message type builder.
     */
    public static Builder messageType(String msgType, String name) {
        return new Builder(msgType, name);
    }
    
    /**
     * Creates a custom field definition builder.
     */
    public static FieldBuilder field(int tag, String name, FixFieldsRegistry.FieldType type) {
        return new FieldBuilder(tag, name, type);
    }
    
    /**
     * Builder for custom message types.
     */
    public static class Builder {
        private final CustomMessageType messageType;
        
        public Builder(String msgType, String name) {
            this.messageType = new CustomMessageType(msgType, name, "");
        }
        
        public Builder description(String description) {
            messageType.setProperty("description", description);
            return this;
        }
        
        public Builder requiredField(int tag) {
            messageType.addRequiredField(tag);
            return this;
        }
        
        public Builder optionalField(int tag) {
            messageType.addOptionalField(tag);
            return this;
        }
        
        public Builder property(String key, Object value) {
            messageType.setProperty(key, value);
            return this;
        }
        
        public CustomMessageType build() {
            return messageType;
        }
    }
    
    /**
     * Validates a message against this custom model.
     */
    public ValidationResult validateMessage(FixMessage message) {
        ValidationResult result = new ValidationResult();
        String msgType = message.getMessageType();
        
        if (!isMessageTypeSupported(msgType)) {
            result.addError("Message type " + msgType + " is not supported in model " + modelName);
            return result;
        }
        
        CustomMessageType customMsgType = customMessageTypes.get(msgType);
        if (customMsgType != null) {
            // Validate required fields
            for (int requiredTag : customMsgType.getRequiredFields()) {
                if (!message.hasField(requiredTag)) {
                    result.addError("Missing required field: " + requiredTag);
                }
            }
            
            // Validate field values
            for (Map.Entry<Integer, String> field : message.getFields().entrySet()) {
                int tag = field.getKey();
                String value = field.getValue();
                
                CustomFieldDefinition fieldDef = customFields.get(tag);
                if (fieldDef != null && !fieldDef.isValidValue(value)) {
                    result.addError("Invalid value '" + value + "' for field " + tag + 
                                  ". Valid values: " + fieldDef.getValidValues());
                }
            }
        }
        
        return result;
    }
    
    /**
     * Checks if a message type is supported.
     */
    public boolean isMessageTypeSupported(String msgType) {
        return supportedMessageTypes.contains(msgType);
    }
    
    /**
     * Gets custom message type definition.
     */
    public CustomMessageType getMessageType(String msgType) {
        return customMessageTypes.get(msgType);
    }
    
    /**
     * Gets custom field definition.
     */
    public CustomFieldDefinition getField(int tag) {
        return customFields.get(tag);
    }
    
    /**
     * Gets all custom message types.
     */
    public Map<String, CustomMessageType> getCustomMessageTypes() {
        return new HashMap<>(customMessageTypes);
    }
    
    /**
     * Gets all custom fields.
     */
    public Map<Integer, CustomFieldDefinition> getCustomFields() {
        return new HashMap<>(customFields);
    }
    
    // Getters
    public String getModelName() { return modelName; }
    public FixVersion getBaseVersion() { return baseVersion; }
    
    private void initializeStandardMessageTypes() {
        // Add standard FIX message types
        supportedMessageTypes.add(FixMessageTypes.HEARTBEAT);
        supportedMessageTypes.add(FixMessageTypes.TEST_REQUEST);
        supportedMessageTypes.add(FixMessageTypes.RESEND_REQUEST);
        supportedMessageTypes.add(FixMessageTypes.REJECT);
        supportedMessageTypes.add(FixMessageTypes.SEQUENCE_RESET);
        supportedMessageTypes.add(FixMessageTypes.LOGOUT);
        supportedMessageTypes.add(FixMessageTypes.LOGON);
        supportedMessageTypes.add(FixMessageTypes.NEW_ORDER_SINGLE);
        supportedMessageTypes.add(FixMessageTypes.EXECUTION_REPORT);
        supportedMessageTypes.add(FixMessageTypes.ORDER_CANCEL_REQUEST);
        supportedMessageTypes.add(FixMessageTypes.ORDER_CANCEL_REJECT);
        supportedMessageTypes.add(FixMessageTypes.ORDER_STATUS_REQUEST);
        supportedMessageTypes.add(FixMessageTypes.BUSINESS_MESSAGE_REJECT);
        supportedMessageTypes.add(FixMessageTypes.MARKET_DATA_REQUEST);
        supportedMessageTypes.add(FixMessageTypes.MARKET_DATA_SNAPSHOT);
        supportedMessageTypes.add(FixMessageTypes.MARKET_DATA_INCREMENTAL_REFRESH);
        supportedMessageTypes.add(FixMessageTypes.MARKET_DATA_REQUEST_REJECT);
    }
    
    /**
     * Validation result for custom model validation.
     */
    public static class ValidationResult {
        private final List<String> errors = new ArrayList<>();
        private final List<String> warnings = new ArrayList<>();
        
        public void addError(String error) {
            errors.add(error);
        }
        
        public void addWarning(String warning) {
            warnings.add(warning);
        }
        
        public boolean isValid() {
            return errors.isEmpty();
        }
        
        public List<String> getErrors() {
            return errors;
        }
        
        public List<String> getWarnings() {
            return warnings;
        }
    }
}
