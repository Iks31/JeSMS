package com.github.Iks31.messagingapp.server.db;

import com.github.Iks31.messagingapp.common.ChatMessage;
import com.github.Iks31.messagingapp.common.Conversation;

import java.util.Collection;
import java.util.List;

// Storage used by the server. Implementations must be thread-safe.
// Methods throw a RuntimeException on an internal storage error.
public interface Database extends AutoCloseable {

    boolean userExists(String username);

    // Returns the stored password hash, or null if the user does not exist
    String getPasswordHash(String username);

    // Returns false if the user already exists
    boolean createUser(String username, String passwordHash);

    void updatePasswordHash(String username, String passwordHash);

    List<Conversation> getConversations(String username);

    // Returns null if no conversation has the given id
    Conversation getConversation(String conversationId);

    // True if a conversation with exactly these members (in any order) exists
    boolean conversationExists(Collection<String> users);

    // Stores the conversation and returns it with its assigned id
    Conversation createConversation(String name, List<String> users);

    void addMessage(String conversationId, ChatMessage message);

    // Each returns false if the message could not be found
    boolean editMessage(String conversationId, ChatMessage message, String newContent);

    boolean deleteMessage(String conversationId, ChatMessage message);

    // Marks all messages from other users as read by the given user
    void markRead(String conversationId, String username);

    void addMember(String conversationId, String username);

    // Removes the user; the conversation is deleted once it has no members
    void removeMember(String conversationId, String username);

    @Override
    void close();
}
