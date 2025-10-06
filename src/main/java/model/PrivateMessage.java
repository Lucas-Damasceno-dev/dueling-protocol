package model;

import java.time.LocalDateTime;

/**
 * Model representing a private message between users.
 */
public class PrivateMessage {
    private String senderId;
    private String recipientId;
    private String content;
    private LocalDateTime timestamp;
    private boolean read; // Whether the message has been read by the recipient

    public PrivateMessage() {
        this.timestamp = LocalDateTime.now();
        this.read = false;
    }

    public PrivateMessage(String senderId, String recipientId, String content) {
        this();
        this.senderId = senderId;
        this.recipientId = recipientId;
        this.content = content;
    }

    // Getters and setters
    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getRecipientId() {
        return recipientId;
    }

    public void setRecipientId(String recipientId) {
        this.recipientId = recipientId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }
}