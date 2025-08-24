package com.fixlite.integration;

import com.fixlite.protocol.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Testes de integração end-to-end com simuladores QuickFIX.
 * Valida compatibilidade completa com clientes FIX reais.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class FixLiteIntegrationTest {
    
    private static final String GATEWAY_HOST = "localhost";
    private static final int GATEWAY_PORT = 9878;
    
    private static FixGatewayTestServer testServer;
    private QuickFixClientSimulator client1;
    private QuickFixClientSimulator client2;
    private QuickFixClientSimulator client3;
    
    private final List<FixMessage> receivedMessages = Collections.synchronizedList(new ArrayList<>());
    
    @BeforeAll
    static void startTestServer() throws InterruptedException {
        testServer = new FixGatewayTestServer(GATEWAY_PORT);
        testServer.start();
        Thread.sleep(1000); // Aguardar servidor inicializar
    }
    
    @AfterAll
    static void stopTestServer() {
        if (testServer != null) {
            testServer.stop();
        }
    }
    
    @BeforeEach
    public void setUp() {
        receivedMessages.clear();
        
        // Cliente FIX 4.4
        client1 = new QuickFixClientSimulator(
            GATEWAY_HOST, GATEWAY_PORT, 
            "CLIENT1", "FIXLITE", 
            FixVersion.FIX_4_4
        );
        
        // Cliente FIX 5.0 SP2
        client2 = new QuickFixClientSimulator(
            GATEWAY_HOST, GATEWAY_PORT,
            "CLIENT2", "FIXLITE",
            FixVersion.FIX_5_0_SP2
        );
        
        // Cliente com modelo customizado
        client3 = new QuickFixClientSimulator(
            GATEWAY_HOST, GATEWAY_PORT,
            "CLIENT3", "FIXLITE",
            FixVersion.CUSTOM
        );
        
        // Configurar callbacks para capturar mensagens
        QuickFixClientSimulator.MessageCallback messageCapture = receivedMessages::add;
        client1.setMessageCallback(messageCapture);
        client2.setMessageCallback(messageCapture);
        client3.setMessageCallback(messageCapture);
    }
    
    @AfterEach
    public void tearDown() {
        if (client1 != null) client1.disconnect();
        if (client2 != null) client2.disconnect();
        if (client3 != null) client3.disconnect();
    }
    
    @Test
    @Order(1)
    public void testSingleClientLogon() throws Exception {
        CountDownLatch logonLatch = new CountDownLatch(1);
        
        client1.setConnectionCallback(new QuickFixClientSimulator.ConnectionCallback() {
            @Override
            public void onConnected() {}
            
            @Override
            public void onDisconnected() {}
            
            @Override
            public void onLoggedOn() {
                logonLatch.countDown();
            }
        });
        
        // Conectar e aguardar logon
        CompletableFuture<Void> connectFuture = client1.connect();
        connectFuture.get(10, TimeUnit.SECONDS);
        
        assertTrue(logonLatch.await(5, TimeUnit.SECONDS), "Logon não completado");
        assertTrue(client1.isLoggedOn(), "Cliente não está logado");
        assertTrue(client1.isConnected(), "Cliente não está conectado");
    }
    
    @Test
    @Order(2)
    public void testMultipleClientsLogon() throws Exception {
        CountDownLatch allLoggedOn = new CountDownLatch(3);
        
        QuickFixClientSimulator.ConnectionCallback logonCallback = 
            new QuickFixClientSimulator.ConnectionCallback() {
                @Override
                public void onConnected() {}
                
                @Override
                public void onDisconnected() {}
                
                @Override
                public void onLoggedOn() {
                    allLoggedOn.countDown();
                }
            };
        
        client1.setConnectionCallback(logonCallback);
        client2.setConnectionCallback(logonCallback);
        client3.setConnectionCallback(logonCallback);
        
        // Conectar todos os clientes simultaneamente
        CompletableFuture<Void> connect1 = client1.connect();
        CompletableFuture<Void> connect2 = client2.connect();
        CompletableFuture<Void> connect3 = client3.connect();
        
        CompletableFuture.allOf(connect1, connect2, connect3).get(15, TimeUnit.SECONDS);
        
        assertTrue(allLoggedOn.await(10, TimeUnit.SECONDS), "Nem todos os clientes fizeram logon");
        assertTrue(client1.isLoggedOn() && client2.isLoggedOn() && client3.isLoggedOn());
    }
    
    @Test
    @Order(3)
    public void testNewOrderSingleCompatibility() throws Exception {
        // Conectar cliente
        client1.connect().get(10, TimeUnit.SECONDS);
        assertTrue(client1.waitForLogon(5, TimeUnit.SECONDS));
        
        CountDownLatch orderResponseLatch = new CountDownLatch(1);
        AtomicReference<FixMessage> executionReport = new AtomicReference<>();
        
        client1.setMessageCallback(message -> {
            if (FixMessageTypes.EXECUTION_REPORT.equals(message.getField(FixFields.MSG_TYPE))) {
                executionReport.set(message);
                orderResponseLatch.countDown();
            }
        });
        
        // Enviar NewOrderSingle
        client1.sendNewOrderSingle("ORDER001", "MSFT", "1", "100", "150.50");
        
        // Aguardar ExecutionReport (simulado pelo gateway)
        assertTrue(orderResponseLatch.await(5, TimeUnit.SECONDS), "ExecutionReport não recebido");
        
        FixMessage execReport = executionReport.get();
        assertNotNull(execReport);
        assertEquals("ORDER001", execReport.getField(FixFields.CL_ORD_ID));
        assertEquals("MSFT", execReport.getField(FixFields.SYMBOL));
    }
    
    @Test
    @Order(4)
    public void testHeartbeatMechanism() throws Exception {
        client1.connect().get(10, TimeUnit.SECONDS);
        assertTrue(client1.waitForLogon(5, TimeUnit.SECONDS));
        
        CountDownLatch heartbeatLatch = new CountDownLatch(1);
        String testReqId = "TEST123";
        
        client1.setMessageCallback(message -> {
            if (FixMessageTypes.HEARTBEAT.equals(message.getField(FixFields.MSG_TYPE))) {
                String receivedTestReqId = message.getField(FixFields.TEST_REQ_ID);
                if (testReqId.equals(receivedTestReqId)) {
                    heartbeatLatch.countDown();
                }
            }
        });
        
        // Enviar TestRequest
        client1.sendTestRequest(testReqId);
        
        // Aguardar Heartbeat response
        assertTrue(heartbeatLatch.await(3, TimeUnit.SECONDS), "Heartbeat não recebido");
    }
    
    @Test
    @Order(5)
    public void testMultiVersionInteroperability() throws Exception {
        // Conectar clientes com diferentes versões FIX
        CompletableFuture<Void> connect44 = client1.connect(); // FIX 4.4
        CompletableFuture<Void> connect50 = client2.connect(); // FIX 5.0 SP2
        
        CompletableFuture.allOf(connect44, connect50).get(15, TimeUnit.SECONDS);
        
        assertTrue(client1.waitForLogon(5, TimeUnit.SECONDS));
        assertTrue(client2.waitForLogon(5, TimeUnit.SECONDS));
        
        // Enviar ordens de diferentes versões
        client1.sendNewOrderSingle("ORDER44", "AAPL", "1", "50", "175.25");
        client2.sendNewOrderSingle("ORDER50", "GOOGL", "2", "25", "2800.75");
        
        // Verificar que ambas foram processadas
        Thread.sleep(1000); // Aguardar processamento
        
        long fix44Messages = receivedMessages.stream()
            .filter(msg -> "FIX.4.4".equals(msg.getField(FixFields.BEGIN_STRING)))
            .count();
        
        long fix50Messages = receivedMessages.stream()
            .filter(msg -> "FIXT.1.1".equals(msg.getField(FixFields.BEGIN_STRING)))
            .count();
        
        assertTrue(fix44Messages > 0, "Mensagens FIX 4.4 não processadas");
        assertTrue(fix50Messages > 0, "Mensagens FIX 5.0 não processadas");
    }
    
    @Test
    @Order(6)
    public void testHighVolumeTrading() throws Exception {
        client1.connect().get(10, TimeUnit.SECONDS);
        assertTrue(client1.waitForLogon(5, TimeUnit.SECONDS));
        
        int orderCount = 100;
        CountDownLatch ordersLatch = new CountDownLatch(orderCount);
        
        client1.setMessageCallback(message -> {
            if (FixMessageTypes.EXECUTION_REPORT.equals(message.getField(FixFields.MSG_TYPE))) {
                ordersLatch.countDown();
            }
        });
        
        // Enviar múltiplas ordens rapidamente
        long startTime = System.currentTimeMillis();
        
        for (int i = 1; i <= orderCount; i++) {
            client1.sendNewOrderSingle(
                "ORDER" + i, 
                "STOCK" + (i % 10), 
                "1", 
                String.valueOf(100 + i), 
                String.valueOf(100.0 + i * 0.1)
            );
        }
        
        // Aguardar todas as respostas
        assertTrue(ordersLatch.await(30, TimeUnit.SECONDS), 
                  "Nem todas as ordens foram processadas");
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        // Verificar performance (deve processar 100 ordens em menos de 10 segundos)
        assertTrue(duration < 10000, 
                  "Performance inadequada: " + duration + "ms para " + orderCount + " ordens");
        
        System.out.println("Processadas " + orderCount + " ordens em " + duration + "ms");
    }
    
    @Test
    @Order(7)
    public void testErrorHandlingAndRecovery() throws Exception {
        client1.connect().get(10, TimeUnit.SECONDS);
        assertTrue(client1.waitForLogon(5, TimeUnit.SECONDS));
        
        // Enviar mensagem inválida
        FixMessage invalidMessage = new FixMessage("INVALID");
        invalidMessage.setVersion(FixVersion.FIX_4_4);
        invalidMessage.setField(FixFields.SENDER_COMP_ID, "CLIENT1");
        invalidMessage.setField(FixFields.TARGET_COMP_ID, "FIXLITE");
        
        client1.sendMessage(invalidMessage);
        
        // Cliente deve continuar conectado após mensagem inválida
        Thread.sleep(1000);
        assertTrue(client1.isConnected(), "Cliente desconectado após mensagem inválida");
        
        // Enviar mensagem válida para confirmar recuperação
        CountDownLatch recoveryLatch = new CountDownLatch(1);
        client1.setMessageCallback(message -> {
            if (FixMessageTypes.HEARTBEAT.equals(message.getField(FixFields.MSG_TYPE))) {
                recoveryLatch.countDown();
            }
        });
        
        client1.sendTestRequest("RECOVERY_TEST");
        assertTrue(recoveryLatch.await(3, TimeUnit.SECONDS), "Recuperação falhou");
    }
    
    @Test
    @Order(8)
    public void testChecksumValidation() throws Exception {
        client1.connect().get(10, TimeUnit.SECONDS);
        assertTrue(client1.waitForLogon(5, TimeUnit.SECONDS));
        
        // Criar mensagem com checksum correto
        FixMessageFactory factory = new FixMessageFactory(FixVersion.FIX_4_4, "CLIENT1", "GATEWAY");
        FixMessage heartbeat = factory.createHeartbeat(null);
        heartbeat.setField(FixFields.MSG_SEQ_NUM, "100");
        
        // Verificar que a mensagem codificada tem checksum válido
        String encoded = heartbeat.encode();
        assertTrue(FixParser.validateChecksum(encoded), "Checksum inválido na mensagem codificada");
        
        // Enviar mensagem e verificar que foi aceita
        CountDownLatch responseLatch = new CountDownLatch(1);
        client1.setMessageCallback(message -> responseLatch.countDown());
        
        client1.sendMessage(heartbeat);
        
        // Se o gateway aceitar a mensagem, o checksum está correto
        assertTrue(client1.isConnected(), "Conexão perdida - possível problema de checksum");
    }
    
    @Test
    @Order(9)
    public void testConcurrentClientOperations() throws Exception {
        // Conectar múltiplos clientes
        CompletableFuture<Void> connect1 = client1.connect();
        CompletableFuture<Void> connect2 = client2.connect();
        CompletableFuture<Void> connect3 = client3.connect();
        
        CompletableFuture.allOf(connect1, connect2, connect3).get(15, TimeUnit.SECONDS);
        
        assertTrue(client1.waitForLogon(5, TimeUnit.SECONDS));
        assertTrue(client2.waitForLogon(5, TimeUnit.SECONDS));
        assertTrue(client3.waitForLogon(5, TimeUnit.SECONDS));
        
        CountDownLatch concurrentOpsLatch = new CountDownLatch(9); // 3 ops per client
        
        QuickFixClientSimulator.MessageCallback concurrentCallback = message -> {
            if (FixMessageTypes.EXECUTION_REPORT.equals(message.getField(FixFields.MSG_TYPE)) ||
                FixMessageTypes.HEARTBEAT.equals(message.getField(FixFields.MSG_TYPE))) {
                concurrentOpsLatch.countDown();
            }
        };
        
        client1.setMessageCallback(concurrentCallback);
        client2.setMessageCallback(concurrentCallback);
        client3.setMessageCallback(concurrentCallback);
        
        // Executar operações concorrentes
        CompletableFuture.runAsync(() -> {
            client1.sendNewOrderSingle("CONC1", "STOCK1", "1", "100", "50.0");
            client1.sendNewOrderSingle("CONC2", "STOCK2", "2", "200", "75.0");
            client1.sendTestRequest("TEST1");
        });
        
        CompletableFuture.runAsync(() -> {
            client2.sendNewOrderSingle("CONC3", "STOCK3", "1", "150", "100.0");
            client2.sendNewOrderSingle("CONC4", "STOCK4", "2", "250", "125.0");
            client2.sendTestRequest("TEST2");
        });
        
        CompletableFuture.runAsync(() -> {
            client3.sendNewOrderSingle("CONC5", "STOCK5", "1", "300", "200.0");
            client3.sendNewOrderSingle("CONC6", "STOCK6", "2", "400", "250.0");
            client3.sendTestRequest("TEST3");
        });
        
        assertTrue(concurrentOpsLatch.await(15, TimeUnit.SECONDS), 
                  "Operações concorrentes não completadas");
        
        // Verificar que todos os clientes ainda estão conectados
        assertTrue(client1.isConnected() && client2.isConnected() && client3.isConnected(),
                  "Algum cliente desconectou durante operações concorrentes");
    }
}
