package com.fixlite.protocol;

/**
 * Builder for custom field definitions.
 */
public class FieldBuilder {
    private final CustomFixModel.CustomFieldDefinition fieldDef;
    
    public FieldBuilder(int tag, String name, FixFieldsRegistry.FieldType type) {
        this.fieldDef = new CustomFixModel.CustomFieldDefinition(tag, name, type, "");
    }
    
    public FieldBuilder description(String description) {
        fieldDef.setProperty("description", description);
        return this;
    }
    
    public FieldBuilder validValue(String value) {
        fieldDef.addValidValue(value);
        return this;
    }
    
    public FieldBuilder property(String key, Object value) {
        fieldDef.setProperty(key, value);
        return this;
    }
    
    public CustomFixModel.CustomFieldDefinition build() {
        return fieldDef;
    }
}
