package com.fixlite.integration;

import com.fixlite.protocol.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Cenário completo de teste de compatibilidade com QuickFIX.
 * Simula ambiente real de trading com múltiplos clientes e cenários complexos.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class QuickFixCompatibilityTest {
    
    private static FixGatewayTestServer testServer;
    private static final int SERVER_PORT = 9880;
    
    @BeforeAll
    static void startTestServer() throws InterruptedException {
        testServer = new FixGatewayTestServer(SERVER_PORT);
        testServer.start();
        Thread.sleep(1000); // Aguardar servidor inicializar
    }
    
    @AfterAll
    static void stopTestServer() {
        if (testServer != null) {
            testServer.stop();
        }
    }
    
    @Test
    @Order(1)
    @DisplayName("Cenário 1: Logon Sequencial de Múltiplos Clientes QuickFIX")
    public void testSequentialClientLogon() throws Exception {
        // Simular diferentes tipos de clientes FIX
        QuickFixClientSimulator[] clients = {
            new QuickFixClientSimulator("localhost", SERVER_PORT, "TRADER1", "FIXLITE", FixVersion.FIX_4_4),
            new QuickFixClientSimulator("localhost", SERVER_PORT, "TRADER2", "FIXLITE", FixVersion.FIX_5_0_SP2),
            new QuickFixClientSimulator("localhost", SERVER_PORT, "ALGO1", "FIXLITE", FixVersion.FIX_4_4),
            new QuickFixClientSimulator("localhost", SERVER_PORT, "MARKET_MAKER", "FIXLITE", FixVersion.FIX_5_0_SP2)
        };
        
        CountDownLatch allLoggedOn = new CountDownLatch(clients.length);
        
        for (QuickFixClientSimulator client : clients) {
            client.setConnectionCallback(new QuickFixClientSimulator.ConnectionCallback() {
                @Override
                public void onConnected() {}
                
                @Override
                public void onDisconnected() {}
                
                @Override
                public void onLoggedOn() {
                    allLoggedOn.countDown();
                }
            });
        }
        
        // Conectar clientes sequencialmente
        for (QuickFixClientSimulator client : clients) {
            client.connect().get(5, TimeUnit.SECONDS);
            Thread.sleep(100); // Pequeno delay entre conexões
        }
        
        assertTrue(allLoggedOn.await(10, TimeUnit.SECONDS), "Nem todos os clientes fizeram logon");
        
        // Verificar que todos estão conectados
        for (QuickFixClientSimulator client : clients) {
            assertTrue(client.isLoggedOn(), "Cliente não está logado");
        }
        
        // Cleanup
        for (QuickFixClientSimulator client : clients) {
            client.disconnect();
        }
    }
    
    @Test
    @Order(2)
    @DisplayName("Cenário 2: Trading Session Completa com Diferentes Instrumentos")
    public void testCompleteTradingSession() throws Exception {
        QuickFixClientSimulator trader = new QuickFixClientSimulator(
            "localhost", SERVER_PORT, "TRADER_MAIN", "FIXLITE", FixVersion.FIX_4_4
        );
        
        CountDownLatch sessionReady = new CountDownLatch(1);
        AtomicInteger executionReports = new AtomicInteger(0);
        
        trader.setConnectionCallback(new QuickFixClientSimulator.ConnectionCallback() {
            @Override
            public void onConnected() {}
            
            @Override
            public void onDisconnected() {}
            
            @Override
            public void onLoggedOn() {
                sessionReady.countDown();
            }
        });
        
        trader.setMessageCallback(message -> {
            if (FixMessageTypes.EXECUTION_REPORT.equals(message.getField(FixFields.MSG_TYPE))) {
                executionReports.incrementAndGet();
            }
        });
        
        // Conectar e aguardar logon
        trader.connect().get(5, TimeUnit.SECONDS);
        assertTrue(sessionReady.await(3, TimeUnit.SECONDS));
        
        // Simular sessão de trading com diferentes instrumentos
        String[] instruments = {"AAPL", "MSFT", "GOOGL", "TSLA", "AMZN"};
        String[] sides = {"1", "2"}; // Buy/Sell
        
        for (int i = 0; i < instruments.length; i++) {
            String clOrdId = "ORDER_" + System.currentTimeMillis() + "_" + i;
            String side = sides[i % 2];
            String qty = String.valueOf(100 * (i + 1));
            String price = String.valueOf(100.0 + i * 10);
            
            trader.sendNewOrderSingle(clOrdId, instruments[i], side, qty, price);
            Thread.sleep(50); // Simular intervalo entre ordens
        }
        
        // Aguardar execução de todas as ordens (2 reports por ordem: New + Fill)
        Thread.sleep(2000);
        assertEquals(instruments.length * 2, executionReports.get(), 
                    "Número incorreto de execution reports");
        
        trader.disconnect();
    }
    
    @Test
    @Order(3)
    @DisplayName("Cenário 3: Teste de Heartbeat e Recuperação de Conexão")
    public void testHeartbeatAndRecovery() throws Exception {
        QuickFixClientSimulator client = new QuickFixClientSimulator(
            "localhost", SERVER_PORT, "HEARTBEAT_CLIENT", "FIXLITE", FixVersion.FIX_4_4
        );
        
        CountDownLatch logonLatch = new CountDownLatch(1);
        CountDownLatch heartbeatLatch = new CountDownLatch(3); // Esperar 3 heartbeats
        
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
            if (FixMessageTypes.HEARTBEAT.equals(message.getField(FixFields.MSG_TYPE))) {
                heartbeatLatch.countDown();
            }
        });
        
        client.connect().get(5, TimeUnit.SECONDS);
        assertTrue(logonLatch.await(3, TimeUnit.SECONDS));
        
        // Enviar múltiplos TestRequest para testar heartbeat
        for (int i = 1; i <= 3; i++) {
            client.sendTestRequest("TEST_" + i);
            Thread.sleep(200);
        }
        
        assertTrue(heartbeatLatch.await(5, TimeUnit.SECONDS), "Heartbeats não recebidos");
        assertTrue(client.isConnected(), "Cliente desconectou durante teste de heartbeat");
        
        client.disconnect();
    }
    
    @Test
    @Order(4)
    @DisplayName("Cenário 4: Teste de Performance com Alto Volume")
    public void testHighVolumePerformance() throws Exception {
        QuickFixClientSimulator highVolumeClient = new QuickFixClientSimulator(
            "localhost", SERVER_PORT, "HIGH_VOLUME", "FIXLITE", FixVersion.FIX_5_0_SP2
        );
        
        CountDownLatch logonLatch = new CountDownLatch(1);
        AtomicInteger processedOrders = new AtomicInteger(0);
        
        highVolumeClient.setConnectionCallback(new QuickFixClientSimulator.ConnectionCallback() {
            @Override
            public void onConnected() {}
            
            @Override
            public void onDisconnected() {}
            
            @Override
            public void onLoggedOn() {
                logonLatch.countDown();
            }
        });
        
        highVolumeClient.setMessageCallback(message -> {
            if (FixMessageTypes.EXECUTION_REPORT.equals(message.getField(FixFields.MSG_TYPE))) {
                processedOrders.incrementAndGet();
            }
        });
        
        highVolumeClient.connect().get(5, TimeUnit.SECONDS);
        assertTrue(logonLatch.await(3, TimeUnit.SECONDS));
        
        // Teste de performance: 500 ordens em rajada
        int orderCount = 500;
        long startTime = System.currentTimeMillis();
        
        for (int i = 1; i <= orderCount; i++) {
            String clOrdId = "HV_ORDER_" + i;
            String symbol = "STOCK" + (i % 50); // 50 símbolos diferentes
            String side = (i % 2 == 0) ? "1" : "2";
            String qty = String.valueOf(100 + (i % 10) * 10);
            String price = String.valueOf(100.0 + (i % 100) * 0.1);
            
            highVolumeClient.sendNewOrderSingle(clOrdId, symbol, side, qty, price);
            
            // Pequeno delay a cada 50 ordens para não sobrecarregar
            if (i % 50 == 0) {
                Thread.sleep(10);
            }
        }
        
        // Aguardar processamento (2 reports por ordem)
        long timeout = 30000; // 30 segundos
        long deadline = System.currentTimeMillis() + timeout;
        
        while (processedOrders.get() < orderCount * 2 && System.currentTimeMillis() < deadline) {
            Thread.sleep(100);
        }
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        assertTrue(processedOrders.get() >= orderCount, "Nem todas as ordens foram processadas");
        assertTrue(duration < 20000, "Performance inadequada: " + duration + "ms para " + orderCount + " ordens");
        
        System.out.println("Performance: " + orderCount + " ordens processadas em " + duration + "ms");
        System.out.println("Taxa: " + (orderCount * 1000.0 / duration) + " ordens/segundo");
        
        highVolumeClient.disconnect();
    }
    
    @Test
    @Order(5)
    @DisplayName("Cenário 5: Teste de Múltiplas Versões FIX Simultâneas")
    public void testMultiVersionSimultaneous() throws Exception {
        QuickFixClientSimulator fix44Client = new QuickFixClientSimulator(
            "localhost", SERVER_PORT, "FIX44_CLIENT", "FIXLITE", FixVersion.FIX_4_4
        );
        
        QuickFixClientSimulator fix50Client = new QuickFixClientSimulator(
            "localhost", SERVER_PORT, "FIX50_CLIENT", "FIXLITE", FixVersion.FIX_5_0_SP2
        );
        
        CountDownLatch bothLoggedOn = new CountDownLatch(2);
        AtomicInteger fix44Messages = new AtomicInteger(0);
        AtomicInteger fix50Messages = new AtomicInteger(0);
        
        QuickFixClientSimulator.ConnectionCallback logonCallback = 
            new QuickFixClientSimulator.ConnectionCallback() {
                @Override
                public void onConnected() {}
                
                @Override
                public void onDisconnected() {}
                
                @Override
                public void onLoggedOn() {
                    bothLoggedOn.countDown();
                }
            };
        
        fix44Client.setConnectionCallback(logonCallback);
        fix50Client.setConnectionCallback(logonCallback);
        
        fix44Client.setMessageCallback(message -> {
            if (FixMessageTypes.EXECUTION_REPORT.equals(message.getField(FixFields.MSG_TYPE))) {
                fix44Messages.incrementAndGet();
            }
        });
        
        fix50Client.setMessageCallback(message -> {
            if (FixMessageTypes.EXECUTION_REPORT.equals(message.getField(FixFields.MSG_TYPE))) {
                fix50Messages.incrementAndGet();
            }
        });
        
        // Conectar ambos os clientes
        CompletableFuture<Void> connect44 = fix44Client.connect();
        CompletableFuture<Void> connect50 = fix50Client.connect();
        
        CompletableFuture.allOf(connect44, connect50).get(10, TimeUnit.SECONDS);
        assertTrue(bothLoggedOn.await(5, TimeUnit.SECONDS));
        
        // Enviar ordens simultaneamente
        CompletableFuture.runAsync(() -> {
            for (int i = 1; i <= 10; i++) {
                fix44Client.sendNewOrderSingle("FIX44_" + i, "STOCK44_" + i, "1", "100", "50.0");
                try { Thread.sleep(50); } catch (InterruptedException e) {}
            }
        });
        
        CompletableFuture.runAsync(() -> {
            for (int i = 1; i <= 10; i++) {
                fix50Client.sendNewOrderSingle("FIX50_" + i, "STOCK50_" + i, "2", "200", "75.0");
                try { Thread.sleep(50); } catch (InterruptedException e) {}
            }
        });
        
        // Aguardar processamento
        Thread.sleep(3000);
        
        assertEquals(20, fix44Messages.get(), "Mensagens FIX 4.4 não processadas corretamente");
        assertEquals(20, fix50Messages.get(), "Mensagens FIX 5.0 não processadas corretamente");
        
        fix44Client.disconnect();
        fix50Client.disconnect();
    }
    
    @Test
    @Order(6)
    @DisplayName("Cenário 6: Teste de Robustez com Mensagens Inválidas")
    public void testRobustnessWithInvalidMessages() throws Exception {
        QuickFixClientSimulator robustClient = new QuickFixClientSimulator(
            "localhost", SERVER_PORT, "ROBUST_CLIENT", "FIXLITE", FixVersion.FIX_4_4
        );
        
        CountDownLatch logonLatch = new CountDownLatch(1);
        AtomicReference<FixMessage> rejectMessage = new AtomicReference<>();
        
        robustClient.setConnectionCallback(new QuickFixClientSimulator.ConnectionCallback() {
            @Override
            public void onConnected() {}
            
            @Override
            public void onDisconnected() {}
            
            @Override
            public void onLoggedOn() {
                logonLatch.countDown();
            }
        });
        
        robustClient.setMessageCallback(message -> {
            if (FixMessageTypes.REJECT.equals(message.getField(FixFields.MSG_TYPE))) {
                rejectMessage.set(message);
            }
        });
        
        robustClient.connect().get(5, TimeUnit.SECONDS);
        assertTrue(logonLatch.await(3, TimeUnit.SECONDS));
        
        // Enviar mensagem inválida
        FixMessage invalidMessage = new FixMessage("INVALID_TYPE");
        invalidMessage.setVersion(FixVersion.FIX_4_4);
        invalidMessage.setField(FixFields.SENDER_COMP_ID, "ROBUST_CLIENT");
        invalidMessage.setField(FixFields.TARGET_COMP_ID, "FIXLITE");
        invalidMessage.setField(FixFields.MSG_SEQ_NUM, "999");
        
        robustClient.sendMessage(invalidMessage);
        
        // Aguardar reject
        Thread.sleep(1000);
        
        // Cliente deve continuar conectado após mensagem inválida
        assertTrue(robustClient.isConnected(), "Cliente desconectou após mensagem inválida");
        
        // Enviar mensagem válida para confirmar que conexão ainda funciona
        CountDownLatch validResponseLatch = new CountDownLatch(1);
        robustClient.setMessageCallback(message -> {
            if (FixMessageTypes.HEARTBEAT.equals(message.getField(FixFields.MSG_TYPE))) {
                validResponseLatch.countDown();
            }
        });
        
        robustClient.sendTestRequest("RECOVERY_TEST");
        assertTrue(validResponseLatch.await(3, TimeUnit.SECONDS), "Recuperação após mensagem inválida falhou");
        
        robustClient.disconnect();
    }
    
    @Test
    @Order(7)
    @DisplayName("Cenário 7: Teste de Compatibilidade de Checksum")
    public void testChecksumCompatibility() throws Exception {
        QuickFixClientSimulator checksumClient = new QuickFixClientSimulator(
            "localhost", SERVER_PORT, "CHECKSUM_CLIENT", "FIXLITE", FixVersion.FIX_4_4
        );
        
        CountDownLatch logonLatch = new CountDownLatch(1);
        checksumClient.setConnectionCallback(new QuickFixClientSimulator.ConnectionCallback() {
            @Override
            public void onConnected() {}
            
            @Override
            public void onDisconnected() {}
            
            @Override
            public void onLoggedOn() {
                logonLatch.countDown();
            }
        });
        
        checksumClient.connect().get(5, TimeUnit.SECONDS);
        assertTrue(logonLatch.await(3, TimeUnit.SECONDS));
        
        // Criar mensagem e verificar checksum
        FixMessageFactory factory = new FixMessageFactory(FixVersion.FIX_4_4, "CHECKSUM_CLIENT", "FIXLITE");
        FixMessage testMessage = factory.createHeartbeat(null);
        testMessage.setField(FixFields.MSG_SEQ_NUM, "100");
        
        // Verificar que mensagem codificada tem checksum válido
        String encoded = testMessage.encode();
        assertTrue(FixParser.validateChecksum(encoded), "Checksum inválido na mensagem");
        
        // Verificar formato QuickFIX
        assertTrue(encoded.startsWith("8=FIX.4.4\001"), "Formato de BeginString incorreto");
        assertTrue(encoded.contains("35=0\001"), "MsgType de Heartbeat incorreto");
        assertTrue(encoded.matches(".*10=\\d{3}\001$"), "Formato de checksum incorreto");
        
        // Enviar mensagem e verificar aceitação
        checksumClient.sendMessage(testMessage);
        Thread.sleep(500);
        
        assertTrue(checksumClient.isConnected(), "Conexão perdida - possível problema de checksum");
        
        checksumClient.disconnect();
    }
    
    @Test
    @Order(8)
    @DisplayName("Cenário 8: Teste de Stress com Reconexões")
    public void testStressWithReconnections() throws Exception {
        QuickFixClientSimulator stressClient = new QuickFixClientSimulator(
            "localhost", SERVER_PORT, "STRESS_CLIENT", "FIXLITE", FixVersion.FIX_4_4
        );
        
        int reconnectionCount = 5;
        AtomicInteger successfulReconnections = new AtomicInteger(0);
        
        for (int i = 1; i <= reconnectionCount; i++) {
            CountDownLatch reconnectLatch = new CountDownLatch(1);
            
            stressClient.setConnectionCallback(new QuickFixClientSimulator.ConnectionCallback() {
                @Override
                public void onConnected() {}
                
                @Override
                public void onDisconnected() {}
                
                @Override
                public void onLoggedOn() {
                    reconnectLatch.countDown();
                    successfulReconnections.incrementAndGet();
                }
            });
            
            // Conectar
            stressClient.connect().get(5, TimeUnit.SECONDS);
            assertTrue(reconnectLatch.await(3, TimeUnit.SECONDS), "Reconexão " + i + " falhou");
            
            // Enviar algumas mensagens
            stressClient.sendTestRequest("STRESS_" + i);
            stressClient.sendHeartbeat(null);
            
            Thread.sleep(100);
            
            // Desconectar
            stressClient.disconnect();
            Thread.sleep(200); // Aguardar desconexão completa
        }
        
        assertEquals(reconnectionCount, successfulReconnections.get(), 
                    "Nem todas as reconexões foram bem-sucedidas");
    }
    
    @Test
    @Order(9)
    @DisplayName("Cenário 9: Teste Final - Ambiente Completo de Trading")
    public void testCompleteTradingEnvironment() throws Exception {
        // Simular ambiente real com múltiplos participantes
        QuickFixClientSimulator[] participants = {
            new QuickFixClientSimulator("localhost", SERVER_PORT, "BANK_A", "FIXLITE", FixVersion.FIX_4_4),
            new QuickFixClientSimulator("localhost", SERVER_PORT, "HEDGE_FUND", "FIXLITE", FixVersion.FIX_5_0_SP2),
            new QuickFixClientSimulator("localhost", SERVER_PORT, "RETAIL_BROKER", "FIXLITE", FixVersion.FIX_4_4),
            new QuickFixClientSimulator("localhost", SERVER_PORT, "ALGO_TRADER", "FIXLITE", FixVersion.FIX_5_0_SP2)
        };
        
        CountDownLatch allConnected = new CountDownLatch(participants.length);
        AtomicInteger totalExecutions = new AtomicInteger(0);
        
        // Configurar callbacks
        for (QuickFixClientSimulator participant : participants) {
            participant.setConnectionCallback(new QuickFixClientSimulator.ConnectionCallback() {
                @Override
                public void onConnected() {}
                
                @Override
                public void onDisconnected() {}
                
                @Override
                public void onLoggedOn() {
                    allConnected.countDown();
                }
            });
            
            participant.setMessageCallback(message -> {
                if (FixMessageTypes.EXECUTION_REPORT.equals(message.getField(FixFields.MSG_TYPE))) {
                    totalExecutions.incrementAndGet();
                }
            });
        }
        
        // Conectar todos os participantes
        CompletableFuture<Void>[] connections = new CompletableFuture[participants.length];
        for (int i = 0; i < participants.length; i++) {
            connections[i] = participants[i].connect();
        }
        
        CompletableFuture.allOf(connections).get(15, TimeUnit.SECONDS);
        assertTrue(allConnected.await(10, TimeUnit.SECONDS), "Nem todos os participantes conectaram");
        
        // Simular atividade de trading concorrente
        String[] instruments = {"AAPL", "MSFT", "GOOGL", "TSLA", "AMZN", "META", "NFLX", "NVDA"};
        
        CompletableFuture<Void>[] tradingTasks = new CompletableFuture[participants.length];
        
        for (int i = 0; i < participants.length; i++) {
            final int participantIndex = i;
            tradingTasks[i] = CompletableFuture.runAsync(() -> {
                QuickFixClientSimulator participant = participants[participantIndex];
                String participantId = participant.toString().split("@")[0]; // Simular ID único
                
                for (int orderNum = 1; orderNum <= 20; orderNum++) {
                    String clOrdId = "P" + participantIndex + "_ORDER_" + orderNum;
                    String symbol = instruments[orderNum % instruments.length];
                    String side = (orderNum % 2 == 0) ? "1" : "2";
                    String qty = String.valueOf(100 * (orderNum % 5 + 1));
                    String price = String.valueOf(100.0 + orderNum * 0.5);
                    
                    participant.sendNewOrderSingle(clOrdId, symbol, side, qty, price);
                    
                    try {
                        Thread.sleep(50 + (int)(Math.random() * 100)); // Delay aleatório
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            });
        }
        
        // Aguardar conclusão de todas as atividades de trading
        CompletableFuture.allOf(tradingTasks).get(30, TimeUnit.SECONDS);
        
        // Aguardar processamento de todas as execuções
        Thread.sleep(5000);
        
        // Verificar resultados
        int expectedExecutions = participants.length * 20 * 2; // 2 reports por ordem
        assertTrue(totalExecutions.get() >= expectedExecutions * 0.9, 
                  "Menos de 90% das execuções processadas: " + totalExecutions.get() + "/" + expectedExecutions);
        
        // Verificar que todos os participantes ainda estão conectados
        for (QuickFixClientSimulator participant : participants) {
            assertTrue(participant.isConnected(), "Participante desconectou durante trading");
        }
        
        System.out.println("Ambiente completo de trading testado com sucesso:");
        System.out.println("- " + participants.length + " participantes");
        System.out.println("- " + totalExecutions.get() + " execuções processadas");
        System.out.println("- " + testServer.getActiveSessionCount() + " sessões ativas");
        
        // Cleanup
        for (QuickFixClientSimulator participant : participants) {
            participant.disconnect();
        }
    }
}
