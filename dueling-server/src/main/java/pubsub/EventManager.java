package pubsub;

import java.io.PrintWriter;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Manages event publishing and subscriptions for the game.
 * This class implements an in-memory publisher-subscriber system
 * where topics are identified by player IDs.
 */
@Component
@Profile("!distributed") // Use this when NOT in distributed mode
public class EventManager implements IEventManager {
    private static final Logger logger = LoggerFactory.getLogger(EventManager.class);
    private final ConcurrentHashMap<String, CopyOnWriteArrayList<PrintWriter>> subscribers = new ConcurrentHashMap<>();

    /**
     * Subscribes a client's PrintWriter to a specific topic (player ID).
     *
     * @param topic      The topic to subscribe to (e.g., a player's ID).
     * @param subscriber The PrintWriter to notify of new messages.
     */
    public void subscribe(String topic, PrintWriter subscriber) {
        CopyOnWriteArrayList<PrintWriter> topicSubscribers = subscribers.computeIfAbsent(topic, k -> new CopyOnWriteArrayList<>());
        // Remove existing subscriber if present to avoid duplicates
        topicSubscribers.remove(subscriber);
        topicSubscribers.add(subscriber);
        logger.info("New subscriber for topic {}", topic);
    }

    /**
     * Unsubscribes a client's PrintWriter from a topic.
     *
     * @param topic      The topic to unsubscribe from.
     * @param subscriber The PrintWriter to remove.
     */
    public void unsubscribe(String topic, PrintWriter subscriber) {
        if (subscribers.containsKey(topic)) {
            subscribers.get(topic).remove(subscriber);
            logger.info("Subscriber removed from topic {}", topic);
        }
    }

    /**
     * Publishes a message to all subscribers of a specific topic.
     *
     * @param topic   The topic to publish the message to.
     * @param message The message to send.
     */
    public void publish(String topic, String message) {
        if (subscribers.containsKey(topic)) {
            logger.debug("Publishing to topic {}: {}", topic, message);
            for (PrintWriter subscriber : subscribers.get(topic)) {
                subscriber.println(message);
                subscriber.flush();
            }
        }
    }

    @Override
    public void sendGroupMessage(String groupName, String senderId, String content) {
        // Not implemented for in-memory EventManager
        logger.warn("sendGroupMessage not implemented for in-memory EventManager");
    }

    @Override
    public void sendInGameMessage(String matchId, String senderId, String content) {
        // Not implemented for in-memory EventManager
        logger.warn("sendInGameMessage not implemented for in-memory EventManager");
    }

    @Override
    public void sendEmote(String channelType, String channelId, String senderId, String emoteId) {
        logger.info("Emote received: channelType={}, channelId={}, senderId={}, emoteId={}", channelType, channelId, senderId, emoteId);
        // For in-memory, we might publish to a generic channel or directly to players if they are subscribed to a 'channelId' topic
        // For simplicity, just logging for now.
    }
}