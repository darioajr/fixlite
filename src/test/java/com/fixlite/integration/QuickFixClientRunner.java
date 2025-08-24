package com.fixlite.integration;

import com.fixlite.protocol.FixVersion;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Runner para executar simulador QuickFIX em ambiente containerizado.
 */
public class QuickFixClientRunner {
    
    public static void main(String[] args) throws Exception {
        String gatewayHost = System.getenv().getOrDefault("GATEWAY_HOST", "localhost");
        int gatewayPort = Integer.parseInt(System.getenv().getOrDefault("GATEWAY_PORT", "9878"));
        String clientId = System.getenv().getOrDefault("CLIENT_ID", "TEST_CLIENT");
        String fixVersionStr = System.getenv().getOrDefault("FIX_VERSION", "FIX.4.4");
        
        FixVersion fixVersion = FixVersion.fromBeginString(fixVersionStr);
        if (fixVersion == null) {
            fixVersion = FixVersion.FIX_4_4;
        }
        
        System.out.println("Iniciando QuickFIX Client: " + clientId);
        System.out.println("Gateway: " + gatewayHost + ":" + gatewayPort);
        System.out.println("FIX Version: " + fixVersionStr);
        
        QuickFixClientSimulator client = new QuickFixClientSimulator(
            gatewayHost, gatewayPort, clientId, "FIXLITE", fixVersion
        );
        
        CountDownLatch logonLatch = new CountDownLatch(1);
        CountDownLatch shutdownLatch = new CountDownLatch(1);
        
        client.setConnectionCallback(new QuickFixClientSimulator.ConnectionCallback() {
            @Override
            public void onConnected() {
                System.out.println("Cliente conectado ao gateway");
            }
            
            @Override
            public void onDisconnected() {
                System.out.println("Cliente desconectado do gateway");
                shutdownLatch.countDown();
            }
            
            @Override
            public void onLoggedOn() {
                System.out.println("Logon realizado com sucesso");
                logonLatch.countDown();
            }
        });
        
        client.setMessageCallback(message -> {
            String msgType = message.getField(com.fixlite.protocol.FixFields.MSG_TYPE);
            System.out.println("Mensagem recebida: " + msgType);
        });
        
        // Conectar
        client.connect().get(10, TimeUnit.SECONDS);
        
        if (!logonLatch.await(5, TimeUnit.SECONDS)) {
            System.err.println("Falha no logon");
            System.exit(1);
        }
        
        // Simular atividade de trading
        simulateTradingActivity(client, clientId);
        
        // Manter cliente ativo
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Desconectando cliente...");
            client.disconnect();
        }));
        
        // Aguardar sinal de shutdown
        shutdownLatch.await();
    }
    
    private static void simulateTradingActivity(QuickFixClientSimulator client, String clientId) {
        new Thread(() -> {
            try {
                String[] instruments = {"AAPL", "MSFT", "GOOGL", "TSLA", "AMZN"};
                
                for (int i = 1; i <= 50; i++) {
                    String clOrdId = clientId + "_ORDER_" + i;
                    String symbol = instruments[i % instruments.length];
                    String side = (i % 2 == 0) ? "1" : "2";
                    String qty = String.valueOf(100 * (i % 5 + 1));
                    String price = String.valueOf(100.0 + i * 0.1);
                    
                    client.sendNewOrderSingle(clOrdId, symbol, side, qty, price);
                    
                    Thread.sleep(1000 + (int)(Math.random() * 2000)); // 1-3 segundos
                    
                    // Enviar heartbeat ocasionalmente
                    if (i % 10 == 0) {
                        client.sendTestRequest("TEST_" + i);
                    }
                }
                
                System.out.println("Atividade de trading simulada concluída");
                
            } catch (Exception e) {
                System.err.println("Erro na simulação de trading: " + e.getMessage());
            }
        }).start();
    }
}
