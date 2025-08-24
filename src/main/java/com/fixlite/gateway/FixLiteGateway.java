package com.fixlite.gateway;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

/**
 * Main entry point for the FixLite Gateway.
 * High-performance FIX protocol server built with Netty and Java 21.
 */
public class FixLiteGateway {
    
    private static final Logger logger = LoggerFactory.getLogger(FixLiteGateway.class);
    
    private final int port;
    private final FixSessionManager sessionManager;
    private final FixMessageProcessor messageProcessor;
    
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private ChannelFuture serverFuture;
    
    public FixLiteGateway(int port) {
        this.port = port;
        this.sessionManager = new FixSessionManager();
        this.messageProcessor = new FixMessageProcessor();
    }
    
    /**
     * Start the FIX gateway server
     */
    public void start() throws InterruptedException {
        logger.info("Starting FixLite Gateway on port {}", port);
        
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();
        
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .option(ChannelOption.SO_REUSEADDR, true)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .childOption(ChannelOption.TCP_NODELAY, true)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ch.pipeline().addLast(
                                    new StringDecoder(StandardCharsets.UTF_8),
                                    new StringEncoder(StandardCharsets.UTF_8),
                                    new FixServerHandler(sessionManager, messageProcessor)
                            );
                        }
                    });
            
            // Bind and start to accept incoming connections
            serverFuture = bootstrap.bind(port).sync();
            logger.info("FixLite Gateway started successfully on port {}", port);
            
            // Add shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
            
            // Wait until the server socket is closed
            serverFuture.channel().closeFuture().sync();
            
        } finally {
            shutdown();
        }
    }
    
    /**
     * Stop the FIX gateway server
     */
    public void shutdown() {
        logger.info("Shutting down FixLite Gateway...");
        
        if (sessionManager != null) {
            sessionManager.shutdown();
        }
        
        if (serverFuture != null) {
            serverFuture.channel().close();
        }
        
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
        
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        
        logger.info("FixLite Gateway shutdown complete");
    }
    
    /**
     * Get session manager
     */
    public FixSessionManager getSessionManager() {
        return sessionManager;
    }
    
    /**
     * Get message processor
     */
    public FixMessageProcessor getMessageProcessor() {
        return messageProcessor;
    }
    
    /**
     * Main method
     */
    public static void main(String[] args) {
        int port = 9878; // Default FIX port
        
        // Parse command line arguments
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                logger.error("Invalid port number: {}", args[0]);
                System.exit(1);
            }
        }
        
        FixLiteGateway gateway = new FixLiteGateway(port);
        
        try {
            gateway.start();
        } catch (InterruptedException e) {
            logger.error("Gateway interrupted", e);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            logger.error("Failed to start gateway", e);
            System.exit(1);
        }
    }
}
