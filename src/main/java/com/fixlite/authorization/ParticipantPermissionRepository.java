package com.fixlite.authorization;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Repositório para gerenciar permissões entre participantes.
 * Implementação em memória com suporte a persistência futura.
 */
public class ParticipantPermissionRepository {
    
    private final Map<String, ParticipantPermission> permissions = new ConcurrentHashMap<>();
    
    /**
     * Adiciona ou atualiza uma permissão.
     */
    public void savePermission(ParticipantPermission permission) {
        String key = generateKey(permission.getSenderCompId(), permission.getTargetCompId());
        permissions.put(key, permission);
    }
    
    /**
     * Remove uma permissão.
     */
    public boolean removePermission(String senderCompId, String targetCompId) {
        String key = generateKey(senderCompId, targetCompId);
        return permissions.remove(key) != null;
    }
    
    /**
     * Busca permissão específica entre dois participantes.
     */
    public Optional<ParticipantPermission> findPermission(String senderCompId, String targetCompId) {
        String key = generateKey(senderCompId, targetCompId);
        return Optional.ofNullable(permissions.get(key));
    }
    
    /**
     * Lista todas as permissões de um remetente.
     */
    public List<ParticipantPermission> findBySender(String senderCompId) {
        return permissions.values().stream()
            .filter(p -> p.getSenderCompId().equals(senderCompId))
            .collect(Collectors.toList());
    }
    
    /**
     * Lista todas as permissões para um destinatário.
     */
    public List<ParticipantPermission> findByTarget(String targetCompId) {
        return permissions.values().stream()
            .filter(p -> p.getTargetCompId().equals(targetCompId))
            .collect(Collectors.toList());
    }
    
    /**
     * Lista todas as permissões.
     */
    public List<ParticipantPermission> findAll() {
        return new ArrayList<>(permissions.values());
    }
    
    /**
     * Verifica se uma mensagem é permitida entre participantes.
     */
    public boolean isMessageAllowed(String senderCompId, String targetCompId, 
                                   String messageType, String symbol) {
        // Buscar permissão específica
        Optional<ParticipantPermission> permission = findPermission(senderCompId, targetCompId);
        
        if (permission.isPresent()) {
            return permission.get().isMessageAllowed(messageType, symbol);
        }
        
        // Verificar permissão wildcard para o remetente
        Optional<ParticipantPermission> wildcardPermission = findPermission(senderCompId, "*");
        if (wildcardPermission.isPresent()) {
            return wildcardPermission.get().isMessageAllowed(messageType, symbol);
        }
        
        // Por padrão, negar acesso se não houver permissão explícita
        return false;
    }
    
    /**
     * Carrega permissões padrão do sistema.
     */
    public void loadDefaultPermissions() {
        // Clearing House pode enviar para todos
        savePermission(new ParticipantPermission(
            "CLEARING_HOUSE", "*",
            Set.of("AK", "J", "AS", "T"), // Confirmation, Allocation, Settlement
            Set.of("*"),
            true, null, null,
            "Clearing House - Acesso total"
        ));
        
        // CCP pode enviar para clearing houses
        savePermission(new ParticipantPermission(
            "CCP_MAIN", "CLEARING_HOUSE",
            Set.of("AE", "AX", "BE"), // Trade Capture, Collateral, Margin
            Set.of("*"),
            true, null, null,
            "CCP para Clearing - Operações centralizadas"
        ));
        
        // Brokers podem enviar ordens
        savePermission(new ParticipantPermission(
            "BROKER_*", "EXCHANGE",
            Set.of("D", "G", "F"), // NewOrderSingle, OrderCancelRequest, OrderCancelReplaceRequest
            Set.of("AAPL", "MSFT", "GOOGL", "TSLA"),
            true, null, null,
            "Brokers - Ordens de mercado"
        ));
        
        // Settlement systems
        savePermission(new ParticipantPermission(
            "SETTLEMENT_SYS", "*",
            Set.of("AV", "BQ"), // Settlement Instructions
            Set.of("*"),
            true, null, null,
            "Sistema de Settlement - Instruções"
        ));
    }
    
    /**
     * Limpa todas as permissões.
     */
    public void clear() {
        permissions.clear();
    }
    
    /**
     * Retorna estatísticas das permissões.
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalPermissions", permissions.size());
        stats.put("enabledPermissions", permissions.values().stream()
            .mapToLong(p -> p.isEnabled() ? 1 : 0).sum());
        stats.put("uniqueSenders", permissions.values().stream()
            .map(ParticipantPermission::getSenderCompId).distinct().count());
        stats.put("uniqueTargets", permissions.values().stream()
            .map(ParticipantPermission::getTargetCompId).distinct().count());
        return stats;
    }
    
    private String generateKey(String senderCompId, String targetCompId) {
        return senderCompId + "->" + targetCompId;
    }
}
