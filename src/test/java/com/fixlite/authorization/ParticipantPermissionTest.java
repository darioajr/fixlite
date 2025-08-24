package com.fixlite.authorization;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.Map;

/**
 * Testes para o sistema de autorização de participantes.
 */
public class ParticipantPermissionTest {
    
    private ParticipantPermissionService permissionService;
    
    @BeforeEach
    void setUp() {
        permissionService = new ParticipantPermissionService();
    }
    
    @Test
    void testCreateSimplePermission() {
        // Criar permissão simples
        permissionService.createSimplePermission("BROKER_A", "EXCHANGE_X", "D", "G");
        
        // Verificar se mensagem é permitida
        assertTrue(permissionService.isMessageAllowed("BROKER_A", "EXCHANGE_X", "D", "AAPL"));
        assertTrue(permissionService.isMessageAllowed("BROKER_A", "EXCHANGE_X", "G", "MSFT"));
        
        // Verificar se mensagem não permitida é negada
        assertFalse(permissionService.isMessageAllowed("BROKER_A", "EXCHANGE_X", "F", "AAPL"));
    }
    
    @Test
    void testTradingPermission() {
        // Criar permissão de trading
        permissionService.createTradingPermission("BROKER_B", "EXCHANGE_Y", "AAPL", "MSFT");
        
        // Verificar ordens permitidas
        assertTrue(permissionService.isMessageAllowed("BROKER_B", "EXCHANGE_Y", "D", "AAPL"));
        assertTrue(permissionService.isMessageAllowed("BROKER_B", "EXCHANGE_Y", "G", "MSFT"));
        assertTrue(permissionService.isMessageAllowed("BROKER_B", "EXCHANGE_Y", "F", "AAPL"));
        
        // Verificar símbolo não permitido
        assertFalse(permissionService.isMessageAllowed("BROKER_B", "EXCHANGE_Y", "D", "GOOGL"));
    }
    
    @Test
    void testPostTradingPermission() {
        // Criar permissão pós-trading
        permissionService.createPostTradingPermission("CLEARING_A", "SETTLEMENT_B");
        
        // Verificar mensagens pós-trading
        assertTrue(permissionService.isMessageAllowed("CLEARING_A", "SETTLEMENT_B", "AK", null)); // Confirmation
        assertTrue(permissionService.isMessageAllowed("CLEARING_A", "SETTLEMENT_B", "J", null));  // Allocation
        assertTrue(permissionService.isMessageAllowed("CLEARING_A", "SETTLEMENT_B", "T", null));  // Settlement
    }
    
    @Test
    void testWildcardPermissions() {
        // Criar permissão com wildcard para target
        permissionService.createSimplePermission("ADMIN", "*", "A", "5");
        
        // Verificar se funciona para qualquer target
        assertTrue(permissionService.isMessageAllowed("ADMIN", "BROKER_A", "A", null));
        assertTrue(permissionService.isMessageAllowed("ADMIN", "EXCHANGE_X", "5", null));
        assertTrue(permissionService.isMessageAllowed("ADMIN", "CLEARING_B", "A", null));
    }
    
    @Test
    void testPermissionDisabling() {
        // Criar e depois desabilitar permissão
        permissionService.createSimplePermission("BROKER_C", "EXCHANGE_Z", "D");
        
        // Verificar se está ativa
        assertTrue(permissionService.isMessageAllowed("BROKER_C", "EXCHANGE_Z", "D", "AAPL"));
        
        // Desabilitar
        assertTrue(permissionService.disablePermission("BROKER_C", "EXCHANGE_Z"));
        
        // Verificar se foi desabilitada
        assertFalse(permissionService.isMessageAllowed("BROKER_C", "EXCHANGE_Z", "D", "AAPL"));
    }
    
    @Test
    void testPermissionRemoval() {
        // Criar permissão
        permissionService.createSimplePermission("BROKER_D", "EXCHANGE_W", "D");
        
        // Verificar se existe
        assertTrue(permissionService.isMessageAllowed("BROKER_D", "EXCHANGE_W", "D", "AAPL"));
        
        // Remover
        assertTrue(permissionService.removePermission("BROKER_D", "EXCHANGE_W"));
        
        // Verificar se foi removida
        assertFalse(permissionService.isMessageAllowed("BROKER_D", "EXCHANGE_W", "D", "AAPL"));
        
        // Tentar remover novamente (deve retornar false)
        assertFalse(permissionService.removePermission("BROKER_D", "EXCHANGE_W"));
    }
    
    @Test
    void testAdministrativeMessages() {
        // Mensagens administrativas devem sempre ser permitidas
        MessageAuthorizationInterceptor interceptor = permissionService.getInterceptor();
        
        // Simular sessão e mensagem de logon
        TestFixSession session = new TestFixSession("BROKER_TEST");
        TestFixMessage logonMessage = new TestFixMessage("A", "TARGET_TEST");
        
        MessageAuthorizationInterceptor.AuthorizationResult result = 
            interceptor.validateMessage(session, logonMessage);
        
        assertTrue(result.isAllowed());
        assertTrue(result.getReason().contains("administrativa"));
    }
    
    @Test
    void testTimeBasedPermissions() {
        // Criar permissão com validade
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime future = now.plusHours(1);
        
        permissionService.createPermission("BROKER_TIME", "EXCHANGE_TIME",
            Set.of("D"), Set.of("*"), now, future, "Permissão temporária");
        
        // Deve estar ativa agora
        assertTrue(permissionService.isMessageAllowed("BROKER_TIME", "EXCHANGE_TIME", "D", "AAPL"));
        
        // Criar permissão expirada
        LocalDateTime past = now.minusHours(1);
        permissionService.createPermission("BROKER_EXPIRED", "EXCHANGE_EXPIRED",
            Set.of("D"), Set.of("*"), past, past, "Permissão expirada");
        
        // Não deve estar ativa
        assertFalse(permissionService.isMessageAllowed("BROKER_EXPIRED", "EXCHANGE_EXPIRED", "D", "AAPL"));
    }
    
    @Test
    void testStatistics() {
        // Criar algumas permissões
        permissionService.createSimplePermission("A", "B", "D");
        permissionService.createSimplePermission("C", "D", "G");
        permissionService.createSimplePermission("E", "F", "F");
        
        Map<String, Object> stats = permissionService.getStatistics();
        
        assertTrue(((Number) stats.get("totalPermissions")).longValue() >= 3);
        assertTrue(((Number) stats.get("enabledPermissions")).longValue() >= 3);
        assertTrue(((Number) stats.get("uniqueSenders")).longValue() >= 3);
        assertTrue(((Number) stats.get("uniqueTargets")).longValue() >= 3);
    }
    
    @Test
    void testBulkOperations() {
        // Testar exportação
        Map<String, Object> exported = permissionService.exportPermissionsConfig();
        assertNotNull(exported.get("permissions"));
        assertNotNull(exported.get("exportedAt"));
        
        // Limpar e reimportar
        permissionService = new ParticipantPermissionService(); // Nova instância
        permissionService.loadPermissionsFromConfig(exported);
        
        // Verificar se as permissões padrão foram carregadas
        Map<String, Object> stats = permissionService.getStatistics();
        assertTrue(((Number) stats.get("totalPermissions")).longValue() > 0);
    }
    
    // Classes auxiliares para teste
    private static class TestFixSession extends com.fixlite.session.FixSession {
        public TestFixSession(String senderCompId) {
            super(senderCompId, "TARGET", null);
        }
    }
    
    private static class TestFixMessage extends com.fixlite.protocol.FixMessage {
        private final String msgType;
        private final String targetCompId;
        
        public TestFixMessage(String msgType, String targetCompId) {
            this.msgType = msgType;
            this.targetCompId = targetCompId;
        }
        
        @Override
        public String getField(int tag) {
            if (tag == com.fixlite.protocol.FixFields.MSG_TYPE) return msgType;
            if (tag == com.fixlite.protocol.FixFields.TARGET_COMP_ID) return targetCompId;
            return null;
        }
        
        @Override
        public com.fixlite.protocol.FixMessage setField(int tag, String value) { return this; }
        
        @Override
        public String encode() { return ""; }
        
        @Override
        public boolean hasField(int tag) {
            return tag == com.fixlite.protocol.FixFields.MSG_TYPE || tag == com.fixlite.protocol.FixFields.TARGET_COMP_ID;
        }
    }
}
