package com.fixlite.integration;

import com.fixlite.protocol.FixVersion;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Runner para testes de performance com múltiplos clientes QuickFIX simultâneos.
 */
public class PerformanceTestRunner {
    
    public static void main(String[] args) throws Exception {
        String gatewayHost = System.getenv().getOrDefault("GATEWAY_HOST", "localhost");
        int gatewayPort = Integer.parseInt(System.getenv().getOrDefault("GATEWAY_PORT", "9878"));
        int orderCount = Integer.parseInt(System.getenv().getOrDefault("ORDER_COUNT", "1000"));
        int concurrentClients = Integer.parseInt(System.getenv().getOrDefault("CONCURRENT_CLIENTS", "5"));
        
        System.out.println("=== TESTE DE PERFORMANCE QUICKFIX ===");
        System.out.println("Gateway: " + gatewayHost + ":" + gatewayPort);
        System.out.println("Ordens por cliente: " + orderCount);
        System.out.println("Clientes simultâneos: " + concurrentClients);
        System.out.println("Total de ordens: " + (orderCount * concurrentClients));
        
        ExecutorService executor = Executors.newFixedThreadPool(concurrentClients);
        CountDownLatch allClientsReady = new CountDownLatch(concurrentClients);
        CountDownLatch testComplete = new CountDownLatch(concurrentClients);
        
        AtomicInteger totalOrdersSent = new AtomicInteger(0);
        AtomicInteger totalExecutionsReceived = new AtomicInteger(0);
        AtomicLong totalLatency = new AtomicLong(0);
        AtomicInteger latencyMeasurements = new AtomicInteger(0);
        
        long testStartTime = System.currentTimeMillis();
        
        // Criar e executar clientes de performance
        for (int clientNum = 1; clientNum <= concurrentClients; clientNum++) {
            final int clientId = clientNum;
            
            executor.submit(() -> {
                try {
                    runPerformanceClient(
                        gatewayHost, gatewayPort, clientId, orderCount,
                        allClientsReady, testComplete,
                        totalOrdersSent, totalExecutionsReceived,
                        totalLatency, latencyMeasurements
                    );
                } catch (Exception e) {
                    System.err.println("Erro no cliente " + clientId + ": " + e.getMessage());
                    testComplete.countDown();
                }
            });
        }
        
        // Aguardar todos os clientes estarem prontos
        System.out.println("Aguardando todos os clientes conectarem...");
        if (!allClientsReady.await(30, TimeUnit.SECONDS)) {
            System.err.println("ERRO: Nem todos os clientes conseguiram conectar");
            System.exit(1);
        }
        
        System.out.println("Todos os clientes conectados! Iniciando teste de performance...");
        
        // Aguardar conclusão do teste
        if (!testComplete.await(300, TimeUnit.SECONDS)) {
            System.err.println("ERRO: Teste não completou em 5 minutos");
            System.exit(1);
        }
        
        long testEndTime = System.currentTimeMillis();
        long totalDuration = testEndTime - testStartTime;
        
        // Calcular estatísticas
        double ordersPerSecond = (totalOrdersSent.get() * 1000.0) / totalDuration;
        double executionsPerSecond = (totalExecutionsReceived.get() * 1000.0) / totalDuration;
        double avgLatency = latencyMeasurements.get() > 0 ? 
            (totalLatency.get() / (double) latencyMeasurements.get()) : 0;
        
        // Exibir resultados
        System.out.println("\n=== RESULTADOS DO TESTE DE PERFORMANCE ===");
        System.out.println("Duração total: " + totalDuration + "ms");
        System.out.println("Ordens enviadas: " + totalOrdersSent.get());
        System.out.println("Execuções recebidas: " + totalExecutionsReceived.get());
        System.out.println("Taxa de ordens: " + String.format("%.2f", ordersPerSecond) + " ordens/seg");
        System.out.println("Taxa de execuções: " + String.format("%.2f", executionsPerSecond) + " exec/seg");
        System.out.println("Latência média: " + String.format("%.2f", avgLatency) + "ms");
        
        // Verificar critérios de sucesso
        boolean success = true;
        if (totalOrdersSent.get() < orderCount * concurrentClients * 0.95) {
            System.err.println("FALHA: Menos de 95% das ordens foram enviadas");
            success = false;
        }
        
        if (totalExecutionsReceived.get() < totalOrdersSent.get() * 0.9) {
            System.err.println("FALHA: Menos de 90% das execuções foram recebidas");
            success = false;
        }
        
        if (ordersPerSecond < 100) {
            System.err.println("FALHA: Taxa de ordens muito baixa (< 100/seg)");
            success = false;
        }
        
        if (avgLatency > 1000) {
            System.err.println("FALHA: Latência muito alta (> 1000ms)");
            success = false;
        }
        
        if (success) {
            System.out.println("✓ TESTE DE PERFORMANCE PASSOU!");
        } else {
            System.err.println("✗ TESTE DE PERFORMANCE FALHOU!");
            System.exit(1);
        }
        
        executor.shutdown();
    }
    
    private static void runPerformanceClient(
            String host, int port, int clientId, int orderCount,
            CountDownLatch allReady, CountDownLatch testComplete,
            AtomicInteger totalOrdersSent, AtomicInteger totalExecutionsReceived,
            AtomicLong totalLatency, AtomicInteger latencyMeasurements) throws Exception {
        
        String clientName = "PERF_CLIENT_" + clientId;
        FixVersion version = (clientId % 2 == 0) ? FixVersion.FIX_4_4 : FixVersion.FIX_5_0_SP2;
        
        QuickFixClientSimulator client = new QuickFixClientSimulator(
            host, port, clientName, "FIXLITE", version
        );
        
        CountDownLatch logonLatch = new CountDownLatch(1);
        ConcurrentHashMap<String, Long> orderTimes = new ConcurrentHashMap<>();
        
        client.setConnectionCallback(new QuickFixClientSimulator.ConnectionCallback() {
            @Override
            public void onConnected() {}
            
            @Override
            public void onDisconnected() {}
            
            @Override
            public void onLoggedOn() {
                logonLatch.countDown();
            }
        });
        
        client.setMessageCallback(message -> {
            String msgType = message.getField(com.fixlite.protocol.FixFields.MSG_TYPE);
            if ("8".equals(msgType)) { // ExecutionReport
                totalExecutionsReceived.incrementAndGet();
                
                String clOrdId = message.getField(com.fixlite.protocol.FixFields.CL_ORD_ID);
                Long orderTime = orderTimes.remove(clOrdId);
                if (orderTime != null) {
                    long latency = System.currentTimeMillis() - orderTime;
                    totalLatency.addAndGet(latency);
                    latencyMeasurements.incrementAndGet();
                }
            }
        });
        
        try {
            // Conectar
            client.connect().get(10, TimeUnit.SECONDS);
            
            if (!logonLatch.await(5, TimeUnit.SECONDS)) {
                throw new RuntimeException("Falha no logon para cliente " + clientId);
            }
            
            // Sinalizar que está pronto
            allReady.countDown();
            
            // Aguardar todos os clientes estarem prontos
            allReady.await();
            
            // Enviar ordens em alta velocidade
            String[] instruments = {"AAPL", "MSFT", "GOOGL", "TSLA", "AMZN", "META", "NFLX", "NVDA"};
            
            for (int i = 1; i <= orderCount; i++) {
                String clOrdId = clientName + "_ORDER_" + i;
                String symbol = instruments[i % instruments.length];
                String side = (i % 2 == 0) ? "1" : "2";
                String qty = String.valueOf(100 + (i % 10) * 10);
                String price = String.valueOf(100.0 + (i % 100) * 0.1);
                
                orderTimes.put(clOrdId, System.currentTimeMillis());
                client.sendNewOrderSingle(clOrdId, symbol, side, qty, price);
                totalOrdersSent.incrementAndGet();
                
                // Pequeno delay para não sobrecarregar
                if (i % 100 == 0) {
                    Thread.sleep(1);
                }
            }
            
            // Aguardar processamento das últimas ordens
            Thread.sleep(5000);
            
        } finally {
            client.disconnect();
            testComplete.countDown();
        }
    }
}
