package com.fixlite.integration;

import com.fixlite.protocol.FixVersion;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Runner para executar simulador de cliente pós-trading em ambiente containerizado.
 */
public class PostTradingClientRunner {
    
    public static void main(String[] args) throws Exception {
        String gatewayHost = System.getenv().getOrDefault("GATEWAY_HOST", "localhost");
        int gatewayPort = Integer.parseInt(System.getenv().getOrDefault("GATEWAY_PORT", "9878"));
        String clientId = System.getenv().getOrDefault("CLIENT_ID", "POST_TRADING_CLIENT");
        String clientTypeStr = System.getenv().getOrDefault("CLIENT_TYPE", "CLEARING_HOUSE");
        String fixVersionStr = System.getenv().getOrDefault("FIX_VERSION", "FIX.4.4");
        
        FixVersion fixVersion = FixVersion.fromBeginString(fixVersionStr);
        if (fixVersion == null) {
            fixVersion = FixVersion.FIX_4_4;
        }
        
        PostTradingClientSimulator.ClientType clientType;
        try {
            clientType = PostTradingClientSimulator.ClientType.valueOf(clientTypeStr);
        } catch (IllegalArgumentException e) {
            clientType = PostTradingClientSimulator.ClientType.CLEARING_HOUSE;
        }
        
        System.out.println("Iniciando Post-Trading Client: " + clientId);
        System.out.println("Tipo: " + clientType);
        System.out.println("Gateway: " + gatewayHost + ":" + gatewayPort);
        System.out.println("FIX Version: " + fixVersionStr);
        
        PostTradingClientSimulator client = new PostTradingClientSimulator(
            gatewayHost, gatewayPort, clientId, "FIXLITE", fixVersion, clientType
        );
        
        CountDownLatch logonLatch = new CountDownLatch(1);
        CountDownLatch shutdownLatch = new CountDownLatch(1);
        
        client.setConnectionCallback(new PostTradingClientSimulator.ConnectionCallback() {
            @Override
            public void onConnected() {
                System.out.println("Cliente pós-trading conectado ao gateway");
            }
            
            @Override
            public void onDisconnected() {
                System.out.println("Cliente pós-trading desconectado do gateway");
                shutdownLatch.countDown();
            }
            
            @Override
            public void onLoggedOn() {
                System.out.println("Logon pós-trading realizado com sucesso");
                logonLatch.countDown();
            }
        });
        
        client.setMessageCallback(message -> {
            String msgType = message.getField(com.fixlite.protocol.FixFields.MSG_TYPE);
            System.out.println("Mensagem recebida: " + msgType + " - " + 
                com.fixlite.protocol.PostTradingMessageTypes.getMessageDescription(msgType));
        });
        
        // Conectar
        client.connect().get(10, TimeUnit.SECONDS);
        
        if (!logonLatch.await(5, TimeUnit.SECONDS)) {
            System.err.println("Falha no logon pós-trading");
            System.exit(1);
        }
        
        // Simular atividade específica do tipo de cliente
        simulatePostTradingActivity(client, clientId, clientType);
        
        // Manter cliente ativo
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Desconectando cliente pós-trading...");
            client.disconnect();
        }));
        
        // Aguardar sinal de shutdown
        shutdownLatch.await();
    }
    
    private static void simulatePostTradingActivity(PostTradingClientSimulator client, 
                                                   String clientId, 
                                                   PostTradingClientSimulator.ClientType clientType) {
        new Thread(() -> {
            try {
                String[] symbols = {"AAPL", "MSFT", "GOOGL", "TSLA", "AMZN"};
                String[] accounts = {"ACCOUNT_A", "ACCOUNT_B", "ACCOUNT_C"};
                
                for (int i = 1; i <= 20; i++) {
                    String baseId = clientId + "_" + i;
                    String symbol = symbols[i % symbols.length];
                    String side = (i % 2 == 0) ? "1" : "2";
                    String quantity = String.valueOf(100 * (i % 5 + 1));
                    String price = String.valueOf(100.0 + i * 0.5);
                    String account = accounts[i % accounts.length];
                    
                    switch (clientType) {
                        case CLEARING_HOUSE:
                            client.sendTradeConfirmation("CONF_" + baseId, "TRADE_" + baseId, 
                                                        symbol, side, quantity, price);
                            client.sendAllocationInstruction("ALLOC_" + baseId, symbol, side, quantity);
                            break;
                            
                        case CCP:
                            client.sendTradeCaptureReport("TCR_" + baseId, symbol, side, quantity, price, "20250824");
                            client.sendCollateralAssignment("COLL_" + baseId, account, "10000", "USD");
                            client.sendMarginRequirementReport("MARGIN_" + baseId, account, "5000", "USD");
                            break;
                            
                        case CUSTODIAN:
                            client.sendSettlementInstructions("SETTL_" + baseId, symbol, "20250826", "USD");
                            client.requestPositions("POS_" + baseId, account, symbol);
                            break;
                            
                        case SETTLEMENT_SYSTEM:
                            client.sendSettlementInstructions("SETTL_" + baseId, symbol, "20250826", "USD");
                            client.sendSettlementInstructions("SETTL_EUR_" + baseId, symbol, "20250826", "EUR");
                            break;
                            
                        case RISK_MANAGER:
                            client.sendMarginRequirementReport("MARGIN_" + baseId, account, 
                                                              String.valueOf(5000 + i * 1000), "USD");
                            client.requestPositions("RISK_POS_" + baseId, account, null);
                            break;
                            
                        case REGULATORY_REPORTER:
                            client.sendTradeCaptureReport("REG_" + baseId, symbol, side, quantity, price, "20250824");
                            client.sendTradeConfirmation("REG_CONF_" + baseId, "REG_TRADE_" + baseId, 
                                                        symbol, side, quantity, price);
                            break;
                    }
                    
                    Thread.sleep(2000 + (int)(Math.random() * 3000)); // 2-5 segundos
                    
                    // Ocasionalmente simular fluxo completo
                    if (i % 5 == 0) {
                        client.simulatePostTradingFlow("FULL_" + baseId, symbol, side, quantity, price);
                    }
                }
                
                System.out.println("Atividade pós-trading simulada concluída para " + clientType);
                
            } catch (Exception e) {
                System.err.println("Erro na simulação pós-trading: " + e.getMessage());
            }
        }).start();
    }
}
