package pubsub;

import java.io.PrintWriter;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages event publishing and subscriptions for the game.
 * This class implements an in-memory publisher-subscriber system
 * where topics are identified by player IDs.
 */
public class EventManager {
    private static final Logger logger = LoggerFactory.getLogger(EventManager.class);
    private final ConcurrentHashMap<String, CopyOnWriteArrayList<PrintWriter>> subscribers = new ConcurrentHashMap<>();

    /**
     * Subscribes a client's PrintWriter to a specific topic (player ID).
     *
     * @param topic      The topic to subscribe to (e.g., a player's ID).
     * @param subscriber The PrintWriter to notify of new messages.
     */
    public void subscribe(String topic, PrintWriter subscriber) {
        subscribers.computeIfAbsent(topic, k -> new CopyOnWriteArrayList<>()).add(subscriber);
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
            }
        }
    }
}