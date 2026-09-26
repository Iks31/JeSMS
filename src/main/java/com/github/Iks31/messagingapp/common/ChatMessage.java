package com.github.Iks31.messagingapp.common;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

// A single chat message within a conversation.
// Messages are identified within a conversation by their sender and timestamp.
public class ChatMessage implements Serializable {
    @Serial
    private static final long serialVersionUID = 2L;

    public String content;
    public String sender;
    // Milliseconds since the epoch
    public long timestamp;
    public List<String> readBy = new ArrayList<>();
    public boolean edited;
    public boolean isDeleted;

    public ChatMessage() {}

    public ChatMessage(String sender, String content, long timestamp) {
        this.sender = sender;
        this.content = content;
        this.timestamp = timestamp;
    }

    // Deep copy
    public ChatMessage(ChatMessage other) {
        this.content = other.content;
        this.sender = other.sender;
        this.timestamp = other.timestamp;
        this.readBy = new ArrayList<>(other.readBy);
        this.edited = other.edited;
        this.isDeleted = other.isDeleted;
    }

    public Instant getTimestampInstant() {
        return Instant.ofEpochMilli(timestamp);
    }

    // True if both objects refer to the same stored message
    public boolean isSameMessage(ChatMessage other) {
        return other != null && timestamp == other.timestamp && sender != null && sender.equals(other.sender);
    }

    // True if at least one user other than the sender has read the message
    public boolean isReadByOthers() {
        for (String user : readBy) {
            if (!user.equals(sender)) return true;
        }
        return false;
    }

    // Text shown in previews and bubbles
    public String displayContent() {
        return isDeleted ? "This message was deleted" : content;
    }
}
