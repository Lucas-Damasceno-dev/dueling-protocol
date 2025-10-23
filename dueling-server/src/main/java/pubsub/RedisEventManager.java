package pubsub;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import controller.dto.chat.GroupMessage;
import controller.dto.chat.InGameMessage;
import model.PrivateMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;

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
     * Sends a message to a specific group.
     *
     * @param groupName The name of the group to send the message to.
     * @param senderId  The ID of the sender.
     * @param content   The content of the message.
     */
    @Override
    public void sendGroupMessage(String groupName, String senderId, String content) {
        try {
            GroupMessage message = new GroupMessage(groupName, senderId, content);
            String messageJson = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(message);

            String channel = "group-messages:" + groupName;
            redisTemplate.convertAndSend(channel, messageJson);

            logger.debug("Group message sent to group {}: {}", groupName, content);
        } catch (Exception e) {
            logger.error("Error sending group message: {}", e.getMessage());
        }
    }

    /**
     * Sends a message to a specific match.
     *
     * @param matchId  The ID of the match to send the message to.
     * @param senderId The ID of the sender.
     * @param content  The content of the message.
     */
    @Override
    public void sendInGameMessage(String matchId, String senderId, String content) {
        try {
            InGameMessage message = new InGameMessage(matchId, senderId, content);
            String messageJson = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(message);

            String channel = "in-game-chat:" + matchId;
            redisTemplate.convertAndSend(channel, messageJson);

            logger.debug("In-game message sent to match {}: {}", matchId, content);
        } catch (Exception e) {
            logger.error("Error sending in-game message: {}", e.getMessage());
        }
    }

    /**
     * Sends an emote to a specific channel.
     *
     * @param channelType The type of the channel (e.g., "group", "match").
     * @param channelId   The ID of the channel.
     * @param senderId    The ID of the sender.
     * @param emoteId     The ID of the emote.
     */
    @Override
    public void sendEmote(String channelType, String channelId, String senderId, String emoteId) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode emoteMessage = mapper.createObjectNode();
            emoteMessage.put("channelType", channelType);
            emoteMessage.put("channelId", channelId);
            emoteMessage.put("senderId", senderId);
            emoteMessage.put("emoteId", emoteId);

            String messageJson = mapper.writeValueAsString(emoteMessage);

            String channel = channelType + "-chat:" + channelId;
            redisTemplate.convertAndSend(channel, messageJson);

            logger.debug("Emote {} sent to channel {}", emoteId, channel);
        } catch (Exception e) {
            logger.error("Error sending emote: {}", e.getMessage());
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
        logger.debug("About to register handler for topic {} with subscriber hash {}", topic, subscriber.hashCode());
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
        logger.debug("Currently have {} subscribers in map", subscribers.size());
        logger.debug("Has subscriber for topic {}: {}", topic, subscribers.containsKey(topic));
        
        // Log trade-related messages for debugging
        if (message != null && message.contains("TRADE")) {
            logger.info("[TRADE-PUBSUB] Publishing trade message to topic {}: {}", topic, message);
            logger.info("[TRADE-PUBSUB] Local subscriber present: {}", subscribers.containsKey(topic));
        }
        
        // Send to local subscriber if present (fast path for same-server)
        PrintWriter localSubscriber = subscribers.get(topic);
        if (localSubscriber != null) {
            try {
                localSubscriber.println(message);
                localSubscriber.flush();
                logger.debug("Message sent directly to local subscriber for topic {}", topic);
                if (message != null && message.contains("TRADE")) {
                    logger.info("[TRADE-PUBSUB] Trade message delivered locally to topic {}", topic);
                }
            } catch (Exception e) {
                logger.warn("Failed to send to local subscriber for topic {}: {}", topic, e.getMessage());
            }
        }
        
        // ALWAYS send via Redis for cross-server communication
        if (message != null && message.contains("TRADE")) {
            logger.info("[TRADE-PUBSUB] Sending trade message via Redis to topic {} (cross-server)", topic);
        }
        redisTemplate.convertAndSend(topic, message);
        if (message != null && message.contains("TRADE")) {
            logger.info("[TRADE-PUBSUB] Trade message sent via Redis successfully");
        }
    }
}
