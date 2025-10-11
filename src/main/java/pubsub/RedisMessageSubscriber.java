package pubsub;

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
    
    private final Map<String, PrintWriter> sessionHandlers = new ConcurrentHashMap<>();
    
    @Autowired
    private StringRedisTemplate redisTemplate;
    
    @Autowired
    private RedisMessageListenerContainer redisMessageListenerContainer;
    
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
     * Remove a WebSocket handler for a specific topic
     */
    public void unregisterHandler(String topic) {
        sessionHandlers.remove(topic);
        logger.info("Unregistered handler for topic: {}", topic);
    }
    
    /**
     * Handle incoming Redis messages
     */
    @Override
    public void onMessage(Message message, byte[] pattern) {
        String topic = new String(message.getChannel());
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
}