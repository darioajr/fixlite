package com.fixlite.client;

import com.fixlite.protocol.FixFields;
import com.fixlite.protocol.FixMessage;
import com.fixlite.protocol.FixMessageTypes;
import com.fixlite.protocol.FixParser;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simple FIX client for testing the FixLite Gateway.
 * Demonstrates logon, order submission, and message handling.
 */
public class FixClient {
    
    private static final Logger logger = LoggerFactory.getLogger(FixClient.class);
    
    private final String host;
    private final int port;
    private final String senderCompId;
    private final String targetCompId;
    
    private Channel channel;
    private EventLoopGroup group;
    private final AtomicInteger seqNum = new AtomicInteger(1);
    private volatile boolean loggedOn = false;
    
    public FixClient(String host, int port, String senderCompId, String targetCompId) {
        this.host = host;
        this.port = port;
        this.senderCompId = senderCompId;
        this.targetCompId = targetCompId;
    }
    
    /**
     * Connect to the FIX gateway
     */
    public void connect() throws InterruptedException {
        group = new NioEventLoopGroup();
        
        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ch.pipeline().addLast(
                                    new StringDecoder(StandardCharsets.UTF_8),
                                    new StringEncoder(StandardCharsets.UTF_8),
                                    new FixClientHandler()
                            );
                        }
                    });
            
            ChannelFuture future = bootstrap.connect(host, port).sync();
            channel = future.channel();
            
            logger.info("Connected to FIX gateway at {}:{}", host, port);
            
        } catch (Exception e) {
            logger.error("Failed to connect to gateway", e);
            disconnect();
            throw e;
        }
    }
    
    /**
     * Disconnect from the gateway
     */
    public void disconnect() {
        if (channel != null && channel.isActive()) {
            sendLogout("Client disconnect");
            channel.close();
        }
        
        if (group != null) {
            group.shutdownGracefully();
        }
        
        logger.info("Disconnected from FIX gateway");
    }
    
    /**
     * Send logon message
     */
    public void sendLogon() {
        FixMessage logon = createMessageWithHeader(FixMessageTypes.LOGON);
        logon.setField(FixFields.ENCRYPT_METHOD, "0")
             .setField(FixFields.HEART_BT_INT, "30")
             .setField(FixFields.USERNAME, "admin")
             .setField(FixFields.PASSWORD, "password");
        
        sendMessage(logon);
        logger.info("Logon sent");
    }
    
    /**
     * Send logout message
     */
    public void sendLogout(String reason) {
        if (!loggedOn) return;
        
        FixMessage logout = createMessageWithHeader(FixMessageTypes.LOGOUT);
        if (reason != null) {
            logout.setField(FixFields.TEXT, reason);
        }
        
        sendMessage(logout);
        loggedOn = false;
        logger.info("Logout sent: {}", reason);
    }
    
    /**
     * Send new order single
     */
    public void sendNewOrder(String symbol, String side, String quantity, String price) {
        if (!loggedOn) {
            logger.warn("Cannot send order: not logged on");
            return;
        }
        
        String clOrdId = "ORDER_" + System.currentTimeMillis();
        
        FixMessage order = createMessageWithHeader(FixMessageTypes.NEW_ORDER_SINGLE);
        order.setField(FixFields.CL_ORD_ID, clOrdId)
             .setField(FixFields.SYMBOL, symbol)
             .setField(FixFields.SIDE, side) // 1=Buy, 2=Sell
             .setField(FixFields.ORDER_QTY, quantity)
             .setField(FixFields.ORD_TYPE, "2") // Limit order
             .setField(FixFields.PRICE, price)
             .setField(FixFields.TIME_IN_FORCE, "0"); // Day order
        
        sendMessage(order);
        logger.info("New order sent: {} {} {} @ {}", side.equals("1") ? "BUY" : "SELL", 
                   quantity, symbol, price);
    }
    
    /**
     * Send heartbeat
     */
    public void sendHeartbeat() {
        FixMessage heartbeat = createMessageWithHeader(FixMessageTypes.HEARTBEAT);
        sendMessage(heartbeat);
        logger.debug("Heartbeat sent");
    }
    
    /**
     * Create message with standard header
     */
    private FixMessage createMessageWithHeader(String messageType) {
        FixMessage message = new FixMessage(messageType);
        
        message.setField(FixFields.BEGIN_STRING, "FIX.4.4")
               .setField(FixFields.SENDER_COMP_ID, senderCompId)
               .setField(FixFields.TARGET_COMP_ID, targetCompId)
               .setField(FixFields.MSG_SEQ_NUM, seqNum.getAndIncrement())
               .setField(FixFields.SENDING_TIME, getCurrentTimestamp());
        
        return message;
    }
    
    /**
     * Send message to gateway
     */
    private void sendMessage(FixMessage message) {
        if (channel == null || !channel.isActive()) {
            logger.error("Cannot send message: channel not active");
            return;
        }
        
        String encoded = message.encode();
        channel.writeAndFlush(encoded);
        logger.debug("Sent: {}", encoded);
    }
    
    /**
     * Get current timestamp
     */
    private String getCurrentTimestamp() {
        return java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd-HH:mm:ss.SSS"));
    }
    
    /**
     * Client message handler
     */
    private class FixClientHandler extends ChannelInboundHandlerAdapter {
        
        private StringBuilder messageBuffer = new StringBuilder();
        
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) {
            String data = (String) msg;
            messageBuffer.append(data);
            
            // Process complete messages
            processMessages();
        }
        
        private void processMessages() {
            String bufferContent = messageBuffer.toString();
            
            int lastCompleteMessage = -1;
            int searchStart = 0;
            
            while (true) {
                int checksumPos = bufferContent.indexOf("10=", searchStart);
                if (checksumPos == -1) break;
                
                int messageEnd = bufferContent.indexOf('\u0001', checksumPos);
                if (messageEnd == -1) break;
                
                String messageString = bufferContent.substring(lastCompleteMessage + 1, messageEnd + 1);
                
                try {
                    FixMessage message = FixParser.parse(messageString);
                    handleMessage(message);
                } catch (Exception e) {
                    logger.error("Error parsing message: {}", messageString, e);
                }
                
                lastCompleteMessage = messageEnd;
                searchStart = messageEnd + 1;
            }
            
            if (lastCompleteMessage >= 0) {
                messageBuffer = new StringBuilder(bufferContent.substring(lastCompleteMessage + 1));
            }
        }
        
        private void handleMessage(FixMessage message) {
            String messageType = message.getMessageType();
            logger.info("Received {}: {}", messageType, message);
            
            switch (messageType) {
                case FixMessageTypes.LOGON:
                    loggedOn = true;
                    logger.info("Logon successful!");
                    break;
                case FixMessageTypes.LOGOUT:
                    loggedOn = false;
                    logger.info("Logged out: {}", message.getField(FixFields.TEXT));
                    break;
                case FixMessageTypes.HEARTBEAT:
                    logger.debug("Heartbeat received");
                    break;
                case FixMessageTypes.TEST_REQUEST:
                    // Respond to test request
                    FixMessage heartbeat = createMessageWithHeader(FixMessageTypes.HEARTBEAT);
                    String testReqId = message.getField(FixFields.TEST_REQ_ID);
                    if (testReqId != null) {
                        heartbeat.setField(FixFields.TEST_REQ_ID, testReqId);
                    }
                    sendMessage(heartbeat);
                    break;
                case FixMessageTypes.EXECUTION_REPORT:
                    handleExecutionReport(message);
                    break;
                default:
                    logger.info("Received message type {}", messageType);
                    break;
            }
        }
        
        private void handleExecutionReport(FixMessage execReport) {
            String clOrdId = execReport.getField(FixFields.CL_ORD_ID);
            String execType = execReport.getField(FixFields.EXEC_TYPE);
            String ordStatus = execReport.getField(FixFields.ORD_STATUS);
            String symbol = execReport.getField(FixFields.SYMBOL);
            String lastQty = execReport.getField(FixFields.LAST_QTY);
            String lastPx = execReport.getField(FixFields.LAST_PX);
            
            switch (execType) {
                case "0": // New
                    logger.info("Order acknowledged: {}", clOrdId);
                    break;
                case "F": // Trade
                    logger.info("Order executed: {} {} @ {} for order {}", 
                               lastQty, symbol, lastPx, clOrdId);
                    break;
                case "8": // Rejected
                    String reason = execReport.getField(FixFields.TEXT);
                    logger.warn("Order rejected: {} - {}", clOrdId, reason);
                    break;
                default:
                    logger.info("Execution report: {} status={}", clOrdId, ordStatus);
                    break;
            }
        }
        
        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            logger.error("Client handler exception", cause);
            ctx.close();
        }
    }
    
    /**
     * Main method for testing
     */
    public static void main(String[] args) {
        String host = "localhost";
        int port = 9878;
        String senderCompId = "CLIENT";
        String targetCompId = "SERVER";
        
        FixClient client = new FixClient(host, port, senderCompId, targetCompId);
        
        try {
            client.connect();
            
            // Send logon
            client.sendLogon();
            
            // Wait a bit for logon response
            Thread.sleep(1000);
            
            // Interactive mode
            Scanner scanner = new Scanner(System.in);
            System.out.println("\nFixLite Client Commands:");
            System.out.println("1. order <symbol> <side> <qty> <price> - Send new order (side: 1=Buy, 2=Sell)");
            System.out.println("2. heartbeat - Send heartbeat");
            System.out.println("3. logout - Send logout");
            System.out.println("4. quit - Disconnect and exit");
            System.out.println("\nExample: order AAPL 1 100 150.50");
            
            while (true) {
                System.out.print("\n> ");
                String input = scanner.nextLine().trim();
                
                if (input.isEmpty()) continue;
                
                String[] parts = input.split("\\s+");
                String command = parts[0].toLowerCase();
                
                switch (command) {
                    case "order":
                        if (parts.length >= 5) {
                            client.sendNewOrder(parts[1], parts[2], parts[3], parts[4]);
                        } else {
                            System.out.println("Usage: order <symbol> <side> <qty> <price>");
                        }
                        break;
                    case "heartbeat":
                        client.sendHeartbeat();
                        break;
                    case "logout":
                        client.sendLogout("User requested logout");
                        break;
                    case "quit":
                        client.disconnect();
                        return;
                    default:
                        System.out.println("Unknown command: " + command);
                        break;
                }
            }
            
        } catch (Exception e) {
            logger.error("Client error", e);
        } finally {
            client.disconnect();
        }
    }
}
