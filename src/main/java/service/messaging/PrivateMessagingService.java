package service.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import model.PrivateMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for handling private messaging between users using Redis Pub/Sub.
 */
@Service
public class PrivateMessagingService {

    private static final Logger logger = LoggerFactory.getLogger(PrivateMessagingService.class);
    private static final String PRIVATE_MESSAGE_CHANNEL_PREFIX = "private-messages:";

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private RedisMessageListenerContainer redisMessageListenerContainer;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, PrivateMessageHandler> messageHandlers = new ConcurrentHashMap<>();

    /**
     * Send a private message to a specific user
     */
    public void sendPrivateMessage(String senderId, String recipientId, String content) {
        try {
            PrivateMessage message = new PrivateMessage(senderId, recipientId, content);
            String messageJson = objectMapper.writeValueAsString(message);

            // Publish the message to the recipient's private message channel
            String channel = PRIVATE_MESSAGE_CHANNEL_PREFIX + recipientId;
            redisTemplate.convertAndSend(channel, messageJson);

            logger.debug("Private message sent from {} to {}: {}", senderId, recipientId, content);
        } catch (JsonProcessingException e) {
            logger.error("Error serializing private message: {}", e.getMessage());
        }
    }

    /**
     * Subscribe to private messages for a specific user
     */
    public void subscribeToPrivateMessages(String playerId, PrivateMessageHandler handler) {
        String channel = PRIVATE_MESSAGE_CHANNEL_PREFIX + playerId;
        
        // Store the handler for this player
        messageHandlers.put(playerId, handler);
        
        // Add a listener to the Redis channel for this player
        ChannelTopic topic = new ChannelTopic(channel);
        redisMessageListenerContainer.addMessageListener((message, pattern) -> {
            String messageContent = new String(message.getBody());
            try {
                PrivateMessage privateMessage = objectMapper.readValue(messageContent, PrivateMessage.class);
                
                // Get the appropriate handler and process the message
                PrivateMessageHandler playerHandler = messageHandlers.get(playerId);
                if (playerHandler != null) {
                    playerHandler.handleMessage(privateMessage);
                }
            } catch (Exception e) {
                logger.error("Error deserializing private message: {}", e.getMessage());
            }
        }, topic);

        logger.info("Subscribed user {} to private messages", playerId);
    }

    /**
     * Unsubscribe from private messages for a specific user
     */
    public void unsubscribeFromPrivateMessages(String playerId) {
        messageHandlers.remove(playerId);
        logger.info("Unsubscribed user {} from private messages", playerId);
    }

    /**
     * Interface for handling incoming private messages
     */
    @FunctionalInterface
    public interface PrivateMessageHandler {
        void handleMessage(PrivateMessage message);
    }
}