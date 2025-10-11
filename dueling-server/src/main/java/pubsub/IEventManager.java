package pubsub;

import java.io.PrintWriter;

/**
 * Interface for Publisher-Subscriber system for event management.
 * This allows switching between in-memory and distributed implementations.
 */
public interface IEventManager {
    /**
     * Subscribes a client's PrintWriter to a specific topic (player ID).
     *
     * @param topic      The topic to subscribe to (e.g., a player's ID).
     * @param subscriber The PrintWriter to notify of new messages.
     */
    void subscribe(String topic, PrintWriter subscriber);

    /**
     * Unsubscribes a client's PrintWriter from a topic.
     *
     * @param topic      The topic to unsubscribe from.
     * @param subscriber The PrintWriter to remove.
     */
    void unsubscribe(String topic, PrintWriter subscriber);

    /**
     * Publishes a message to all subscribers of a specific topic.
     *
     * @param topic   The topic to publish the message to.
     * @param message The message to send.
     */
    void publish(String topic, String message);

    /**
     * Sends a message to a specific group.
     *
     * @param groupName The name of the group to send the message to.
     * @param senderId  The ID of the sender.
     * @param content   The content of the message.
     */
    void sendGroupMessage(String groupName, String senderId, String content);

    /**
     * Sends a message to a specific match.
     *
     * @param matchId  The ID of the match to send the message to.
     * @param senderId The ID of the sender.
     * @param content  The content of the message.
     */
    void sendInGameMessage(String matchId, String senderId, String content);

    /**
     * Sends an emote to a specific channel.
     *
     * @param channelType The type of the channel (e.g., "group", "match").
     * @param channelId   The ID of the channel.
     * @param senderId    The ID of the sender.
     * @param emoteId     The ID of the emote.
     */
    void sendEmote(String channelType, String channelId, String senderId, String emoteId);
}