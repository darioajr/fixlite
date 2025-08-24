package com.fixlite.authorization;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * Representa uma permissão entre participantes do sistema FIX.
 */
public class ParticipantPermission {
    
    private final String senderCompId;
    private final String targetCompId;
    private final Set<String> allowedMessageTypes;
    private final Set<String> allowedSymbols;
    private final boolean enabled;
    private final LocalDateTime validFrom;
    private final LocalDateTime validTo;
    private final String ruleDescription;
    
    public ParticipantPermission(String senderCompId, 
                                String targetCompId,
                                Set<String> allowedMessageTypes,
                                Set<String> allowedSymbols,
                                boolean enabled,
                                LocalDateTime validFrom,
                                LocalDateTime validTo,
                                String ruleDescription) {
        this.senderCompId = senderCompId;
        this.targetCompId = targetCompId;
        this.allowedMessageTypes = allowedMessageTypes;
        this.allowedSymbols = allowedSymbols;
        this.enabled = enabled;
        this.validFrom = validFrom;
        this.validTo = validTo;
        this.ruleDescription = ruleDescription;
    }
    
    /**
     * Verifica se a mensagem é permitida entre os participantes.
     */
    public boolean isMessageAllowed(String messageType, String symbol) {
        if (!enabled) {
            return false;
        }
        
        LocalDateTime now = LocalDateTime.now();
        if (validFrom != null && now.isBefore(validFrom)) {
            return false;
        }
        
        if (validTo != null && now.isAfter(validTo)) {
            return false;
        }
        
        // Verificar tipo de mensagem
        if (allowedMessageTypes != null && !allowedMessageTypes.isEmpty()) {
            if (!allowedMessageTypes.contains(messageType) && !allowedMessageTypes.contains("*")) {
                return false;
            }
        }
        
        // Verificar símbolo (se aplicável)
        if (symbol != null && allowedSymbols != null && !allowedSymbols.isEmpty()) {
            if (!allowedSymbols.contains(symbol) && !allowedSymbols.contains("*")) {
                return false;
            }
        }
        
        return true;
    }
    
    // Getters
    public String getSenderCompId() { return senderCompId; }
    public String getTargetCompId() { return targetCompId; }
    public Set<String> getAllowedMessageTypes() { return allowedMessageTypes; }
    public Set<String> getAllowedSymbols() { return allowedSymbols; }
    public boolean isEnabled() { return enabled; }
    public LocalDateTime getValidFrom() { return validFrom; }
    public LocalDateTime getValidTo() { return validTo; }
    public String getRuleDescription() { return ruleDescription; }
    
    @Override
    public String toString() {
        return String.format("Permission[%s -> %s, types=%s, symbols=%s, enabled=%s]",
            senderCompId, targetCompId, allowedMessageTypes, allowedSymbols, enabled);
    }
}
