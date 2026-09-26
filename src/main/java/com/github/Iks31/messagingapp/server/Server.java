package com.github.Iks31.messagingapp.server;

import com.github.Iks31.messagingapp.common.ChatMessage;
import com.github.Iks31.messagingapp.common.Conversation;
import com.github.Iks31.messagingapp.common.NetworkMessage;
import com.github.Iks31.messagingapp.common.Protocol;
import com.github.Iks31.messagingapp.common.Protocol.ChatRequest;
import com.github.Iks31.messagingapp.common.Protocol.Credentials;
import com.github.Iks31.messagingapp.common.Protocol.MemberRequest;
import com.github.Iks31.messagingapp.common.Protocol.PasswordChange;
import com.github.Iks31.messagingapp.server.db.Database;
import com.github.Iks31.messagingapp.server.db.InMemoryDatabase;
import com.github.Iks31.messagingapp.server.db.MongoDatabase;
import com.github.Iks31.messagingapp.server.db.PasswordHasher;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server implements Runnable {
    public static final int DEFAULT_PORT = 9999;

    // Only the shared model classes (and the JDK types they use) may be deserialized from clients
    private static final ObjectInputFilter INPUT_FILTER = ObjectInputFilter.Config.createFilter(
            "maxdepth=20;maxarray=100000;com.github.Iks31.messagingapp.common.**;java.lang.*;java.util.*;!*");

    private final Map<String, ConnectionHandler> loggedInConnections = new ConcurrentHashMap<>();
    private final List<ConnectionHandler> connections = new CopyOnWriteArrayList<>();
    private final ExecutorService pool = Executors.newCachedThreadPool();
    // Serialises operations that check then modify conversation membership
    private final Object conversationLock = new Object();
    private final Database db;
    private final ServerSocket server;
    private volatile boolean done;

    public Server(int port, Database db) throws IOException {
        this.db = db;
        this.server = new ServerSocket(port);
    }

    public int getPort() {
        return server.getLocalPort();
    }

    // Accepts connections until shut down
    public void run() {
        System.out.println("[START] Server started on port " + getPort() + "...");
        while (!done) {
            try {
                Socket client = server.accept();
                System.out.println("[CONNECTION] Accepted connection from " + client.getInetAddress());
                ConnectionHandler handler = new ConnectionHandler(client);
                connections.add(handler);
                pool.execute(handler);
            } catch (IOException e) {
                if (!done) e.printStackTrace();
            }
        }
    }

    public void shutDown() {
        done = true;
        try {
            server.close();
        } catch (IOException ignored) {}
        for (ConnectionHandler ch : connections) {
            ch.shutdown();
        }
        pool.shutdownNow();
        db.close();
    }

    // Delivers a message to a user if they are currently logged in
    private void sendToUser(String user, NetworkMessage<?> message) {
        ConnectionHandler handler = loggedInConnections.get(user);
        if (handler != null) {
            handler.sendMessage(message);
        }
    }

    // Delivers a message to every member of a conversation except one
    private void notifyMembers(Collection<String> users, String except, NetworkMessage<?> message) {
        for (String user : users) {
            if (!user.equals(except)) sendToUser(user, message);
        }
    }

    // Thrown to reject a request with a message that is shown to the user
    private static class RequestException extends Exception {
        RequestException(String message) {
            super(message);
        }
    }

    class ConnectionHandler implements Runnable {
        private final Socket client;
        private final InetAddress address;
        private ObjectOutputStream oos;
        private ObjectInputStream ois;
        private volatile String username;

        public ConnectionHandler(Socket client) {
            this.client = client;
            this.address = client.getInetAddress();
        }

        @Override
        public void run() {
            try {
                oos = new ObjectOutputStream(client.getOutputStream());
                oos.flush();
                ois = new ObjectInputStream(client.getInputStream());
                ois.setObjectInputFilter(INPUT_FILTER);
                sendMessage(new NetworkMessage<>(Protocol.INIT_SUCCESS, "You have successfully connected to a JeSMS server!"));

                while (!client.isClosed()) {
                    Object received = ois.readObject();
                    if (!(received instanceof NetworkMessage<?> message)) {
                        continue;
                    }
                    System.out.println("[RECEIVED] " + message.getFlag() + " from " + describe());
                    handle(message);
                }
            } catch (EOFException | SocketException e) {
                // Client closed the connection
            } catch (IOException | ClassNotFoundException e) {
                System.out.println("[ERROR] " + describe() + ": " + e.getMessage());
            } finally {
                shutdown();
            }
        }

        private String describe() {
            return username == null ? address.toString() : username + " (" + address + ")";
        }

        private void handle(NetworkMessage<?> message) {
            Object content = message.getContent();
            try {
                switch (message.getFlag()) {
                    case Protocol.DISCONNECT -> {
                        System.out.println("[DISCONNECT] " + describe() + " disconnected from the server");
                        shutdown();
                    }
                    case Protocol.LOGIN -> serveLoginRequest((Credentials) content);
                    case Protocol.REGISTER -> serveRegistrationRequest((Credentials) content);
                    default -> {
                        if (username == null) {
                            throw new RequestException("You must be logged in to do that.");
                        }
                        handleAuthenticated(message.getFlag(), content);
                    }
                }
            } catch (RequestException e) {
                sendMessage(new NetworkMessage<>(Protocol.REQUEST_FAIL, e.getMessage()));
            } catch (ClassCastException | NullPointerException e) {
                sendMessage(new NetworkMessage<>(Protocol.REQUEST_FAIL, "Malformed request."));
            } catch (RuntimeException e) {
                e.printStackTrace();
                sendMessage(new NetworkMessage<>(Protocol.REQUEST_FAIL, "There is an internal error. Please try again."));
            }
        }

        private void handleAuthenticated(String flag, Object content) throws RequestException {
            switch (flag) {
                case Protocol.LOGOUT -> logOut();
                case Protocol.GET_CONVERSATIONS -> serveConversationsRequest();
                case Protocol.CREATE_CONVERSATION -> serveCreateConversationRequest((Conversation) content);
                case Protocol.SEND_CHAT -> serveSendChatRequest((ChatRequest) content);
                case Protocol.EDIT_CHAT -> serveEditMessageRequest((ChatRequest) content);
                case Protocol.DELETE_CHAT -> serveDeleteMessageRequest((ChatRequest) content);
                case Protocol.READ_CONVERSATION -> serveReadRequest((String) content);
                case Protocol.ADD_MEMBER -> serveAddMemberRequest((MemberRequest) content);
                case Protocol.LEAVE_CONVERSATION -> serveLeaveRequest((String) content);
                case Protocol.CHANGE_PASSWORD -> serveChangePasswordRequest((PasswordChange) content);
                default -> throw new RequestException("Unknown request: " + flag);
            }
        }

        public void sendMessage(NetworkMessage<?> message) {
            if (oos == null) return;
            synchronized (oos) {
                try {
                    oos.writeObject(message);
                    // Prevents the stream from caching objects so later changes are always sent
                    oos.reset();
                    oos.flush();
                } catch (IOException e) {
                    shutdown();
                }
            }
        }

        // ---- Accounts ----

        public void serveLoginRequest(Credentials credentials) {
            System.out.println("[LOGIN ATTEMPT] " + address + " attempted to login");
            if (username != null) {
                sendMessage(new NetworkMessage<>(Protocol.LOGIN_FAIL, "You are already logged in!"));
                return;
            }
            String stored = db.getPasswordHash(credentials.username());
            if (stored == null || !PasswordHasher.verify(credentials.password(), stored)) {
                System.out.println("[LOGIN FAILURE] " + address + " failed to login");
                sendMessage(new NetworkMessage<>(Protocol.LOGIN_FAIL, "Username or password is incorrect. Please try again."));
                return;
            }
            if (loggedInConnections.putIfAbsent(credentials.username(), this) != null) {
                sendMessage(new NetworkMessage<>(Protocol.LOGIN_FAIL, "This account is already logged in elsewhere!"));
                return;
            }
            username = credentials.username();
            if (PasswordHasher.isLegacy(stored)) {
                db.updatePasswordHash(username, PasswordHasher.hash(credentials.password()));
            }
            System.out.println("[LOGIN SUCCESS] " + address + " successfully logged in as " + username);
            sendMessage(new NetworkMessage<>(Protocol.LOGIN_SUCCESS, username));
        }

        public void serveRegistrationRequest(Credentials credentials) {
            System.out.println("[REGISTER ATTEMPT] " + address + " attempted to register");
            String error = Protocol.validateUsername(credentials.username());
            if (error == null) error = Protocol.validatePassword(credentials.password());
            if (error == null && !db.createUser(credentials.username(), PasswordHasher.hash(credentials.password()))) {
                error = "That username is already taken.";
            }
            if (error == null) {
                System.out.println("[REGISTER SUCCESS] " + address + " registered " + credentials.username());
                sendMessage(new NetworkMessage<>(Protocol.REGISTER_SUCCESS, "Registration successful! You can now log in."));
            } else {
                System.out.println("[REGISTER FAILURE] " + address + " failed to register an account");
                sendMessage(new NetworkMessage<>(Protocol.REGISTER_FAIL, error));
            }
        }

        public void serveChangePasswordRequest(PasswordChange change) {
            String stored = db.getPasswordHash(username);
            String error = null;
            if (!PasswordHasher.verify(change.oldPassword(), stored)) {
                error = "Current password is incorrect.";
            } else {
                error = Protocol.validatePassword(change.newPassword());
            }
            if (error != null) {
                sendMessage(new NetworkMessage<>(Protocol.CHANGE_PASSWORD_FAIL, error));
                return;
            }
            db.updatePasswordHash(username, PasswordHasher.hash(change.newPassword()));
            System.out.println("[CHANGE PASSWORD] " + describe() + " changed their password");
            sendMessage(new NetworkMessage<>(Protocol.CHANGE_PASSWORD_SUCCESS, "Password changed."));
        }

        public void logOut() {
            System.out.println("[LOGOUT] " + describe() + " logged out");
            loggedInConnections.remove(username, this);
            username = null;
            sendMessage(new NetworkMessage<>(Protocol.LOGOUT_SUCCESS, null));
        }

        // ---- Conversations ----

        public void serveConversationsRequest() {
            System.out.println("[GET CONVERSATIONS] " + describe() + " requested their conversations");
            try {
                sendMessage(new NetworkMessage<>(Protocol.CONVERSATIONS_RECEIVED, new ArrayList<>(db.getConversations(username))));
            } catch (RuntimeException e) {
                e.printStackTrace();
                sendMessage(new NetworkMessage<>(Protocol.CONVERSATIONS_NOT_RECEIVED, "Your conversations could not be loaded."));
            }
        }

        public void serveCreateConversationRequest(Conversation request) {
            System.out.println("[CREATE CONVERSATION] " + describe() + " requested a conversation");
            String error;
            Conversation created = null;
            synchronized (conversationLock) {
                LinkedHashSet<String> members = new LinkedHashSet<>(request.users);
                members.add(username);
                String name = request.name == null ? "" : request.name.trim();
                error = validateNewConversation(members, name);
                if (error == null) {
                    created = db.createConversation(members.size() > 2 ? name : "", new ArrayList<>(members));
                }
            }
            if (error != null) {
                sendMessage(new NetworkMessage<>(Protocol.CREATE_CONVERSATION_FAIL, error));
                return;
            }
            sendMessage(new NetworkMessage<>(Protocol.CREATE_CONVERSATION_SUCCESS, created));
            notifyMembers(created.users, username, new NetworkMessage<>(Protocol.REALTIME_CONVERSATION, created));
        }

        private String validateNewConversation(Set<String> members, String name) {
            if (members.size() < 2) {
                return "You cannot create a conversation with only yourself.";
            }
            if (members.size() > 2 && name.isEmpty()) {
                return "Enter a chat name for group chats.";
            }
            if (name.length() > Protocol.MAX_CONVERSATION_NAME_LENGTH) {
                return "Chat names can be at most " + Protocol.MAX_CONVERSATION_NAME_LENGTH + " characters.";
            }
            for (String user : members) {
                if (!db.userExists(user)) return "User '" + user + "' does not exist.";
            }
            if (db.conversationExists(members)) {
                return "A conversation with these members already exists.";
            }
            return null;
        }

        // Returns the conversation if it exists and the current user is a member
        private Conversation requireMembership(String conversationId) throws RequestException {
            Conversation conversation = db.getConversation(conversationId);
            if (conversation == null || !conversation.users.contains(username)) {
                throw new RequestException("That conversation no longer exists.");
            }
            return conversation;
        }

        public void serveAddMemberRequest(MemberRequest request) throws RequestException {
            Conversation updated;
            synchronized (conversationLock) {
                Conversation conversation = requireMembership(request.conversationId());
                String newMember = request.username() == null ? "" : request.username().trim();
                if (!db.userExists(newMember)) {
                    throw new RequestException("User '" + newMember + "' does not exist.");
                }
                if (conversation.users.contains(newMember)) {
                    throw new RequestException(newMember + " is already in this conversation.");
                }
                if (!conversation.isGroup()) {
                    throw new RequestException("Members can only be added to group chats.");
                }
                Set<String> members = new HashSet<>(conversation.users);
                members.add(newMember);
                if (db.conversationExists(members)) {
                    throw new RequestException("A conversation with these members already exists.");
                }
                db.addMember(conversation.id, newMember);
                updated = db.getConversation(conversation.id);
            }
            System.out.println("[ADD MEMBER] " + describe() + " added " + request.username() + " to " + updated.id);
            notifyMembers(updated.users, null, new NetworkMessage<>(Protocol.REALTIME_CONVERSATION, updated));
        }

        public void serveLeaveRequest(String conversationId) throws RequestException {
            Conversation updated;
            synchronized (conversationLock) {
                requireMembership(conversationId);
                db.removeMember(conversationId, username);
                updated = db.getConversation(conversationId);
            }
            System.out.println("[LEAVE CONVERSATION] " + describe() + " left " + conversationId);
            sendMessage(new NetworkMessage<>(Protocol.REMOVED_FROM_CONVERSATION, conversationId));
            if (updated != null) {
                notifyMembers(updated.users, username, new NetworkMessage<>(Protocol.REALTIME_CONVERSATION, updated));
            }
        }

        public void serveReadRequest(String conversationId) throws RequestException {
            Conversation conversation = requireMembership(conversationId);
            db.markRead(conversationId, username);
            notifyMembers(conversation.users, username,
                    new NetworkMessage<>(Protocol.REALTIME_READ, new MemberRequest(conversationId, username)));
        }

        // ---- Messages ----

        private void validateContent(String content) throws RequestException {
            if (content == null || content.isBlank()) {
                throw new RequestException("Messages cannot be empty.");
            }
            if (content.length() > Protocol.MAX_MESSAGE_LENGTH) {
                throw new RequestException("Messages can be at most " + Protocol.MAX_MESSAGE_LENGTH + " characters.");
            }
        }

        public void serveSendChatRequest(ChatRequest request) throws RequestException {
            Conversation conversation = requireMembership(request.conversationId());
            ChatMessage received = request.message();
            validateContent(received.content);
            if (received.timestamp <= 0) {
                throw new RequestException("Message has an invalid timestamp.");
            }
            // Only trust the content and timestamp sent by the client
            ChatMessage message = new ChatMessage(username, received.content, received.timestamp);
            if (conversation.findMessage(message) != null) {
                throw new RequestException("Duplicate message.");
            }
            db.addMessage(conversation.id, message);
            System.out.println("[SEND CHAT] " + describe() + " sent a chat to " + conversation.id);
            notifyMembers(conversation.users, username,
                    new NetworkMessage<>(Protocol.REALTIME_CHAT, new ChatRequest(conversation.id, message)));
        }

        // Returns the stored message if it exists and was sent by the current user
        private ChatMessage requireOwnMessage(Conversation conversation, ChatMessage target) throws RequestException {
            ChatMessage stored = conversation.findMessage(target);
            if (stored == null || stored.isDeleted) {
                throw new RequestException("That message no longer exists.");
            }
            if (!username.equals(stored.sender)) {
                throw new RequestException("You can only change your own messages.");
            }
            return stored;
        }

        public void serveEditMessageRequest(ChatRequest request) throws RequestException {
            Conversation conversation = requireMembership(request.conversationId());
            ChatMessage stored = requireOwnMessage(conversation, request.message());
            validateContent(request.message().content);
            if (!db.editMessage(conversation.id, stored, request.message().content)) {
                throw new RequestException("That message no longer exists.");
            }
            stored.content = request.message().content;
            stored.edited = true;
            System.out.println("[EDIT MESSAGE] " + describe() + " edited a chat in " + conversation.id);
            notifyMembers(conversation.users, username,
                    new NetworkMessage<>(Protocol.REALTIME_EDIT, new ChatRequest(conversation.id, stored)));
        }

        public void serveDeleteMessageRequest(ChatRequest request) throws RequestException {
            Conversation conversation = requireMembership(request.conversationId());
            ChatMessage stored = requireOwnMessage(conversation, request.message());
            if (!db.deleteMessage(conversation.id, stored)) {
                throw new RequestException("That message no longer exists.");
            }
            stored.isDeleted = true;
            System.out.println("[DELETE MESSAGE] " + describe() + " deleted a chat in " + conversation.id);
            notifyMembers(conversation.users, username,
                    new NetworkMessage<>(Protocol.REALTIME_DELETE, new ChatRequest(conversation.id, stored)));
        }

        public void shutdown() {
            if (username != null) {
                loggedInConnections.remove(username, this);
            }
            connections.remove(this);
            try {
                client.close();
            } catch (IOException ignored) {}
        }
    }

    // Usage: Server [--port N] [--in-memory | --mongo-uri URI] [--db NAME]
    // Environment variables JESMS_PORT, JESMS_MONGO_URI and JESMS_DB_NAME are used as defaults.
    public static void main(String[] args) throws IOException {
        int port = Integer.parseInt(Objects.requireNonNullElse(System.getenv("JESMS_PORT"), String.valueOf(DEFAULT_PORT)));
        String mongoUri = Objects.requireNonNullElse(System.getenv("JESMS_MONGO_URI"), "mongodb://localhost:27017");
        String dbName = Objects.requireNonNullElse(System.getenv("JESMS_DB_NAME"), "JeSMS");
        boolean inMemory = false;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--port" -> port = Integer.parseInt(args[++i]);
                case "--mongo-uri" -> mongoUri = args[++i];
                case "--db" -> dbName = args[++i];
                case "--in-memory" -> inMemory = true;
                default -> {
                    System.err.println("Unknown argument: " + args[i]);
                    System.err.println("Usage: Server [--port N] [--in-memory | --mongo-uri URI] [--db NAME]");
                    System.exit(1);
                }
            }
        }

        Database db;
        if (inMemory) {
            System.out.println("[DATABASE] Using in-memory storage (data is lost when the server stops)");
            db = new InMemoryDatabase();
        } else {
            try {
                db = new MongoDatabase(mongoUri, dbName);
                System.out.println("[DATABASE] Connected to MongoDB database '" + dbName + "'");
            } catch (RuntimeException e) {
                System.err.println("[DATABASE] Could not connect to MongoDB: " + e.getMessage());
                System.err.println("Set JESMS_MONGO_URI, pass --mongo-uri, or use --in-memory.");
                System.exit(1);
                return;
            }
        }

        Server server = new Server(port, db);
        Runtime.getRuntime().addShutdownHook(new Thread(server::shutDown));
        server.run();
    }
}
