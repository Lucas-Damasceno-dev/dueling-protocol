package pubsub;

import model.PrivateMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;

import java.io.PrintWriter;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Redis-based Publisher-Subscriber system for distributed event management.
 * This implementation uses Redis Pub/Sub to allow multiple server instances
 * to communicate with any connected WebSocket clients regardless of which
 * server instance they're connected to.
 * 
 * This class implements the same interface as the original in-memory EventManager
 * but uses Redis for distributed messaging across multiple server instances.
 */
@Component
@Profile("distributed") // Use this when in distributed mode
public class RedisEventManager implements IEventManager {

    private static final Logger logger = LoggerFactory.getLogger(RedisEventManager.class);
    
    @Autowired
    private StringRedisTemplate redisTemplate;
    
    @Autowired
    private RedisMessageListenerContainer redisMessageListenerContainer;
    
    @Autowired
    private RedisMessageSubscriber redisMessageSubscriber;
    
    private final ConcurrentHashMap<String, PrintWriter> subscribers = new ConcurrentHashMap<>();
    
    /**
     * Subscribe to private messages for a specific player
     */
    public void subscribeToPrivateMessages(String playerId, RedisMessageSubscriber.PrivateMessageHandler handler) {
        redisMessageSubscriber.registerPrivateMessageHandler(playerId, handler);
    }
    
    /**
     * Unsubscribe from private messages for a specific player
     */
    public void unsubscribeFromPrivateMessages(String playerId) {
        redisMessageSubscriber.unregisterPrivateMessageHandler(playerId);
    }
    
    /**
     * Send a private message to a specific player
     */
    public void sendPrivateMessage(String senderId, String recipientId, String content) {
        try {
            PrivateMessage message = new PrivateMessage(senderId, recipientId, content);
            String messageJson = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(message);
            
            String channel = "private-messages:" + recipientId;
            redisTemplate.convertAndSend(channel, messageJson);
            
            logger.debug("Private message sent from {} to {}: {}", senderId, recipientId, content);
        } catch (Exception e) {
            logger.error("Error sending private message: {}", e.getMessage());
        }
    }

    /**
     * Subscribes a client's PrintWriter to a specific topic (player ID).
     *
     * @param topic      The topic to subscribe to (e.g., a player's ID).
     * @param subscriber The PrintWriter to notify of new messages.
     */
    public void subscribe(String topic, PrintWriter subscriber) {
        subscribers.put(topic, subscriber);
        redisMessageSubscriber.registerHandler(topic, subscriber);
        logger.info("New subscriber for topic {}", topic);
    }

    /**
     * Unsubscribes a client's PrintWriter from a topic.
     *
     * @param topic      The topic to unsubscribe from.
     * @param subscriber The PrintWriter to remove.
     */
    public void unsubscribe(String topic, PrintWriter subscriber) {
        subscribers.remove(topic);
        redisMessageSubscriber.unregisterHandler(topic);
        logger.info("Subscriber removed from topic {}", topic);
    }

    /**
     * Publishes a message to all subscribers of a specific topic via Redis.
     *
     * @param topic   The topic to publish the message to.
     * @param message The message to send.
     */
    public void publish(String topic, String message) {
        logger.debug("Publishing to Redis topic {}: {}", topic, message);
        redisTemplate.convertAndSend(topic, message);
    }
}