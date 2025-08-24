package com.fixlite.protocol;

/**
 * FIX protocol version enumeration.
 * Defines supported FIX protocol versions and their characteristics.
 */
public enum FixVersion {
    
    FIX_4_4("FIX.4.4", "4.4", "FIX.4.4"),
    FIX_5_0_SP2("FIXT.1.1", "5.0SP2", "FIXT.1.1"),
    CUSTOM("CUSTOM", "CUSTOM", "CUSTOM");
    
    private final String beginString;
    private final String version;
    private final String transportVersion;
    
    FixVersion(String beginString, String version, String transportVersion) {
        this.beginString = beginString;
        this.version = version;
        this.transportVersion = transportVersion;
    }
    
    public String getBeginString() {
        return beginString;
    }
    
    public String getVersion() {
        return version;
    }
    
    public String getTransportVersion() {
        return transportVersion;
    }
    
    /**
     * Determines if this version uses FIXT transport layer.
     */
    public boolean usesFixtTransport() {
        return transportVersion.startsWith("FIXT");
    }
    
    /**
     * Gets the FIX version from begin string.
     */
    public static FixVersion fromBeginString(String beginString) {
        for (FixVersion version : values()) {
            if (version.getBeginString().equals(beginString)) {
                return version;
            }
        }
        return CUSTOM;
    }
    
    /**
     * Checks if the version supports a specific feature.
     */
    public boolean supportsFeature(FixFeature feature) {
        switch (this) {
            case FIX_4_4:
                return feature.getSupportedSince().ordinal() <= FIX_4_4.ordinal();
            case FIX_5_0_SP2:
                return feature.getSupportedSince().ordinal() <= FIX_5_0_SP2.ordinal();
            case CUSTOM:
                return true; // Custom versions support all features
            default:
                return false;
        }
    }
    
    /**
     * FIX protocol features and their introduction versions.
     */
    public enum FixFeature {
        BASIC_TRADING(FIX_4_4),
        ENHANCED_SECURITY(FIX_4_4),
        FIXT_TRANSPORT(FIX_5_0_SP2),
        COMPONENT_BLOCKS(FIX_5_0_SP2),
        REPEATING_GROUPS(FIX_4_4),
        CUSTOM_FIELDS(FIX_4_4);
        
        private final FixVersion supportedSince;
        
        FixFeature(FixVersion supportedSince) {
            this.supportedSince = supportedSince;
        }
        
        public FixVersion getSupportedSince() {
            return supportedSince;
        }
    }
}
