package pubsub;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import model.PrivateMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.PrintWriter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

/**
 * Listener component that receives messages from Redis Pub/Sub channels
 * and forwards them to the appropriate WebSocket sessions.
 */
@Component
public class RedisMessageSubscriber implements MessageListener {
    
    private static final Logger logger = LoggerFactory.getLogger(RedisMessageSubscriber.class);
    private static final String PRIVATE_MESSAGE_CHANNEL_PREFIX = "private-messages:";
    
    // Dedicated thread pool for Redis subscriptions to avoid blocking
    private static final ExecutorService subscriptionExecutor = Executors.newFixedThreadPool(10);
    
    private final Map<String, PrintWriter> sessionHandlers = new ConcurrentHashMap<>();
    private final Map<String, PrivateMessageHandler> privateMessageHandlers = new ConcurrentHashMap<>();
    
    @Autowired
    private StringRedisTemplate redisTemplate;
    
    @Autowired
    private RedisMessageListenerContainer redisMessageListenerContainer;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Initialize global pattern subscription for all player messages
     * This replaces per-player topic subscriptions to avoid RedisMessageListenerContainer deadlock
     */
    @PostConstruct
    public void init() {
        logger.info("Initializing RedisMessageSubscriber with wildcard pattern for all player topics");
        try {
            // Subscribe to all numeric topics (player IDs are numbers)
            // Pattern "[0-9]*" matches any sequence of digits
            PatternTopic pattern = new PatternTopic("[0-9]*");
            redisMessageListenerContainer.addMessageListener(this, pattern);
            logger.info("Successfully subscribed to pattern '[0-9]*' for all player messages");
        } catch (Exception e) {
            logger.error("Failed to subscribe to wildcard pattern: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Register a WebSocket handler for a specific topic
     */
    public void registerHandler(String topic, PrintWriter handler) {
        logger.info("Registering handler for topic: {} (hash: {})", topic, handler.hashCode());
        sessionHandlers.put(topic, handler);
        
        // NO LONGER subscribe to individual topics - we use a wildcard pattern instead
        // This prevents RedisMessageListenerContainer deadlock/hang when adding many listeners
        logger.info("Handler registered in memory for topic: {} (Redis subscription handled by wildcard pattern)", topic);
    }
    
    /**
     * Register a private message handler for a specific player
     */
    public void registerPrivateMessageHandler(String playerId, PrivateMessageHandler handler) {
        String channel = PRIVATE_MESSAGE_CHANNEL_PREFIX + playerId;
        privateMessageHandlers.put(playerId, handler);
        
        // Subscribe to the private message channel
        ChannelTopic channelTopic = new ChannelTopic(channel);
        redisMessageListenerContainer.addMessageListener((message, pattern) -> {
            String messageBody = new String(message.getBody());
            try {
                PrivateMessage privateMessage = objectMapper.readValue(messageBody, PrivateMessage.class);
                PrivateMessageHandler privateHandler = privateMessageHandlers.get(playerId);
                if (privateHandler != null) {
                    privateHandler.handleMessage(privateMessage);
                }
            } catch (JsonProcessingException e) {
                logger.error("Error deserializing private message: {}", e.getMessage());
            } catch (Exception e) {
                logger.error("Error processing private message: {}", e.getMessage());
            }
        }, channelTopic);
        
        logger.info("Registered private message handler for player: {}", playerId);
    }
    
    /**
     * Remove a WebSocket handler for a specific topic
     */
    public void unregisterHandler(String topic) {
        sessionHandlers.remove(topic);
        logger.info("Unregistered handler for topic: {}", topic);
    }
    
    /**
     * Remove a private message handler for a specific player
     */
    public void unregisterPrivateMessageHandler(String playerId) {
        privateMessageHandlers.remove(playerId);
        logger.info("Unregistered private message handler for player: {}", playerId);
    }
    
    /**
     * Handle incoming Redis messages for regular topics
     */
    @Override
    public void onMessage(Message message, byte[] pattern) {
        String topic = new String(message.getChannel());
        
        // Check if this is a private message channel
        if (topic.startsWith(PRIVATE_MESSAGE_CHANNEL_PREFIX)) {
            // This should be handled by the private message listener
            return;
        }
        
        String messageBody = new String(message.getBody());
        
        // Log trade-related messages for debugging
        if (messageBody != null && messageBody.contains("TRADE")) {
            logger.info("[TRADE-PUBSUB] Received trade message from Redis for topic {}: {}", topic, messageBody);
        }
        
        // Use the topic as the key to get the appropriate PrintWriter handler
        PrintWriter handler = sessionHandlers.get(topic);
        if (handler != null) {
            logger.debug("Received message for topic {}: {}", topic, messageBody);
            handler.println(messageBody);
            handler.flush();
            if (messageBody != null && messageBody.contains("TRADE")) {
                logger.info("[TRADE-PUBSUB] Trade message delivered to handler for topic {}", topic);
            }
        } else {
            logger.warn("No handler found for topic: {}", topic);
            if (messageBody != null && messageBody.contains("TRADE")) {
                logger.warn("[TRADE-PUBSUB] No handler for trade message on topic: {}", topic);
            }
        }
    }
    
    /**
     * Interface for handling incoming private messages
     */
    @FunctionalInterface
    public interface PrivateMessageHandler {
        void handleMessage(PrivateMessage message);
    }
}