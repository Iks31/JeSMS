package com.github.Iks31.messagingapp.server.db;

import com.github.Iks31.messagingapp.common.ChatMessage;
import com.github.Iks31.messagingapp.common.Conversation;
import com.mongodb.ErrorCategory;
import com.mongodb.MongoException;
import com.mongodb.MongoWriteException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.UpdateResult;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import java.util.*;

import static com.mongodb.client.model.Filters.eq;

// MongoDB backed storage.
// Collections:
//   users:         { user, password }
//   conversations: { name, users: [..], messages: [{ content, sender, timestamp, readBy, edited, isDeleted }] }
public class MongoDatabase implements Database {
    private final MongoClient mongoClient;
    private final MongoCollection<Document> users;
    private final MongoCollection<Document> conversations;

    public MongoDatabase(String uri, String databaseName) {
        mongoClient = MongoClients.create(uri);
        com.mongodb.client.MongoDatabase db = mongoClient.getDatabase(databaseName);
        // Fails fast if the database cannot be reached
        db.runCommand(new Document("ping", 1));
        users = db.getCollection("users");
        conversations = db.getCollection("conversations");
        try {
            // Guarantees usernames are unique even if two registrations race
            users.createIndex(Indexes.ascending("user"), new IndexOptions().unique(true));
        } catch (MongoException e) {
            System.err.println("[DATABASE] Could not create unique username index (duplicate usernames exist?): " + e.getMessage());
        }
    }

    @Override
    public boolean userExists(String username) {
        return users.find(eq("user", username)).first() != null;
    }

    @Override
    public String getPasswordHash(String username) {
        Document user = users.find(eq("user", username)).first();
        return user == null ? null : user.getString("password");
    }

    @Override
    public synchronized boolean createUser(String username, String passwordHash) {
        if (userExists(username)) {
            return false;
        }
        try {
            users.insertOne(new Document("user", username).append("password", passwordHash));
        } catch (MongoWriteException e) {
            if (e.getError().getCategory() == ErrorCategory.DUPLICATE_KEY) return false;
            throw e;
        }
        return true;
    }

    @Override
    public void updatePasswordHash(String username, String passwordHash) {
        users.updateOne(eq("user", username), Updates.set("password", passwordHash));
    }

    @Override
    public List<Conversation> getConversations(String username) {
        List<Conversation> result = new ArrayList<>();
        for (Document document : conversations.find(eq("users", username))) {
            result.add(toConversation(document));
        }
        return result;
    }

    @Override
    public Conversation getConversation(String conversationId) {
        if (!ObjectId.isValid(conversationId)) return null;
        Document document = conversations.find(byId(conversationId)).first();
        return document == null ? null : toConversation(document);
    }

    @Override
    public boolean conversationExists(Collection<String> members) {
        Set<String> distinct = new HashSet<>(members);
        Bson filter = Filters.and(Filters.all("users", distinct), Filters.size("users", distinct.size()));
        return conversations.find(filter).first() != null;
    }

    @Override
    public Conversation createConversation(String name, List<String> members) {
        Document document = new Document("name", name == null ? "" : name)
                .append("users", new ArrayList<>(members))
                .append("messages", new ArrayList<>());
        conversations.insertOne(document);
        return toConversation(document);
    }

    @Override
    public void addMessage(String conversationId, ChatMessage message) {
        Document document = new Document("content", message.content)
                .append("sender", message.sender)
                .append("timestamp", message.timestamp)
                .append("readBy", new ArrayList<>(message.readBy))
                .append("edited", message.edited)
                .append("isDeleted", message.isDeleted);
        conversations.updateOne(byId(conversationId), Updates.push("messages", document));
    }

    @Override
    public boolean editMessage(String conversationId, ChatMessage message, String newContent) {
        Bson update = Updates.combine(
                Updates.set("messages.$[msg].content", newContent),
                Updates.set("messages.$[msg].edited", true));
        return updateMessage(conversationId, message, update);
    }

    @Override
    public boolean deleteMessage(String conversationId, ChatMessage message) {
        return updateMessage(conversationId, message, Updates.set("messages.$[msg].isDeleted", true));
    }

    @Override
    public void markRead(String conversationId, String username) {
        UpdateOptions options = new UpdateOptions().arrayFilters(List.of(Filters.ne("msg.sender", username)));
        conversations.updateOne(byId(conversationId), Updates.addToSet("messages.$[msg].readBy", username), options);
    }

    @Override
    public void addMember(String conversationId, String username) {
        conversations.updateOne(byId(conversationId), Updates.addToSet("users", username));
    }

    @Override
    public void removeMember(String conversationId, String username) {
        conversations.updateOne(byId(conversationId), Updates.pull("users", username));
        conversations.deleteOne(Filters.and(byId(conversationId), Filters.size("users", 0)));
    }

    @Override
    public void close() {
        mongoClient.close();
    }

    // Applies an update to the message matching the sender and timestamp
    private boolean updateMessage(String conversationId, ChatMessage message, Bson update) {
        if (!ObjectId.isValid(conversationId)) return false;
        Bson messageMatch = Filters.and(eq("timestamp", message.timestamp), eq("sender", message.sender));
        Bson filter = Filters.and(byId(conversationId), Filters.elemMatch("messages", messageMatch));
        UpdateOptions options = new UpdateOptions().arrayFilters(List.of(
                Filters.and(eq("msg.timestamp", message.timestamp), eq("msg.sender", message.sender))));
        UpdateResult result = conversations.updateOne(filter, update, options);
        return result.getMatchedCount() > 0;
    }

    private static Bson byId(String conversationId) {
        return eq("_id", new ObjectId(conversationId));
    }

    private static Conversation toConversation(Document document) {
        Conversation conversation = new Conversation();
        conversation.id = document.getObjectId("_id").toHexString();
        conversation.name = Objects.requireNonNullElse(document.getString("name"), "");
        conversation.users = new ArrayList<>(document.getList("users", String.class, List.of()));
        for (Document messageDocument : document.getList("messages", Document.class, List.of())) {
            conversation.messages.add(toChatMessage(messageDocument));
        }
        return conversation;
    }

    private static ChatMessage toChatMessage(Document document) {
        ChatMessage message = new ChatMessage();
        message.content = Objects.requireNonNullElse(document.getString("content"), "");
        message.sender = document.getString("sender");
        Object timestamp = document.get("timestamp");
        message.timestamp = timestamp instanceof Number number ? number.longValue()
                : timestamp instanceof Date date ? date.getTime() : 0L;
        message.readBy = new ArrayList<>(document.getList("readBy", String.class, List.of()));
        message.edited = document.getBoolean("edited", false);
        message.isDeleted = document.getBoolean("isDeleted", false);
        return message;
    }
}
