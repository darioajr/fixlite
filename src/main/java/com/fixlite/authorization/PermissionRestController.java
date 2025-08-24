package com.fixlite.authorization;

import java.time.LocalDateTime;
import java.util.*;

/**
 * REST Controller para gerenciar permissões entre participantes.
 * Implementação simples sem dependências externas.
 */
public class PermissionRestController {
    
    private final ParticipantPermissionService permissionService;
    
    public PermissionRestController(ParticipantPermissionService permissionService) {
        this.permissionService = permissionService;
    }
    
    /**
     * GET /permissions - Lista todas as permissões ativas
     */
    public Map<String, Object> getAllPermissions() {
        List<ParticipantPermission> permissions = permissionService.getAllActivePermissions();
        
        Map<String, Object> response = new HashMap<>();
        response.put("permissions", permissions);
        response.put("count", permissions.size());
        response.put("timestamp", LocalDateTime.now().toString());
        
        return response;
    }
    
    /**
     * GET /permissions/sender/{compId} - Lista permissões de um remetente
     */
    public Map<String, Object> getPermissionsBySender(String senderCompId) {
        List<ParticipantPermission> permissions = permissionService.getPermissionsForSender(senderCompId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("senderCompId", senderCompId);
        response.put("permissions", permissions);
        response.put("count", permissions.size());
        
        return response;
    }
    
    /**
     * GET /permissions/target/{compId} - Lista permissões para um destinatário
     */
    public Map<String, Object> getPermissionsByTarget(String targetCompId) {
        List<ParticipantPermission> permissions = permissionService.getPermissionsForTarget(targetCompId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("targetCompId", targetCompId);
        response.put("permissions", permissions);
        response.put("count", permissions.size());
        
        return response;
    }
    
    /**
     * POST /permissions - Cria nova permissão
     */
    public Map<String, Object> createPermission(Map<String, Object> request) {
        try {
            String sender = (String) request.get("sender");
            String target = (String) request.get("target");
            @SuppressWarnings("unchecked")
            List<String> messageTypes = (List<String>) request.get("messageTypes");
            @SuppressWarnings("unchecked")
            List<String> symbols = (List<String>) request.get("symbols");
            String description = (String) request.get("description");
            
            if (sender == null || target == null) {
                return createErrorResponse("Sender and target are required");
            }
            
            Set<String> msgTypeSet = messageTypes != null ? Set.copyOf(messageTypes) : Set.of("*");
            Set<String> symbolSet = symbols != null ? Set.copyOf(symbols) : Set.of("*");
            
            permissionService.createPermission(sender, target, msgTypeSet, symbolSet,
                null, null, description != null ? description : "Created via API");
            
            return createSuccessResponse("Permission created successfully");
            
        } catch (Exception e) {
            return createErrorResponse("Error creating permission: " + e.getMessage());
        }
    }
    
    /**
     * POST /permissions/trading - Cria permissão de trading
     */
    public Map<String, Object> createTradingPermission(Map<String, Object> request) {
        try {
            String broker = (String) request.get("broker");
            String exchange = (String) request.get("exchange");
            @SuppressWarnings("unchecked")
            List<String> symbols = (List<String>) request.get("symbols");
            
            if (broker == null || exchange == null) {
                return createErrorResponse("Broker and exchange are required");
            }
            
            String[] symbolArray = symbols != null ? symbols.toArray(new String[0]) : new String[]{"*"};
            permissionService.createTradingPermission(broker, exchange, symbolArray);
            
            return createSuccessResponse("Trading permission created successfully");
            
        } catch (Exception e) {
            return createErrorResponse("Error creating trading permission: " + e.getMessage());
        }
    }
    
    /**
     * POST /permissions/post-trading - Cria permissão de pós-trading
     */
    public Map<String, Object> createPostTradingPermission(Map<String, Object> request) {
        try {
            String sender = (String) request.get("sender");
            String target = (String) request.get("target");
            
            if (sender == null || target == null) {
                return createErrorResponse("Sender and target are required");
            }
            
            permissionService.createPostTradingPermission(sender, target);
            
            return createSuccessResponse("Post-trading permission created successfully");
            
        } catch (Exception e) {
            return createErrorResponse("Error creating post-trading permission: " + e.getMessage());
        }
    }
    
    /**
     * PUT /permissions/{sender}/{target}/disable - Desabilita permissão
     */
    public Map<String, Object> disablePermission(String sender, String target) {
        boolean disabled = permissionService.disablePermission(sender, target);
        
        if (disabled) {
            return createSuccessResponse("Permission disabled successfully");
        } else {
            return createErrorResponse("Permission not found");
        }
    }
    
    /**
     * DELETE /permissions/{sender}/{target} - Remove permissão
     */
    public Map<String, Object> removePermission(String sender, String target) {
        boolean removed = permissionService.removePermission(sender, target);
        
        if (removed) {
            return createSuccessResponse("Permission removed successfully");
        } else {
            return createErrorResponse("Permission not found");
        }
    }
    
    /**
     * POST /permissions/check - Verifica se mensagem é permitida
     */
    public Map<String, Object> checkPermission(Map<String, Object> request) {
        String sender = (String) request.get("sender");
        String target = (String) request.get("target");
        String messageType = (String) request.get("messageType");
        String symbol = (String) request.get("symbol");
        
        if (sender == null || target == null || messageType == null) {
            return createErrorResponse("Sender, target and messageType are required");
        }
        
        boolean allowed = permissionService.isMessageAllowed(sender, target, messageType, symbol);
        
        Map<String, Object> response = new HashMap<>();
        response.put("allowed", allowed);
        response.put("sender", sender);
        response.put("target", target);
        response.put("messageType", messageType);
        response.put("symbol", symbol);
        response.put("timestamp", LocalDateTime.now().toString());
        
        return response;
    }
    
    /**
     * GET /permissions/statistics - Estatísticas do sistema
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = permissionService.getStatistics();
        stats.put("timestamp", LocalDateTime.now().toString());
        return stats;
    }
    
    /**
     * POST /permissions/bulk - Cria múltiplas permissões
     */
    public Map<String, Object> createBulkPermissions(Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> permissions = (List<Map<String, Object>>) request.get("permissions");
            
            if (permissions == null || permissions.isEmpty()) {
                return createErrorResponse("Permissions list is required");
            }
            
            int created = 0;
            List<String> errors = new ArrayList<>();
            
            for (Map<String, Object> permRequest : permissions) {
                try {
                    createPermission(permRequest);
                    created++;
                } catch (Exception e) {
                    errors.add("Error creating permission: " + e.getMessage());
                }
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("created", created);
            response.put("total", permissions.size());
            if (!errors.isEmpty()) {
                response.put("errors", errors);
            }
            
            return response;
            
        } catch (Exception e) {
            return createErrorResponse("Error processing bulk permissions: " + e.getMessage());
        }
    }
    
    /**
     * GET /permissions/export - Exporta configuração de permissões
     */
    public Map<String, Object> exportPermissions() {
        return permissionService.exportPermissionsConfig();
    }
    
    /**
     * POST /permissions/import - Importa configuração de permissões
     */
    public Map<String, Object> importPermissions(Map<String, Object> config) {
        try {
            permissionService.loadPermissionsFromConfig(config);
            return createSuccessResponse("Permissions imported successfully");
        } catch (Exception e) {
            return createErrorResponse("Error importing permissions: " + e.getMessage());
        }
    }
    
    private Map<String, Object> createSuccessResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", message);
        response.put("timestamp", LocalDateTime.now().toString());
        return response;
    }
    
    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", message);
        response.put("timestamp", LocalDateTime.now().toString());
        return response;
    }
}
