package com.fixlite.integration;

import com.fixlite.protocol.*;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.MessageToByteEncoder;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simulador de cliente para ambiente pós-trading.
 * Simula clearing houses, custódios, CCPs e sistemas de settlement.
 */
public class PostTradingClientSimulator {
    
    private final String host;
    private final int port;
    private final String senderCompId;
    private final String targetCompId;
    private final FixVersion fixVersion;
    private final ClientType clientType;
    
    private Channel channel;
    private EventLoopGroup group;
    private final AtomicInteger msgSeqNum = new AtomicInteger(1);
    private final CountDownLatch logonLatch = new CountDownLatch(1);
    private volatile boolean loggedOn = false;
    
    // Callbacks para testes
    private MessageCallback messageCallback;
    private ConnectionCallback connectionCallback;
    
    public enum ClientType {
        CLEARING_HOUSE,     // Casa de compensação
        CUSTODIAN,          // Custodiante
        CCP,                // Central Counterparty
        SETTLEMENT_SYSTEM,  // Sistema de liquidação
        REGULATORY_REPORTER, // Reportador regulatório
        RISK_MANAGER        // Gerenciador de risco
    }
    
    public interface MessageCallback {
        void onMessage(FixMessage message);
    }
    
    public interface ConnectionCallback {
        void onConnected();
        void onDisconnected();
        void onLoggedOn();
    }
    
    public PostTradingClientSimulator(String host, int port, String senderCompId, 
                                     String targetCompId, FixVersion fixVersion, ClientType clientType) {
        this.host = host;
        this.port = port;
        this.senderCompId = senderCompId;
        this.targetCompId = targetCompId;
        this.fixVersion = fixVersion;
        this.clientType = clientType;
    }
    
    public void setMessageCallback(MessageCallback callback) {
        this.messageCallback = callback;
    }
    
    public void setConnectionCallback(ConnectionCallback callback) {
        this.connectionCallback = callback;
    }
    
    /**
     * Conecta ao FixLite gateway e realiza logon.
     */
    public CompletableFuture<Void> connect() {
        CompletableFuture<Void> future = new CompletableFuture<>();
        
        group = new NioEventLoopGroup();
        Bootstrap bootstrap = new Bootstrap();
        
        bootstrap.group(group)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ch.pipeline()
                          .addLast(new PostTradingMessageDecoder())
                          .addLast(new PostTradingMessageEncoder())
                          .addLast(new PostTradingClientHandler(future));
                    }
                });
        
        bootstrap.connect(host, port).addListener((ChannelFutureListener) channelFuture -> {
            if (channelFuture.isSuccess()) {
                this.channel = channelFuture.channel();
                if (connectionCallback != null) {
                    connectionCallback.onConnected();
                }
                sendLogon();
            } else {
                future.completeExceptionally(channelFuture.cause());
            }
        });
        
        return future;
    }
    
    /**
     * Envia mensagem de Logon específica para pós-trading.
     */
    public void sendLogon() {
        FixMessageFactory factory = new FixMessageFactory(fixVersion, senderCompId, targetCompId);
        FixMessage logon = factory.createLogon(30, null, null);
        logon.setField(FixFields.MSG_SEQ_NUM, String.valueOf(msgSeqNum.getAndIncrement()));
        logon.setField(FixFields.ENCRYPT_METHOD, "0");
        logon.setField(FixFields.HEART_BT_INT, "30");
        
        // Adicionar campos específicos do tipo de cliente
        switch (clientType) {
            case CLEARING_HOUSE:
                logon.setField(PostTradingFields.CLEARING_FIRM, senderCompId);
                break;
            case CCP:
                logon.setField(FixFields.DEFAULT_APPL_VER_ID, "9"); // FIX 5.0 SP2
                break;
            case CUSTODIAN:
                logon.setField(FixFields.APPLICATION_SYSTEM_NAME, "CUSTODIAN_SYSTEM");
                break;
            case SETTLEMENT_SYSTEM:
                logon.setField(FixFields.APPLICATION_SYSTEM_NAME, "SETTLEMENT_SYSTEM");
                break;
            case REGULATORY_REPORTER:
                logon.setField(FixFields.APPLICATION_SYSTEM_NAME, "REGULATORY_SYSTEM");
                break;
            case RISK_MANAGER:
                logon.setField(FixFields.APPLICATION_SYSTEM_NAME, "RISK_SYSTEM");
                break;
        }
        
        sendMessage(logon);
    }
    
    /**
     * Envia mensagem FIX através do canal.
     */
    private void sendMessage(FixMessage message) {
        if (channel != null && channel.isActive()) {
            System.out.println("Enviando mensagem: " + message.getField(FixFields.MSG_TYPE) + 
                             " de " + message.getField(FixFields.SENDER_COMP_ID));
            channel.writeAndFlush(message);
        } else {
            System.out.println("ERRO: Canal não está ativo para envio de mensagem");
        }
    }
    
    /**
     * Envia Trade Confirmation.
     */
    public void sendTradeConfirmation(String confirmId, String tradeId, String symbol, 
                                     String side, String quantity, String price) {
        FixMessage confirmation = PostTradingMessageFactory.createTradeConfirmation(
            fixVersion, senderCompId, targetCompId, confirmId, tradeId, symbol, side, quantity, price
        );
        confirmation.setField(FixFields.MSG_SEQ_NUM, String.valueOf(msgSeqNum.getAndIncrement()));
        sendMessage(confirmation);
    }
    
    /**
     * Envia Allocation Instruction.
     */
    public void sendAllocationInstruction(String allocId, String symbol, String side, String quantity) {
        FixMessage allocation = PostTradingMessageFactory.createAllocationInstruction(
            fixVersion, senderCompId, targetCompId, allocId, symbol, side, quantity
        );
        allocation.setField(FixFields.MSG_SEQ_NUM, String.valueOf(msgSeqNum.getAndIncrement()));
        sendMessage(allocation);
    }
    
    /**
     * Envia Settlement Instructions.
     */
    public void sendSettlementInstructions(String settlInstId, String symbol, String settlDate, String currency) {
        FixMessage settlement = PostTradingMessageFactory.createSettlementInstructions(
            fixVersion, senderCompId, targetCompId, settlInstId, symbol, settlDate, currency
        );
        settlement.setField(FixFields.MSG_SEQ_NUM, String.valueOf(msgSeqNum.getAndIncrement()));
        sendMessage(settlement);
    }
    
    /**
     * Solicita Position Report.
     */
    public void requestPositions(String posReqId, String account, String symbol) {
        FixMessage posRequest = PostTradingMessageFactory.createRequestForPositions(
            fixVersion, senderCompId, targetCompId, posReqId, account, symbol
        );
        posRequest.setField(FixFields.MSG_SEQ_NUM, String.valueOf(msgSeqNum.getAndIncrement()));
        sendMessage(posRequest);
    }
    
    /**
     * Envia Trade Capture Report.
     */
    public void sendTradeCaptureReport(String tradeReportId, String symbol, String side, 
                                      String quantity, String price, String tradeDate) {
        FixMessage tradeCapture = PostTradingMessageFactory.createTradeCaptureReport(
            fixVersion, senderCompId, targetCompId, tradeReportId, symbol, side, quantity, price, tradeDate
        );
        tradeCapture.setField(FixFields.MSG_SEQ_NUM, String.valueOf(msgSeqNum.getAndIncrement()));
        sendMessage(tradeCapture);
    }
    
    /**
     * Envia Collateral Assignment.
     */
    public void sendCollateralAssignment(String collateralAssignmentId, String account, 
                                        String collateralAmt, String currency) {
        FixMessage collateral = PostTradingMessageFactory.createCollateralAssignment(
            fixVersion, senderCompId, targetCompId, collateralAssignmentId, account, collateralAmt, currency
        );
        collateral.setField(FixFields.MSG_SEQ_NUM, String.valueOf(msgSeqNum.getAndIncrement()));
        sendMessage(collateral);
    }
    
    /**
     * Envia Margin Requirement Report.
     */
    public void sendMarginRequirementReport(String marginReqId, String account, String marginAmt, String currency) {
        FixMessage marginReport = PostTradingMessageFactory.createMarginRequirementReport(
            fixVersion, senderCompId, targetCompId, marginReqId, account, marginAmt, currency
        );
        marginReport.setField(FixFields.MSG_SEQ_NUM, String.valueOf(msgSeqNum.getAndIncrement()));
        sendMessage(marginReport);
    }
    
    /**
     * Simula fluxo completo de pós-trading para um trade.
     */
    public void simulatePostTradingFlow(String baseId, String symbol, String side, String quantity, String price) {
        // 1. Trade Confirmation
        sendTradeConfirmation("CONF_" + baseId, "TRADE_" + baseId, symbol, side, quantity, price);
        
        // 2. Allocation (após pequeno delay)
        try { Thread.sleep(100); } catch (InterruptedException e) {}
        sendAllocationInstruction("ALLOC_" + baseId, symbol, side, quantity);
        
        // 3. Settlement Instructions
        try { Thread.sleep(100); } catch (InterruptedException e) {}
        String settlDate = LocalDate.now().plusDays(2).format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        sendSettlementInstructions("SETTL_" + baseId, symbol, settlDate, "USD");
        
        // 4. Collateral (se necessário)
        if (clientType == ClientType.CCP || clientType == ClientType.CLEARING_HOUSE) {
            try { Thread.sleep(100); } catch (InterruptedException e) {}
            sendCollateralAssignment("COLL_" + baseId, "ACCOUNT_" + baseId, "10000", "USD");
        }
        
        // 5. Margin Requirement (se aplicável)
        if (clientType == ClientType.CLEARING_HOUSE || clientType == ClientType.RISK_MANAGER) {
            try { Thread.sleep(100); } catch (InterruptedException e) {}
            sendMarginRequirementReport("MARGIN_" + baseId, "ACCOUNT_" + baseId, "5000", "USD");
        }
    }
    
    public boolean waitForLogon(long timeout, TimeUnit unit) throws InterruptedException {
        return logonLatch.await(timeout, unit);
    }
    
    /**
     * Desconecta do gateway.
     */
    public void disconnect() {
        if (channel != null) {
            channel.close();
        }
        if (group != null) {
            group.shutdownGracefully();
        }
    }
    
    /**
     * Handler para processar mensagens de pós-trading.
     */
    private class PostTradingClientHandler extends SimpleChannelInboundHandler<FixMessage> {
        private final CompletableFuture<Void> connectionFuture;
        
        public PostTradingClientHandler(CompletableFuture<Void> connectionFuture) {
            this.connectionFuture = connectionFuture;
        }
        
        @Override
        protected void channelRead0(ChannelHandlerContext ctx, FixMessage msg) {
            String msgType = msg.getField(FixFields.MSG_TYPE);
            
            // Processar Logon response
            if (FixMessageTypes.LOGON.equals(msgType)) {
                loggedOn = true;
                logonLatch.countDown();
                if (connectionCallback != null) {
                    connectionCallback.onLoggedOn();
                }
                connectionFuture.complete(null);
            }
            
            // Processar mensagens específicas de pós-trading
            if (PostTradingMessageTypes.isPostTradingMessage(msgType)) {
                handlePostTradingMessage(msg);
            }
            
            // Responder a TestRequest com Heartbeat
            if (FixMessageTypes.TEST_REQUEST.equals(msgType)) {
                String testReqId = msg.getField(FixFields.TEST_REQ_ID);
                FixMessageFactory factory = new FixMessageFactory(fixVersion, senderCompId, targetCompId);
        FixMessage heartbeat = factory.createHeartbeat(null);
                heartbeat.setField(FixFields.MSG_SEQ_NUM, String.valueOf(msgSeqNum.getAndIncrement()));
                heartbeat.setField(FixFields.TEST_REQ_ID, testReqId);
                sendMessage(heartbeat);
            }
            
            // Callback para testes
            if (messageCallback != null) {
                messageCallback.onMessage(msg);
            }
        }
        
        private void handlePostTradingMessage(FixMessage msg) {
            String msgType = msg.getField(FixFields.MSG_TYPE);
            
            switch (msgType) {
                case PostTradingMessageTypes.CONFIRMATION:
                    // Responder com Confirmation Ack
                    String confirmId = msg.getField(PostTradingFields.CONFIRM_ID);
                    FixMessage confirmAck = PostTradingMessageFactory.createConfirmationAck(
                        fixVersion, senderCompId, targetCompId, confirmId, "1" // Confirmed
                    );
                    confirmAck.setField(FixFields.MSG_SEQ_NUM, String.valueOf(msgSeqNum.getAndIncrement()));
                    sendMessage(confirmAck);
                    break;
                    
                case PostTradingMessageTypes.REQUEST_FOR_POSITIONS:
                    // Responder com Position Report
                    String posReqId = msg.getField(PostTradingFields.POS_REQ_ID);
                    String symbol = msg.getField(FixFields.SYMBOL);
                    String account = msg.getField(FixFields.ACCOUNT);
                    FixMessage posReport = PostTradingMessageFactory.createPositionReport(
                        fixVersion, senderCompId, targetCompId, posReqId, symbol, account, "1000", "0"
                    );
                    posReport.setField(FixFields.MSG_SEQ_NUM, String.valueOf(msgSeqNum.getAndIncrement()));
                    sendMessage(posReport);
                    break;
                    
                case PostTradingMessageTypes.ALLOCATION_INSTRUCTION:
                    // Responder com Allocation Report
                    String allocId = msg.getField(PostTradingFields.ALLOC_ID);
                    FixMessage allocReport = PostTradingMessageFactory.createAllocationReport(
                        fixVersion, senderCompId, targetCompId, "RPT_" + allocId, allocId, "0" // Accepted
                    );
                    allocReport.setField(FixFields.MSG_SEQ_NUM, String.valueOf(msgSeqNum.getAndIncrement()));
                    sendMessage(allocReport);
                    break;
            }
        }
        
        @Override
        public void channelInactive(ChannelHandlerContext ctx) {
            loggedOn = false;
            if (connectionCallback != null) {
                connectionCallback.onDisconnected();
            }
        }
        
        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            cause.printStackTrace();
            connectionFuture.completeExceptionally(cause);
            ctx.close();
        }
    }
    
    /**
     * Decoder para mensagens FIX de pós-trading.
     */
    private static class PostTradingMessageDecoder extends ByteToMessageDecoder {
        @Override
        protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
            if (in.readableBytes() < 20) return;
            
            int startIndex = in.readerIndex();
            
            try {
                if (FixBinaryCodec.isValidFixMessage(in)) {
                    FixMessage message = FixBinaryCodec.parse(in);
                    if (message != null) {
                        out.add(message);
                        return;
                    }
                }
                
                in.readerIndex(startIndex);
                String content = in.toString(StandardCharsets.US_ASCII);
                
                if (content.contains("8=") && content.contains("10=")) {
                    int checksumEnd = content.lastIndexOf("10=");
                    if (checksumEnd > 0) {
                        int messageEnd = content.indexOf('\001', checksumEnd + 6);
                        if (messageEnd > 0) {
                            String fixMessage = content.substring(0, messageEnd + 1);
                            FixMessage parsed = FixParser.parse(fixMessage);
                            out.add(parsed);
                            in.readerIndex(startIndex + messageEnd + 1);
                        }
                    }
                }
            } catch (Exception e) {
                in.readerIndex(startIndex);
            }
        }
    }
    
    /**
     * Encoder para mensagens FIX de pós-trading.
     */
    private static class PostTradingMessageEncoder extends MessageToByteEncoder<FixMessage> {
        @Override
        protected void encode(ChannelHandlerContext ctx, FixMessage msg, ByteBuf out) {
            try {
                ByteBuf encoded = FixBinaryCodec.encode(msg);
                out.writeBytes(encoded);
                encoded.release();
            } catch (Exception e) {
                String fixString = msg.encode();
                out.writeBytes(fixString.getBytes(StandardCharsets.US_ASCII));
            }
        }
    }
    
    public boolean isLoggedOn() {
        return loggedOn;
    }
    
    public boolean isConnected() {
        return channel != null && channel.isActive();
    }
    
    public ClientType getClientType() {
        return clientType;
    }
}
