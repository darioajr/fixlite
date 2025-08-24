package com.fixlite.authorization;

import com.fixlite.protocol.FixMessage;
import com.fixlite.protocol.FixFields;
import com.fixlite.session.FixSession;

/**
 * Interceptador para validar autorização de mensagens FIX entre participantes.
 */
public class MessageAuthorizationInterceptor {
    
    private final ParticipantPermissionRepository permissionRepository;
    
    public MessageAuthorizationInterceptor(ParticipantPermissionRepository permissionRepository) {
        this.permissionRepository = permissionRepository;
    }
    
    /**
     * Valida se a mensagem pode ser enviada entre os participantes.
     */
    public AuthorizationResult validateMessage(FixSession senderSession, FixMessage message) {
        String senderCompId = senderSession.getSenderCompId();
        String targetCompId = message.getField(FixFields.TARGET_COMP_ID);
        String messageType = message.getField(FixFields.MSG_TYPE);
        String symbol = message.getField(FixFields.SYMBOL); // Pode ser null
        
        // Validações básicas
        if (senderCompId == null || targetCompId == null || messageType == null) {
            return AuthorizationResult.denied("Campos obrigatórios ausentes");
        }
        
        // Verificar se é mensagem administrativa (sempre permitida)
        if (isAdministrativeMessage(messageType)) {
            return AuthorizationResult.allowed("Mensagem administrativa");
        }
        
        // Verificar permissão no repositório
        boolean allowed = permissionRepository.isMessageAllowed(
            senderCompId, targetCompId, messageType, symbol);
        
        if (allowed) {
            return AuthorizationResult.allowed(String.format(
                "Permissão concedida: %s -> %s [%s]", 
                senderCompId, targetCompId, messageType));
        } else {
            return AuthorizationResult.denied(String.format(
                "Permissão negada: %s -> %s [%s] símbolo=%s", 
                senderCompId, targetCompId, messageType, symbol));
        }
    }
    
    /**
     * Verifica se é mensagem administrativa que sempre deve ser permitida.
     */
    private boolean isAdministrativeMessage(String messageType) {
        return switch (messageType) {
            case "A" -> true; // Logon
            case "5" -> true; // Logout
            case "0" -> true; // Heartbeat
            case "1" -> true; // Test Request
            case "2" -> true; // Resend Request
            case "4" -> true; // Sequence Reset
            case "3" -> true; // Reject
            default -> false;
        };
    }
    
    /**
     * Resultado da validação de autorização.
     */
    public static class AuthorizationResult {
        private final boolean allowed;
        private final String reason;
        
        private AuthorizationResult(boolean allowed, String reason) {
            this.allowed = allowed;
            this.reason = reason;
        }
        
        public static AuthorizationResult allowed(String reason) {
            return new AuthorizationResult(true, reason);
        }
        
        public static AuthorizationResult denied(String reason) {
            return new AuthorizationResult(false, reason);
        }
        
        public boolean isAllowed() { return allowed; }
        public String getReason() { return reason; }
        
        @Override
        public String toString() {
            return String.format("AuthorizationResult[allowed=%s, reason='%s']", allowed, reason);
        }
    }
}
