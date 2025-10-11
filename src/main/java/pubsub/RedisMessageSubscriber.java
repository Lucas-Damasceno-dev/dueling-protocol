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
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;

import java.io.PrintWriter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Listener component that receives messages from Redis Pub/Sub channels
 * and forwards them to the appropriate WebSocket sessions.
 */
@Component
public class RedisMessageSubscriber implements MessageListener {
    
    private static final Logger logger = LoggerFactory.getLogger(RedisMessageSubscriber.class);
    private static final String PRIVATE_MESSAGE_CHANNEL_PREFIX = "private-messages:";
    
    private final Map<String, PrintWriter> sessionHandlers = new ConcurrentHashMap<>();
    private final Map<String, PrivateMessageHandler> privateMessageHandlers = new ConcurrentHashMap<>();
    
    @Autowired
    private StringRedisTemplate redisTemplate;
    
    @Autowired
    private RedisMessageListenerContainer redisMessageListenerContainer;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Register a WebSocket handler for a specific topic
     */
    public void registerHandler(String topic, PrintWriter handler) {
        sessionHandlers.put(topic, handler);
        
        // Subscribe to the Redis topic
        ChannelTopic channelTopic = new ChannelTopic(topic);
        redisMessageListenerContainer.addMessageListener(this, channelTopic);
        
        logger.info("Registered handler for topic: {}", topic);
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
        
        // Use the topic as the key to get the appropriate PrintWriter handler
        PrintWriter handler = sessionHandlers.get(topic);
        if (handler != null) {
            logger.debug("Received message for topic {}: {}", topic, messageBody);
            handler.println(messageBody);
            handler.flush();
        } else {
            logger.warn("No handler found for topic: {}", topic);
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