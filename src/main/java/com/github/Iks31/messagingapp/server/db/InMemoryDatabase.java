package com.github.Iks31.messagingapp.server.db;

import com.github.Iks31.messagingapp.common.ChatMessage;
import com.github.Iks31.messagingapp.common.Conversation;

import java.util.*;

// Non-persistent storage for local development and tests.
// Returns copies so callers can never mutate the stored state.
public class InMemoryDatabase implements Database {
    private final Map<String, String> users = new HashMap<>();
    private final Map<String, Conversation> conversations = new LinkedHashMap<>();

    @Override
    public synchronized boolean userExists(String username) {
        return users.containsKey(username);
    }

    @Override
    public synchronized String getPasswordHash(String username) {
        return users.get(username);
    }

    @Override
    public synchronized boolean createUser(String username, String passwordHash) {
        return users.putIfAbsent(username, passwordHash) == null;
    }

    @Override
    public synchronized void updatePasswordHash(String username, String passwordHash) {
        users.replace(username, passwordHash);
    }

    @Override
    public synchronized List<Conversation> getConversations(String username) {
        List<Conversation> result = new ArrayList<>();
        for (Conversation conversation : conversations.values()) {
            if (conversation.users.contains(username)) result.add(new Conversation(conversation));
        }
        return result;
    }

    @Override
    public synchronized Conversation getConversation(String conversationId) {
        Conversation conversation = conversations.get(conversationId);
        return conversation == null ? null : new Conversation(conversation);
    }

    @Override
    public synchronized boolean conversationExists(Collection<String> members) {
        Set<String> target = new HashSet<>(members);
        for (Conversation conversation : conversations.values()) {
            if (new HashSet<>(conversation.users).equals(target)) return true;
        }
        return false;
    }

    @Override
    public synchronized Conversation createConversation(String name, List<String> members) {
        Conversation conversation = new Conversation();
        conversation.id = UUID.randomUUID().toString();
        conversation.name = name == null ? "" : name;
        conversation.users = new ArrayList<>(members);
        conversations.put(conversation.id, conversation);
        return new Conversation(conversation);
    }

    @Override
    public synchronized void addMessage(String conversationId, ChatMessage message) {
        Conversation conversation = conversations.get(conversationId);
        if (conversation != null) conversation.messages.add(new ChatMessage(message));
    }

    @Override
    public synchronized boolean editMessage(String conversationId, ChatMessage message, String newContent) {
        ChatMessage stored = find(conversationId, message);
        if (stored == null) return false;
        stored.content = newContent;
        stored.edited = true;
        return true;
    }

    @Override
    public synchronized boolean deleteMessage(String conversationId, ChatMessage message) {
        ChatMessage stored = find(conversationId, message);
        if (stored == null) return false;
        stored.isDeleted = true;
        return true;
    }

    @Override
    public synchronized void markRead(String conversationId, String username) {
        Conversation conversation = conversations.get(conversationId);
        if (conversation != null) conversation.markReadBy(username);
    }

    @Override
    public synchronized void addMember(String conversationId, String username) {
        Conversation conversation = conversations.get(conversationId);
        if (conversation != null && !conversation.users.contains(username)) conversation.users.add(username);
    }

    @Override
    public synchronized void removeMember(String conversationId, String username) {
        Conversation conversation = conversations.get(conversationId);
        if (conversation == null) return;
        conversation.users.remove(username);
        if (conversation.users.isEmpty()) conversations.remove(conversationId);
    }

    private ChatMessage find(String conversationId, ChatMessage message) {
        Conversation conversation = conversations.get(conversationId);
        return conversation == null ? null : conversation.findMessage(message);
    }

    @Override
    public void close() {}
}
