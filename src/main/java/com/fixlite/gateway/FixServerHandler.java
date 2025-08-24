package com.fixlite.gateway;

import com.fixlite.protocol.FixFields;
import com.fixlite.protocol.FixMessage;
import com.fixlite.protocol.FixMessageTypes;
import com.fixlite.protocol.FixParser;
import com.fixlite.session.FixSession;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

/**
 * Netty handler for processing FIX protocol messages.
 * Handles session management and message routing.
 */
public class FixServerHandler extends ChannelInboundHandlerAdapter {
    
    private static final Logger logger = LoggerFactory.getLogger(FixServerHandler.class);
    
    private final FixSessionManager sessionManager;
    private final FixMessageProcessor messageProcessor;
    private FixSession session;
    private StringBuilder messageBuffer = new StringBuilder();
    
    public FixServerHandler(FixSessionManager sessionManager, FixMessageProcessor messageProcessor) {
        this.sessionManager = sessionManager;
        this.messageProcessor = messageProcessor;
    }
    
    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        logger.info("New connection established: {}", ctx.channel().remoteAddress());
        ctx.fireChannelActive();
    }
    
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (!(msg instanceof ByteBuf)) {
            return;
        }
        
        ByteBuf buffer = (ByteBuf) msg;
        try {
            String data = buffer.toString(StandardCharsets.UTF_8);
            messageBuffer.append(data);
            
            // Process complete messages
            processMessages(ctx);
            
        } finally {
            buffer.release();
        }
    }
    
    private void processMessages(ChannelHandlerContext ctx) {
        String bufferContent = messageBuffer.toString();
        
        // Find complete messages (ending with checksum field)
        int lastCompleteMessage = -1;
        int searchStart = 0;
        
        while (true) {
            int checksumPos = bufferContent.indexOf("10=", searchStart);
            if (checksumPos == -1) {
                break;
            }
            
            // Find end of checksum field
            int messageEnd = bufferContent.indexOf('\u0001', checksumPos);
            if (messageEnd == -1) {
                break; // Incomplete message
            }
            
            // Process this complete message
            String messageString = bufferContent.substring(
                lastCompleteMessage + 1, messageEnd + 1);
            
            try {
                FixMessage message = FixParser.parse(messageString);
                handleFixMessage(ctx, message);
            } catch (Exception e) {
                logger.error("Error parsing FIX message: {}", messageString, e);
                sendReject(ctx, "Invalid message format");
            }
            
            lastCompleteMessage = messageEnd;
            searchStart = messageEnd + 1;
        }
        
        // Remove processed messages from buffer
        if (lastCompleteMessage >= 0) {
            messageBuffer = new StringBuilder(
                bufferContent.substring(lastCompleteMessage + 1));
        }
    }
    
    private void handleFixMessage(ChannelHandlerContext ctx, FixMessage message) {
        logger.debug("Received FIX message: {}", message);
        
        String messageType = message.getMessageType();
        
        // Handle session-level messages
        switch (messageType) {
            case FixMessageTypes.LOGON:
                handleLogon(ctx, message);
                break;
            case FixMessageTypes.LOGOUT:
                handleLogout(ctx, message);
                break;
            case FixMessageTypes.HEARTBEAT:
                handleHeartbeat(ctx, message);
                break;
            case FixMessageTypes.TEST_REQUEST:
                handleTestRequest(ctx, message);
                break;
            default:
                // Handle application messages
                if (session != null && session.isActive()) {
                    // Validate sequence number
                    Integer seqNum = message.getFieldAsInt(FixFields.MSG_SEQ_NUM);
                    if (seqNum != null && session.validateIncomingSeqNum(seqNum)) {
                        messageProcessor.processMessage(session, message);
                    } else {
                        logger.warn("Invalid sequence number for message: {}", message);
                        sendReject(ctx, "Invalid sequence number");
                    }
                } else {
                    logger.warn("Received application message without active session: {}", message);
                    sendReject(ctx, "Not logged on");
                }
                break;
        }
    }
    
    private void handleLogon(ChannelHandlerContext ctx, FixMessage message) {
        String senderCompId = message.getField(FixFields.SENDER_COMP_ID);
        String targetCompId = message.getField(FixFields.TARGET_COMP_ID);
        
        if (senderCompId == null || targetCompId == null) {
            logger.error("Logon missing required fields");
            sendLogout(ctx, "Missing required fields");
            return;
        }
        
        // Create or get existing session
        session = sessionManager.getOrCreateSession(targetCompId, senderCompId, ctx.channel());
        
        // Validate credentials if provided
        String username = message.getField(FixFields.USERNAME);
        String password = message.getField(FixFields.PASSWORD);
        
        if (!validateCredentials(username, password)) {
            logger.warn("Invalid credentials for session: {}", session.getSessionId());
            sendLogout(ctx, "Invalid credentials");
            return;
        }
        
        // Set heartbeat interval
        Integer heartbeatInterval = message.getFieldAsInt(FixFields.HEART_BT_INT);
        if (heartbeatInterval != null && heartbeatInterval > 0) {
            session.setHeartbeatInterval(heartbeatInterval);
        }
        
        // Send logon response
        FixMessage logonResponse = session.createMessageWithHeader(FixMessageTypes.LOGON);
        logonResponse.setField(FixFields.ENCRYPT_METHOD, "0")
                    .setField(FixFields.HEART_BT_INT, session.getHeartbeatInterval());
        
        session.setState(FixSession.SessionState.LOGGED_ON);
        session.sendMessage(logonResponse);
        
        logger.info("Session logged on: {}", session.getSessionId());
    }
    
    private void handleLogout(ChannelHandlerContext ctx, FixMessage message) {
        if (session != null) {
            String reason = message.getField(FixFields.TEXT);
            logger.info("Logout received for session {}: {}", session.getSessionId(), reason);
            
            // Send logout response
            FixMessage logoutResponse = session.createMessageWithHeader(FixMessageTypes.LOGOUT);
            if (reason != null) {
                logoutResponse.setField(FixFields.TEXT, "Logout acknowledged");
            }
            
            session.sendMessage(logoutResponse);
            session.setState(FixSession.SessionState.DISCONNECTED);
            
            sessionManager.removeSession(session.getSessionId());
        }
        
        ctx.close();
    }
    
    private void handleHeartbeat(ChannelHandlerContext ctx, FixMessage message) {
        if (session != null) {
            logger.debug("Heartbeat received from session: {}", session.getSessionId());
            // No response needed for heartbeat
        }
    }
    
    private void handleTestRequest(ChannelHandlerContext ctx, FixMessage message) {
        if (session != null) {
            String testReqId = message.getField(FixFields.TEST_REQ_ID);
            
            FixMessage heartbeat = session.createMessageWithHeader(FixMessageTypes.HEARTBEAT);
            if (testReqId != null) {
                heartbeat.setField(FixFields.TEST_REQ_ID, testReqId);
            }
            
            session.sendMessage(heartbeat);
            logger.debug("Test request handled for session: {}", session.getSessionId());
        }
    }
    
    private void sendReject(ChannelHandlerContext ctx, String reason) {
        if (session != null) {
            FixMessage reject = session.createMessageWithHeader(FixMessageTypes.REJECT);
            reject.setField(FixFields.TEXT, reason);
            session.sendMessage(reject);
        } else {
            // Send reject without session context
            ctx.writeAndFlush("8=FIX.4.4\u00019=55\u000135=3\u000149=SERVER\u000156=CLIENT\u000134=1\u000152=" + 
                             java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd-HH:mm:ss.SSS")) + 
                             "\u000158=" + reason + "\u000110=000\u0001");
        }
    }
    
    private void sendLogout(ChannelHandlerContext ctx, String reason) {
        if (session != null) {
            session.sendLogout(reason);
        } else {
            // Send logout without session context
            ctx.writeAndFlush("8=FIX.4.4\u00019=55\u000135=5\u000149=SERVER\u000156=CLIENT\u000134=1\u000152=" + 
                             java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd-HH:mm:ss.SSS")) + 
                             "\u000158=" + reason + "\u000110=000\u0001");
        }
        ctx.close();
    }
    
    private boolean validateCredentials(String username, String password) {
        // Simple validation - in production, use proper authentication
        return username == null || username.isEmpty() || "admin".equals(username);
    }
    
    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        logger.info("Connection closed: {}", ctx.channel().remoteAddress());
        
        if (session != null) {
            session.setState(FixSession.SessionState.DISCONNECTED);
            sessionManager.removeSession(session.getSessionId());
        }
        
        ctx.fireChannelInactive();
    }
    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.error("Exception in FIX handler", cause);
        ctx.close();
    }
}
