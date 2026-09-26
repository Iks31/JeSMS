package com.github.Iks31.messagingapp.common;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

// A conversation between two or more users.
// Direct messages have an empty name; group chats have a name.
public class Conversation implements Serializable {
    @Serial
    private static final long serialVersionUID = 2L;

    // Unique identifier assigned by the server
    public String id;
    public String name = "";
    public ArrayList<String> users = new ArrayList<>();
    public List<ChatMessage> messages = new ArrayList<>();

    public Conversation() {}

    // Deep copy
    public Conversation(Conversation other) {
        this.id = other.id;
        this.name = other.name;
        this.users = new ArrayList<>(other.users);
        this.messages = new ArrayList<>();
        for (ChatMessage message : other.messages) {
            this.messages.add(new ChatMessage(message));
        }
    }

    public boolean isGroup() {
        return name != null && !name.isBlank();
    }

    // Group chat name, or the other members' names for a direct message
    public String displayName(String currentUser) {
        if (isGroup()) {
            return name;
        }
        StringJoiner joiner = new StringJoiner(", ");
        for (String user : users) {
            if (!user.equals(currentUser)) joiner.add(user);
        }
        String others = joiner.toString();
        return others.isEmpty() ? "Just you" : others;
    }

    public ChatMessage lastMessage() {
        return messages.isEmpty() ? null : messages.getLast();
    }

    public ChatMessage findMessage(ChatMessage target) {
        for (ChatMessage message : messages) {
            if (message.isSameMessage(target)) return message;
        }
        return null;
    }

    // Number of messages from other users that the given user has not read
    public int unreadCount(String user) {
        int count = 0;
        for (ChatMessage message : messages) {
            if (!message.sender.equals(user) && !message.isDeleted && !message.readBy.contains(user)) {
                count++;
            }
        }
        return count;
    }

    // Marks every message from other users as read by the given user
    public void markReadBy(String user) {
        for (ChatMessage message : messages) {
            if (!message.sender.equals(user) && !message.readBy.contains(user)) {
                message.readBy.add(user);
            }
        }
    }
}
