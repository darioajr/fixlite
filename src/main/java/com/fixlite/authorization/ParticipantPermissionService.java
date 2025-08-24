package com.fixlite.authorization;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Serviço para gerenciar permissões entre participantes do sistema FIX.
 */
public class ParticipantPermissionService {
    
    private final ParticipantPermissionRepository repository;
    private final MessageAuthorizationInterceptor interceptor;
    
    public ParticipantPermissionService() {
        this.repository = new ParticipantPermissionRepository();
        this.interceptor = new MessageAuthorizationInterceptor(repository);
        
        // Carregar permissões padrão
        repository.loadDefaultPermissions();
    }
    
    /**
     * Cria uma nova permissão entre participantes.
     */
    public void createPermission(String senderCompId, String targetCompId,
                               Set<String> messageTypes, Set<String> symbols,
                               LocalDateTime validFrom, LocalDateTime validTo,
                               String description) {
        ParticipantPermission permission = new ParticipantPermission(
            senderCompId, targetCompId, messageTypes, symbols,
            true, validFrom, validTo, description
        );
        repository.savePermission(permission);
    }
    
    /**
     * Cria permissão simples (sempre ativa).
     */
    public void createSimplePermission(String senderCompId, String targetCompId,
                                     String... messageTypes) {
        createPermission(senderCompId, targetCompId,
            Set.of(messageTypes), Set.of("*"), null, null,
            "Permissão simples criada via API");
    }
    
    /**
     * Cria permissão para trading (ordens de mercado).
     */
    public void createTradingPermission(String brokerCompId, String exchangeCompId,
                                      String... symbols) {
        createPermission(brokerCompId, exchangeCompId,
            Set.of("D", "G", "F", "H"), // NewOrder, Cancel, Replace, OrderStatus
            Set.of(symbols), null, null,
            "Permissão de trading para " + brokerCompId);
    }
    
    /**
     * Cria permissão para pós-trading (clearing, settlement).
     */
    public void createPostTradingPermission(String senderCompId, String targetCompId) {
        createPermission(senderCompId, targetCompId,
            Set.of("AK", "J", "AS", "T", "AV", "AE", "AX", "BE"), // Confirmation, Allocation, Settlement, etc.
            Set.of("*"), null, null,
            "Permissão pós-trading para " + senderCompId);
    }
    
    /**
     * Desabilita uma permissão.
     */
    public boolean disablePermission(String senderCompId, String targetCompId) {
        Optional<ParticipantPermission> existing = repository.findPermission(senderCompId, targetCompId);
        if (existing.isPresent()) {
            ParticipantPermission disabled = new ParticipantPermission(
                existing.get().getSenderCompId(),
                existing.get().getTargetCompId(),
                existing.get().getAllowedMessageTypes(),
                existing.get().getAllowedSymbols(),
                false, // disabled
                existing.get().getValidFrom(),
                existing.get().getValidTo(),
                existing.get().getRuleDescription() + " [DESABILITADA]"
            );
            repository.savePermission(disabled);
            return true;
        }
        return false;
    }
    
    /**
     * Remove uma permissão completamente.
     */
    public boolean removePermission(String senderCompId, String targetCompId) {
        return repository.removePermission(senderCompId, targetCompId);
    }
    
    /**
     * Lista permissões de um participante.
     */
    public List<ParticipantPermission> getPermissionsForSender(String senderCompId) {
        return repository.findBySender(senderCompId);
    }
    
    /**
     * Lista permissões para um destinatário.
     */
    public List<ParticipantPermission> getPermissionsForTarget(String targetCompId) {
        return repository.findByTarget(targetCompId);
    }
    
    /**
     * Lista todas as permissões ativas.
     */
    public List<ParticipantPermission> getAllActivePermissions() {
        return repository.findAll().stream()
            .filter(ParticipantPermission::isEnabled)
            .collect(Collectors.toList());
    }
    
    /**
     * Verifica se uma mensagem é permitida.
     */
    public boolean isMessageAllowed(String senderCompId, String targetCompId,
                                  String messageType, String symbol) {
        return repository.isMessageAllowed(senderCompId, targetCompId, messageType, symbol);
    }
    
    /**
     * Obtém o interceptador de autorização.
     */
    public MessageAuthorizationInterceptor getInterceptor() {
        return interceptor;
    }
    
    /**
     * Carrega configuração de permissões a partir de mapa.
     */
    public void loadPermissionsFromConfig(Map<String, Object> config) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> permissions = (List<Map<String, Object>>) config.get("permissions");
        
        if (permissions != null) {
            for (Map<String, Object> permConfig : permissions) {
                String sender = (String) permConfig.get("sender");
                String target = (String) permConfig.get("target");
                @SuppressWarnings("unchecked")
                List<String> msgTypes = (List<String>) permConfig.get("messageTypes");
                @SuppressWarnings("unchecked")
                List<String> symbols = (List<String>) permConfig.get("symbols");
                String description = (String) permConfig.get("description");
                
                createPermission(sender, target,
                    msgTypes != null ? Set.copyOf(msgTypes) : Set.of("*"),
                    symbols != null ? Set.copyOf(symbols) : Set.of("*"),
                    null, null, description);
            }
        }
    }
    
    /**
     * Exporta permissões para configuração.
     */
    public Map<String, Object> exportPermissionsConfig() {
        List<Map<String, Object>> permissions = repository.findAll().stream()
            .map(this::permissionToMap)
            .collect(Collectors.toList());
        
        Map<String, Object> config = new HashMap<>();
        config.put("permissions", permissions);
        config.put("exportedAt", LocalDateTime.now().toString());
        config.put("statistics", repository.getStatistics());
        
        return config;
    }
    
    private Map<String, Object> permissionToMap(ParticipantPermission permission) {
        Map<String, Object> map = new HashMap<>();
        map.put("sender", permission.getSenderCompId());
        map.put("target", permission.getTargetCompId());
        map.put("messageTypes", new ArrayList<>(permission.getAllowedMessageTypes()));
        map.put("symbols", new ArrayList<>(permission.getAllowedSymbols()));
        map.put("enabled", permission.isEnabled());
        map.put("description", permission.getRuleDescription());
        if (permission.getValidFrom() != null) {
            map.put("validFrom", permission.getValidFrom().toString());
        }
        if (permission.getValidTo() != null) {
            map.put("validTo", permission.getValidTo().toString());
        }
        return map;
    }
    
    /**
     * Obtém estatísticas do sistema de permissões.
     */
    public Map<String, Object> getStatistics() {
        return repository.getStatistics();
    }
}
