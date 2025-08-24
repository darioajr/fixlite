package com.fixlite.integration;

import com.fixlite.protocol.*;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.MessageToByteEncoder;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Servidor de teste simulando o FixLite Gateway para testes de integração.
 * Implementa comportamento básico de um gateway FIX para validar compatibilidade.
 */
public class FixGatewayTestServer {
    
    private final int port;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;
    
    private final ConcurrentHashMap<String, FixSession> sessions = new ConcurrentHashMap<>();
    private final AtomicInteger msgSeqNum = new AtomicInteger(1);
    
    public FixGatewayTestServer(int port) {
        this.port = port;
    }
    
    /**
     * Inicia o servidor de teste.
     */
    public void start() throws InterruptedException {
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();
        
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ch.pipeline()
                              .addLast(new FixMessageDecoder())
                              .addLast(new FixMessageEncoder())
                              .addLast(new FixGatewayHandler());
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);
            
            ChannelFuture future = bootstrap.bind(port).sync();
            serverChannel = future.channel();
            System.out.println("FixLite Test Gateway iniciado na porta " + port);
            
        } catch (Exception e) {
            stop();
            throw e;
        }
    }
    
    /**
     * Para o servidor.
     */
    public void stop() {
        if (serverChannel != null) {
            serverChannel.close();
        }
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
    }
    
    /**
     * Sessão FIX para cada cliente conectado.
     */
    private static class FixSession {
        final String senderCompId;
        final String targetCompId;
        final FixVersion version;
        final AtomicInteger msgSeqNum = new AtomicInteger(1);
        boolean loggedOn = false;
        
        FixSession(String senderCompId, String targetCompId, FixVersion version) {
            this.senderCompId = senderCompId;
            this.targetCompId = targetCompId;
            this.version = version;
        }
    }
    
    /**
     * Handler principal do gateway de teste.
     */
    private class FixGatewayHandler extends SimpleChannelInboundHandler<FixMessage> {
        private FixSession session;
        
        @Override
        protected void channelRead0(ChannelHandlerContext ctx, FixMessage msg) {
            String msgType = msg.getField(FixFields.MSG_TYPE);
            
            try {
                switch (msgType) {
                    case FixMessageTypes.LOGON:
                        handleLogon(ctx, msg);
                        break;
                    case FixMessageTypes.HEARTBEAT:
                        handleHeartbeat(ctx, msg);
                        break;
                    case FixMessageTypes.TEST_REQUEST:
                        handleTestRequest(ctx, msg);
                        break;
                    case FixMessageTypes.NEW_ORDER_SINGLE:
                        handleNewOrderSingle(ctx, msg);
                        break;
                    case FixMessageTypes.LOGOUT:
                        handleLogout(ctx, msg);
                        break;
                    default:
                        // Handle post-trading messages
                        handlePostTradingMessage(ctx, msg);
                }
            } catch (Exception e) {
                System.err.println("Erro processando mensagem: " + e.getMessage());
                sendReject(ctx, msg, "Processing error");
            }
        }
        
        private void handleLogon(ChannelHandlerContext ctx, FixMessage logon) {
            String senderCompId = logon.getField(FixFields.SENDER_COMP_ID);
            String targetCompId = logon.getField(FixFields.TARGET_COMP_ID);
            String beginString = logon.getField(FixFields.BEGIN_STRING);
            
            FixVersion version = FixVersion.fromBeginString(beginString);
            if (version == null) {
                version = FixVersion.FIX_4_4; // Default
            }
            
            // Criar sessão
            session = new FixSession(targetCompId, senderCompId, version); // Inverter para resposta
            session.loggedOn = true;
            sessions.put(senderCompId, session);
            
            // Responder com Logon
            FixMessageFactory factory = new FixMessageFactory(version, session.senderCompId, session.targetCompId);
            FixMessage logonResponse = factory.createLogon(30, null, null);
            logonResponse.setField(FixFields.MSG_SEQ_NUM, String.valueOf(session.msgSeqNum.getAndIncrement()));
            logonResponse.setField(FixFields.ENCRYPT_METHOD, "0");
            logonResponse.setField(FixFields.HEART_BT_INT, "30");
            logonResponse.setField(FixFields.SENDING_TIME, getCurrentTimestamp());
            
            ctx.writeAndFlush(logonResponse);
            System.out.println("Logon aceito para " + senderCompId);
        }
        
        private void handleHeartbeat(ChannelHandlerContext ctx, FixMessage heartbeat) {
            // Simplesmente aceitar heartbeat - não precisa responder
            System.out.println("Heartbeat recebido de " + heartbeat.getField(FixFields.SENDER_COMP_ID));
        }
        
        private void handleTestRequest(ChannelHandlerContext ctx, FixMessage testRequest) {
            if (session == null || !session.loggedOn) return;
            
            String testReqId = testRequest.getField(FixFields.TEST_REQ_ID);
            
            // Responder com Heartbeat
            FixMessageFactory factory = new FixMessageFactory(session.version, session.senderCompId, session.targetCompId);
            FixMessage heartbeat = factory.createHeartbeat(null);
            heartbeat.setField(FixFields.MSG_SEQ_NUM, String.valueOf(session.msgSeqNum.getAndIncrement()));
            heartbeat.setField(FixFields.TEST_REQ_ID, testReqId);
            heartbeat.setField(FixFields.SENDING_TIME, getCurrentTimestamp());
            
            ctx.writeAndFlush(heartbeat);
        }
        
        private void handleNewOrderSingle(ChannelHandlerContext ctx, FixMessage order) {
            if (session == null || !session.loggedOn) return;
            
            String clOrdId = order.getField(FixFields.CL_ORD_ID);
            String symbol = order.getField(FixFields.SYMBOL);
            String side = order.getField(FixFields.SIDE);
            String orderQty = order.getField(FixFields.ORDER_QTY);
            String price = order.getField(FixFields.PRICE);
            
            // Simular ExecutionReport (ordem aceita)
            FixMessage execReport = new FixMessage(FixMessageTypes.EXECUTION_REPORT);
            execReport.setVersion(session.version);
            execReport.setField(FixFields.SENDER_COMP_ID, session.senderCompId);
            execReport.setField(FixFields.TARGET_COMP_ID, session.targetCompId);
            execReport.setField(FixFields.MSG_SEQ_NUM, String.valueOf(session.msgSeqNum.getAndIncrement()));
            execReport.setField(FixFields.CL_ORD_ID, clOrdId);
            execReport.setField(FixFields.ORDER_ID, "EXEC" + System.currentTimeMillis());
            execReport.setField(FixFields.EXEC_ID, "E" + System.currentTimeMillis());
            execReport.setField(FixFields.EXEC_TYPE, "0"); // New
            execReport.setField(FixFields.ORD_STATUS, "0"); // New
            execReport.setField(FixFields.SYMBOL, symbol);
            execReport.setField(FixFields.SIDE, side);
            execReport.setField(FixFields.ORDER_QTY, orderQty);
            execReport.setField(FixFields.LAST_QTY, "0");
            execReport.setField(FixFields.LAST_PX, "0");
            execReport.setField(FixFields.LEAVES_QTY, orderQty);
            execReport.setField(FixFields.CUM_QTY, "0");
            execReport.setField(FixFields.AVG_PX, "0");
            if (price != null) {
                execReport.setField(FixFields.PRICE, price);
            }
            execReport.setField(FixFields.SENDING_TIME, getCurrentTimestamp());
            
            ctx.writeAndFlush(execReport);
            
            // Simular Fill após pequeno delay
            ctx.executor().schedule(() -> {
                FixMessage fillReport = new FixMessage(FixMessageTypes.EXECUTION_REPORT);
                fillReport.setVersion(session.version);
                fillReport.setField(FixFields.SENDER_COMP_ID, session.senderCompId);
                fillReport.setField(FixFields.TARGET_COMP_ID, session.targetCompId);
                fillReport.setField(FixFields.MSG_SEQ_NUM, String.valueOf(session.msgSeqNum.getAndIncrement()));
                fillReport.setField(FixFields.CL_ORD_ID, clOrdId);
                fillReport.setField(FixFields.ORDER_ID, "EXEC" + System.currentTimeMillis());
                fillReport.setField(FixFields.EXEC_ID, "F" + System.currentTimeMillis());
                fillReport.setField(FixFields.EXEC_TYPE, "F"); // Fill
                fillReport.setField(FixFields.ORD_STATUS, "2"); // Filled
                fillReport.setField(FixFields.SYMBOL, symbol);
                fillReport.setField(FixFields.SIDE, side);
                fillReport.setField(FixFields.ORDER_QTY, orderQty);
                fillReport.setField(FixFields.LAST_QTY, orderQty);
                fillReport.setField(FixFields.LAST_PX, price != null ? price : "100.0");
                fillReport.setField(FixFields.LEAVES_QTY, "0");
                fillReport.setField(FixFields.CUM_QTY, orderQty);
                fillReport.setField(FixFields.AVG_PX, price != null ? price : "100.0");
                if (price != null) {
                    fillReport.setField(FixFields.PRICE, price);
                }
                fillReport.setField(FixFields.SENDING_TIME, getCurrentTimestamp());
                
                ctx.writeAndFlush(fillReport);
            }, 100, java.util.concurrent.TimeUnit.MILLISECONDS);
        }
        
        private void handleLogout(ChannelHandlerContext ctx, FixMessage logout) {
            if (session != null) {
                session.loggedOn = false;
                sessions.remove(session.targetCompId);
                
                // Responder com Logout
                FixMessage logoutResponse = new FixMessage(FixMessageTypes.LOGOUT);
                logoutResponse.setVersion(session.version);
                logoutResponse.setField(FixFields.SENDER_COMP_ID, session.senderCompId);
                logoutResponse.setField(FixFields.TARGET_COMP_ID, session.targetCompId);
                logoutResponse.setField(FixFields.MSG_SEQ_NUM, String.valueOf(session.msgSeqNum.getAndIncrement()));
                logoutResponse.setField(FixFields.SENDING_TIME, getCurrentTimestamp());
                
                ctx.writeAndFlush(logoutResponse);
            }
            ctx.close();
        }
        
        private void handlePostTradingMessage(ChannelHandlerContext ctx, FixMessage msg) {
            if (session == null || !session.loggedOn) return;
            
            String msgType = msg.getField(FixFields.MSG_TYPE);
            
            switch (msgType) {
                case PostTradingMessageTypes.CONFIRMATION:
                    // Responder com Confirmation Ack
                    String confirmId = msg.getField(PostTradingFields.CONFIRM_ID);
                    FixMessage confirmAck = PostTradingMessageFactory.createConfirmationAck(
                        session.version, session.senderCompId, session.targetCompId, confirmId, "1" // Confirmed
                    );
                    confirmAck.setField(FixFields.MSG_SEQ_NUM, String.valueOf(session.msgSeqNum.getAndIncrement()));
                    confirmAck.setField(FixFields.SENDING_TIME, getCurrentTimestamp());
                    ctx.writeAndFlush(confirmAck);
                    break;
                    
                case PostTradingMessageTypes.ALLOCATION_INSTRUCTION:
                    // Responder com Allocation Report
                    String allocId = msg.getField(PostTradingFields.ALLOC_ID);
                    FixMessage allocReport = PostTradingMessageFactory.createAllocationReport(
                        session.version, session.senderCompId, session.targetCompId, "RPT_" + allocId, allocId, "0" // Accepted
                    );
                    allocReport.setField(FixFields.MSG_SEQ_NUM, String.valueOf(session.msgSeqNum.getAndIncrement()));
                    allocReport.setField(FixFields.SENDING_TIME, getCurrentTimestamp());
                    ctx.writeAndFlush(allocReport);
                    break;
                    
                case PostTradingMessageTypes.REQUEST_FOR_POSITIONS:
                    // Responder com Position Report
                    String posReqId = msg.getField(PostTradingFields.POS_REQ_ID);
                    String symbol = msg.getField(FixFields.SYMBOL);
                    String account = msg.getField(FixFields.ACCOUNT);
                    FixMessage posReport = PostTradingMessageFactory.createPositionReport(
                        session.version, session.senderCompId, session.targetCompId, posReqId, symbol, account, "1000", "0"
                    );
                    posReport.setField(FixFields.MSG_SEQ_NUM, String.valueOf(session.msgSeqNum.getAndIncrement()));
                    posReport.setField(FixFields.SENDING_TIME, getCurrentTimestamp());
                    ctx.writeAndFlush(posReport);
                    break;
                    
                case PostTradingMessageTypes.TRADE_CAPTURE_REPORT:
                    // Responder com Trade Capture Report Ack
                    String tradeReportId = msg.getField(PostTradingFields.TRADE_REPORT_ID);
                    FixMessage tradeCaptureAck = PostTradingMessageFactory.createTradeCaptureReportAck(
                        session.version, session.senderCompId, session.targetCompId, tradeReportId, "0" // Accepted
                    );
                    tradeCaptureAck.setField(FixFields.MSG_SEQ_NUM, String.valueOf(session.msgSeqNum.getAndIncrement()));
                    tradeCaptureAck.setField(FixFields.SENDING_TIME, getCurrentTimestamp());
                    ctx.writeAndFlush(tradeCaptureAck);
                    break;
                    
                case PostTradingMessageTypes.COLLATERAL_ASSIGNMENT:
                    // Responder com Collateral Response
                    String collateralId = msg.getField(PostTradingFields.COLLATERAL_ASSIGNMENT_ID);
                    FixMessage collateralResponse = PostTradingMessageFactory.createCollateralResponse(
                        session.version, session.senderCompId, session.targetCompId, collateralId, "0" // Accepted
                    );
                    collateralResponse.setField(FixFields.MSG_SEQ_NUM, String.valueOf(session.msgSeqNum.getAndIncrement()));
                    collateralResponse.setField(FixFields.SENDING_TIME, getCurrentTimestamp());
                    ctx.writeAndFlush(collateralResponse);
                    break;
                    
                case PostTradingMessageTypes.MARGIN_REQUIREMENT_REPORT:
                    // Aceitar margin requirement report (sem resposta específica necessária)
                    System.out.println("Margin requirement report aceito: " + msg.getField(PostTradingFields.MARGIN_REQ_ID));
                    break;
                    
                case PostTradingMessageTypes.SETTLEMENT_INSTRUCTIONS:
                    // Responder com Settlement Obligation Report
                    String settlInstId = msg.getField(PostTradingFields.SETTL_INST_ID);
                    FixMessage settlObligationReport = PostTradingMessageFactory.createSettlementObligationReport(
                        session.version, session.senderCompId, session.targetCompId, settlInstId, "0" // Accepted
                    );
                    settlObligationReport.setField(FixFields.MSG_SEQ_NUM, String.valueOf(session.msgSeqNum.getAndIncrement()));
                    settlObligationReport.setField(FixFields.SENDING_TIME, getCurrentTimestamp());
                    ctx.writeAndFlush(settlObligationReport);
                    break;
                    
                default:
                    // Responder com Reject para mensagens não suportadas
                    sendReject(ctx, msg, "Unsupported message type: " + msgType);
            }
        }
        
        private void sendReject(ChannelHandlerContext ctx, FixMessage originalMessage, String reason) {
            if (session == null) return;
            
            FixMessage reject = new FixMessage(FixMessageTypes.REJECT);
            reject.setVersion(session.version);
            reject.setField(FixFields.SENDER_COMP_ID, session.senderCompId);
            reject.setField(FixFields.TARGET_COMP_ID, session.targetCompId);
            reject.setField(FixFields.MSG_SEQ_NUM, String.valueOf(session.msgSeqNum.getAndIncrement()));
            reject.setField(FixFields.REF_SEQ_NUM, originalMessage.getField(FixFields.MSG_SEQ_NUM));
            reject.setField(FixFields.TEXT, reason);
            reject.setField(FixFields.SENDING_TIME, getCurrentTimestamp());
            
            ctx.writeAndFlush(reject);
        }
        
        @Override
        public void channelInactive(ChannelHandlerContext ctx) {
            if (session != null) {
                sessions.remove(session.targetCompId);
                System.out.println("Cliente desconectado: " + session.targetCompId);
            }
        }
        
        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            cause.printStackTrace();
            ctx.close();
        }
    }
    
    /**
     * Decoder para mensagens FIX (binário e texto).
     */
    private static class FixMessageDecoder extends ByteToMessageDecoder {
        @Override
        protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
            if (in.readableBytes() < 20) return;
            
            int startIndex = in.readerIndex();
            
            try {
                // Tentar parsing binário primeiro
                if (FixBinaryCodec.isValidFixMessage(in)) {
                    FixMessage message = FixBinaryCodec.parse(in);
                    if (message != null) {
                        out.add(message);
                        return;
                    }
                }
                
                // Fallback para parsing de texto
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
                System.err.println("Erro decodificando mensagem: " + e.getMessage());
            }
        }
    }
    
    /**
     * Encoder para mensagens FIX.
     */
    private static class FixMessageEncoder extends MessageToByteEncoder<FixMessage> {
        @Override
        protected void encode(ChannelHandlerContext ctx, FixMessage msg, ByteBuf out) {
            try {
                // Usar encoding binário por padrão
                ByteBuf encoded = FixBinaryCodec.encode(msg);
                out.writeBytes(encoded);
                encoded.release();
            } catch (Exception e) {
                // Fallback para string
                String fixString = msg.encode();
                out.writeBytes(fixString.getBytes(StandardCharsets.US_ASCII));
            }
        }
    }
    
    private String getCurrentTimestamp() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HH:mm:ss.SSS"));
    }
    
    public int getActiveSessionCount() {
        return sessions.size();
    }
}
