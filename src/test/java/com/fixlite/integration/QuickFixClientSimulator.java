package com.fixlite.integration;

import com.fixlite.protocol.*;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.MessageToByteEncoder;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simulador de cliente QuickFIX para testes de interoperabilidade.
 * Simula comportamento real de clientes FIX conectando ao FixLite gateway.
 */
public class QuickFixClientSimulator {
    
    private final String host;
    private final int port;
    private final String senderCompId;
    private final String targetCompId;
    private final FixVersion fixVersion;
    
    private Channel channel;
    private EventLoopGroup group;
    private final AtomicInteger msgSeqNum = new AtomicInteger(1);
    private final CountDownLatch logonLatch = new CountDownLatch(1);
    private volatile boolean loggedOn = false;
    
    // Callbacks para testes
    private MessageCallback messageCallback;
    private ConnectionCallback connectionCallback;
    
    public interface MessageCallback {
        void onMessage(FixMessage message);
    }
    
    public interface ConnectionCallback {
        void onConnected();
        void onDisconnected();
        void onLoggedOn();
    }
    
    public QuickFixClientSimulator(String host, int port, String senderCompId, 
                                  String targetCompId, FixVersion fixVersion) {
        this.host = host;
        this.port = port;
        this.senderCompId = senderCompId;
        this.targetCompId = targetCompId;
        this.fixVersion = fixVersion;
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
                          .addLast(new FixMessageDecoder())
                          .addLast(new FixMessageEncoder())
                          .addLast(new QuickFixClientHandler(future));
                    }
                });
        
        bootstrap.connect(host, port).addListener((ChannelFutureListener) channelFuture -> {
            if (channelFuture.isSuccess()) {
                this.channel = channelFuture.channel();
                if (connectionCallback != null) {
                    connectionCallback.onConnected();
                }
                // Enviar Logon automaticamente
                sendLogon();
            } else {
                future.completeExceptionally(channelFuture.cause());
            }
        });
        
        return future;
    }
    
    /**
     * Envia mensagem de Logon compatível com QuickFIX.
     */
    public void sendLogon() {
        FixMessageFactory factory = new FixMessageFactory(fixVersion, senderCompId, targetCompId);
        FixMessage logon = factory.createLogon(30, null, null);
        logon.setField(FixFields.MSG_SEQ_NUM, String.valueOf(msgSeqNum.getAndIncrement()));
        logon.setField(FixFields.ENCRYPT_METHOD, "0");
        logon.setField(FixFields.HEART_BT_INT, "30");
        logon.setField(FixFields.SENDING_TIME, getCurrentTimestamp());
        
        sendMessage(logon);
    }
    
    /**
     * Envia NewOrderSingle para teste de compatibilidade.
     */
    public void sendNewOrderSingle(String clOrdId, String symbol, String side, 
                                  String orderQty, String price) {
        FixMessageFactory factory = new FixMessageFactory(fixVersion, senderCompId, targetCompId);
        FixMessage order = factory.createNewOrderSingle(
            clOrdId, symbol, side.charAt(0), Double.parseDouble(orderQty), '2', Double.parseDouble(price)
        );
        order.setField(FixFields.MSG_SEQ_NUM, String.valueOf(msgSeqNum.getAndIncrement()));
        order.setField(FixFields.ORD_TYPE, "2"); // Limit order
        order.setField(FixFields.PRICE, price);
        order.setField(FixFields.TIME_IN_FORCE, "0"); // Day
        order.setField(FixFields.SENDING_TIME, getCurrentTimestamp());
        
        sendMessage(order);
    }
    
    /**
     * Envia Heartbeat.
     */
    public void sendHeartbeat(String testReqId) {
        FixMessageFactory factory = new FixMessageFactory(fixVersion, senderCompId, targetCompId);
        FixMessage heartbeat = factory.createHeartbeat(testReqId);
        heartbeat.setField(FixFields.MSG_SEQ_NUM, String.valueOf(msgSeqNum.getAndIncrement()));
        heartbeat.setField(FixFields.SENDING_TIME, getCurrentTimestamp());
        
        sendMessage(heartbeat);
    }
    
    /**
     * Envia TestRequest.
     */
    public void sendTestRequest(String testReqId) {
        FixMessage testRequest = new FixMessage(FixMessageTypes.TEST_REQUEST);
        testRequest.setVersion(fixVersion);
        testRequest.setField(FixFields.SENDER_COMP_ID, senderCompId);
        testRequest.setField(FixFields.TARGET_COMP_ID, targetCompId);
        testRequest.setField(FixFields.MSG_SEQ_NUM, String.valueOf(msgSeqNum.getAndIncrement()));
        testRequest.setField(FixFields.TEST_REQ_ID, testReqId);
        testRequest.setField(FixFields.SENDING_TIME, getCurrentTimestamp());
        
        sendMessage(testRequest);
    }
    
    /**
     * Envia mensagem genérica.
     */
    public void sendMessage(FixMessage message) {
        if (channel != null && channel.isActive()) {
            channel.writeAndFlush(message);
        }
    }
    
    /**
     * Aguarda logon ser completado.
     */
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
    
    private String getCurrentTimestamp() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HH:mm:ss.SSS"));
    }
    
    /**
     * Handler para processar mensagens recebidas.
     */
    private class QuickFixClientHandler extends SimpleChannelInboundHandler<FixMessage> {
        private final CompletableFuture<Void> connectionFuture;
        
        public QuickFixClientHandler(CompletableFuture<Void> connectionFuture) {
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
            
            // Responder a TestRequest com Heartbeat
            if (FixMessageTypes.TEST_REQUEST.equals(msgType)) {
                String testReqId = msg.getField(FixFields.TEST_REQ_ID);
                sendHeartbeat(testReqId);
            }
            
            // Callback para testes
            if (messageCallback != null) {
                messageCallback.onMessage(msg);
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
     * Decoder para mensagens FIX binárias.
     */
    private static class FixMessageDecoder extends ByteToMessageDecoder {
        @Override
        protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
            // Procurar por mensagem FIX completa
            if (in.readableBytes() < 20) return; // Mensagem mínima
            
            int startIndex = in.readerIndex();
            
            // Verificar se é uma mensagem FIX válida
            if (!FixBinaryCodec.isValidFixMessage(in)) {
                // Tentar parsing como string
                String fixString = in.toString(StandardCharsets.US_ASCII);
                if (fixString.contains("8=") && fixString.contains("10=")) {
                    try {
                        FixMessage message = FixParser.parse(fixString);
                        out.add(message);
                        in.readerIndex(in.writerIndex()); // Consumir todo o buffer
                        return;
                    } catch (Exception e) {
                        // Ignorar mensagens inválidas
                    }
                }
                in.readerIndex(startIndex);
                return;
            }
            
            try {
                FixMessage message = FixBinaryCodec.parse(in);
                if (message != null) {
                    out.add(message);
                }
            } catch (Exception e) {
                in.readerIndex(startIndex);
            }
        }
    }
    
    /**
     * Encoder para mensagens FIX.
     */
    private static class FixMessageEncoder extends MessageToByteEncoder<FixMessage> {
        @Override
        protected void encode(ChannelHandlerContext ctx, FixMessage msg, ByteBuf out) {
            // Tentar encoding binário primeiro
            try {
                ByteBuf encoded = FixBinaryCodec.encode(msg);
                out.writeBytes(encoded);
                encoded.release();
            } catch (Exception e) {
                // Fallback para string encoding
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
}
