package com.github.Iks31.messagingapp.server;

import com.github.Iks31.messagingapp.common.ChatMessage;
import com.github.Iks31.messagingapp.common.Conversation;
import com.github.Iks31.messagingapp.common.NetworkMessage;
import com.github.Iks31.messagingapp.common.Protocol;
import com.github.Iks31.messagingapp.common.Protocol.ChatRequest;
import com.github.Iks31.messagingapp.common.Protocol.Credentials;
import com.github.Iks31.messagingapp.common.Protocol.MemberRequest;
import com.github.Iks31.messagingapp.common.Protocol.PasswordChange;
import com.github.Iks31.messagingapp.server.db.InMemoryDatabase;
import com.github.Iks31.messagingapp.server.db.PasswordHasher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

// Runs a real server with in-memory storage and talks to it over sockets
class ServerIntegrationTest {
    private InMemoryDatabase db;
    private Server server;
    private final List<TestClient> clients = new ArrayList<>();

    @BeforeEach
    void startServer() throws IOException {
        db = new InMemoryDatabase();
        server = new Server(0, db);
        Thread thread = new Thread(server);
        thread.setDaemon(true);
        thread.start();
    }

    @AfterEach
    void stopServer() {
        clients.forEach(TestClient::close);
        server.shutDown();
    }

    // ---- Helpers ----

    static class TestClient {
        final Socket socket;
        final ObjectOutputStream out;
        final BlockingQueue<NetworkMessage<?>> received = new LinkedBlockingQueue<>();
        // Set once the server closes the connection
        volatile boolean disconnected;

        TestClient(int port) throws IOException {
            socket = new Socket("localhost", port);
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
            Thread reader = new Thread(() -> {
                try {
                    while (true) received.add((NetworkMessage<?>) in.readObject());
                } catch (Exception ignored) {
                    disconnected = true;
                }
            });
            reader.setDaemon(true);
            reader.start();
        }

        void send(String flag, Object content) throws IOException {
            out.writeObject(new NetworkMessage<>(flag, content));
            out.reset();
            out.flush();
        }

        // Waits for the next message with the given flag, skipping others
        NetworkMessage<?> expect(String flag) throws InterruptedException {
            long deadline = System.currentTimeMillis() + 5000;
            while (System.currentTimeMillis() < deadline) {
                NetworkMessage<?> msg = received.poll(deadline - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
                if (msg != null && msg.getFlag().equals(flag)) return msg;
            }
            fail("Timed out waiting for " + flag);
            return null;
        }

        // Asserts no message with the given flag arrives within a short time
        void expectNone(String flag) throws InterruptedException {
            long deadline = System.currentTimeMillis() + 300;
            while (System.currentTimeMillis() < deadline) {
                NetworkMessage<?> msg = received.poll(deadline - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
                if (msg != null) assertNotEquals(flag, msg.getFlag(), "Unexpected " + flag);
            }
        }

        void close() {
            try {
                socket.close();
            } catch (IOException ignored) {}
        }
    }

    private TestClient connect() throws Exception {
        TestClient client = new TestClient(server.getPort());
        clients.add(client);
        client.expect(Protocol.INIT_SUCCESS);
        return client;
    }

    private TestClient loggedIn(String username) throws Exception {
        TestClient client = connect();
        if (!db.userExists(username)) {
            client.send(Protocol.REGISTER, new Credentials(username, "password1"));
            client.expect(Protocol.REGISTER_SUCCESS);
        }
        client.send(Protocol.LOGIN, new Credentials(username, "password1"));
        client.expect(Protocol.LOGIN_SUCCESS);
        return client;
    }

    private Conversation create(TestClient client, String name, String... users) throws Exception {
        Conversation request = new Conversation();
        request.name = name;
        request.users = new ArrayList<>(List.of(users));
        client.send(Protocol.CREATE_CONVERSATION, request);
        return (Conversation) client.expect(Protocol.CREATE_CONVERSATION_SUCCESS).getContent();
    }

    // ---- Accounts ----

    @Test
    void registerAndLogin() throws Exception {
        TestClient client = connect();
        client.send(Protocol.REGISTER, new Credentials("alice", "password1"));
        client.expect(Protocol.REGISTER_SUCCESS);
        assertFalse(PasswordHasher.isLegacy(db.getPasswordHash("alice")), "passwords are stored hashed");

        client.send(Protocol.REGISTER, new Credentials("alice", "password1"));
        assertEquals("That username is already taken.", client.expect(Protocol.REGISTER_FAIL).getContent());

        client.send(Protocol.LOGIN, new Credentials("alice", "wrong"));
        client.expect(Protocol.LOGIN_FAIL);
        client.send(Protocol.LOGIN, new Credentials("alice", "password1"));
        assertEquals("alice", client.expect(Protocol.LOGIN_SUCCESS).getContent());
    }

    @Test
    void registrationIsValidatedByTheServer() throws Exception {
        TestClient client = connect();
        client.send(Protocol.REGISTER, new Credentials("ab", "password1"));
        client.expect(Protocol.REGISTER_FAIL);
        client.send(Protocol.REGISTER, new Credentials("valid_name", "123"));
        client.expect(Protocol.REGISTER_FAIL);
        assertFalse(db.userExists("ab"));
    }

    @Test
    void accountCannotBeLoggedInTwiceUntilLogout() throws Exception {
        TestClient first = loggedIn("alice");
        TestClient second = connect();
        second.send(Protocol.LOGIN, new Credentials("alice", "password1"));
        second.expect(Protocol.LOGIN_FAIL);

        first.send(Protocol.LOGOUT, null);
        first.expect(Protocol.LOGOUT_SUCCESS);
        second.send(Protocol.LOGIN, new Credentials("alice", "password1"));
        second.expect(Protocol.LOGIN_SUCCESS);
    }

    @Test
    void disconnectingFreesTheLogin() throws Exception {
        TestClient first = loggedIn("alice");
        first.close();
        TestClient second = connect();
        // The server notices the closed socket asynchronously
        for (int attempt = 0; attempt < 20; attempt++) {
            second.send(Protocol.LOGIN, new Credentials("alice", "password1"));
            NetworkMessage<?> reply = second.received.poll(5, TimeUnit.SECONDS);
            if (reply != null && Protocol.LOGIN_SUCCESS.equals(reply.getFlag())) return;
            Thread.sleep(50);
        }
        fail("Login was not freed after disconnect");
    }

    @Test
    void legacyPlainTextPasswordIsUpgradedOnLogin() throws Exception {
        db.createUser("olduser", "plainpass");
        TestClient client = connect();
        client.send(Protocol.LOGIN, new Credentials("olduser", "plainpass"));
        client.expect(Protocol.LOGIN_SUCCESS);
        assertFalse(PasswordHasher.isLegacy(db.getPasswordHash("olduser")));
        assertTrue(PasswordHasher.verify("plainpass", db.getPasswordHash("olduser")));
    }

    @Test
    void changePassword() throws Exception {
        TestClient client = loggedIn("alice");
        client.send(Protocol.CHANGE_PASSWORD, new PasswordChange("wrong", "newpassword"));
        client.expect(Protocol.CHANGE_PASSWORD_FAIL);
        client.send(Protocol.CHANGE_PASSWORD, new PasswordChange("password1", "newpassword"));
        client.expect(Protocol.CHANGE_PASSWORD_SUCCESS);
        assertTrue(PasswordHasher.verify("newpassword", db.getPasswordHash("alice")));
    }

    @Test
    void requestsRequireLogin() throws Exception {
        TestClient client = connect();
        client.send(Protocol.GET_CONVERSATIONS, null);
        client.expect(Protocol.REQUEST_FAIL);
    }

    // ---- Conversations ----

    @Test
    void creatingAConversationNotifiesBothUsers() throws Exception {
        TestClient alice = loggedIn("alice");
        TestClient bobby = loggedIn("bobby");

        Conversation created = create(alice, "", "bobby");
        assertNotNull(created.id);
        assertEquals(2, created.users.size());

        Conversation received = (Conversation) bobby.expect(Protocol.REALTIME_CONVERSATION).getContent();
        assertEquals(created.id, received.id);

        alice.send(Protocol.GET_CONVERSATIONS, null);
        @SuppressWarnings("unchecked")
        List<Conversation> list = (List<Conversation>) alice.expect(Protocol.CONVERSATIONS_RECEIVED).getContent();
        assertEquals(1, list.size());
    }

    @Test
    void invalidConversationsAreRejected() throws Exception {
        TestClient alice = loggedIn("alice");
        loggedIn("bobby");
        loggedIn("carol");

        Conversation self = new Conversation();
        self.users = new ArrayList<>(List.of("alice"));
        alice.send(Protocol.CREATE_CONVERSATION, self);
        alice.expect(Protocol.CREATE_CONVERSATION_FAIL);

        Conversation unknown = new Conversation();
        unknown.users = new ArrayList<>(List.of("nobody"));
        alice.send(Protocol.CREATE_CONVERSATION, unknown);
        alice.expect(Protocol.CREATE_CONVERSATION_FAIL);

        Conversation unnamedGroup = new Conversation();
        unnamedGroup.users = new ArrayList<>(List.of("bobby", "carol"));
        alice.send(Protocol.CREATE_CONVERSATION, unnamedGroup);
        alice.expect(Protocol.CREATE_CONVERSATION_FAIL);

        create(alice, "", "bobby");
        Conversation duplicate = new Conversation();
        duplicate.users = new ArrayList<>(List.of("bobby"));
        alice.send(Protocol.CREATE_CONVERSATION, duplicate);
        alice.expect(Protocol.CREATE_CONVERSATION_FAIL);
    }

    @Test
    void messagesAreStoredAndDeliveredInRealTime() throws Exception {
        TestClient alice = loggedIn("alice");
        TestClient bobby = loggedIn("bobby");
        Conversation conversation = create(alice, "", "bobby");

        alice.send(Protocol.SEND_CHAT, new ChatRequest(conversation.id, new ChatMessage("alice", "hello", 1000)));
        ChatRequest delivered = (ChatRequest) bobby.expect(Protocol.REALTIME_CHAT).getContent();
        assertEquals(conversation.id, delivered.conversationId());
        assertEquals("hello", delivered.message().content);
        assertEquals(1000, delivered.message().timestamp);
        alice.expectNone(Protocol.REALTIME_CHAT);

        assertEquals("hello", db.getConversation(conversation.id).messages.getFirst().content);
    }

    @Test
    void messagesToOfflineUsersAreLoadedLater() throws Exception {
        TestClient alice = loggedIn("alice");
        loggedIn("bobby").send(Protocol.LOGOUT, null);
        Conversation conversation = create(alice, "", "bobby");
        alice.send(Protocol.SEND_CHAT, new ChatRequest(conversation.id, new ChatMessage("alice", "while you were out", 5)));

        TestClient bobby = loggedIn("bobby");
        bobby.send(Protocol.GET_CONVERSATIONS, null);
        @SuppressWarnings("unchecked")
        List<Conversation> list = (List<Conversation>) bobby.expect(Protocol.CONVERSATIONS_RECEIVED).getContent();
        assertEquals("while you were out", list.getFirst().messages.getFirst().content);
    }

    @Test
    void senderCannotBeSpoofed() throws Exception {
        TestClient alice = loggedIn("alice");
        TestClient bobby = loggedIn("bobby");
        Conversation conversation = create(alice, "", "bobby");

        alice.send(Protocol.SEND_CHAT, new ChatRequest(conversation.id, new ChatMessage("bobby", "fake", 1)));
        ChatRequest delivered = (ChatRequest) bobby.expect(Protocol.REALTIME_CHAT).getContent();
        assertEquals("alice", delivered.message().sender);
    }

    @Test
    void nonMembersCannotSendMessages() throws Exception {
        TestClient alice = loggedIn("alice");
        loggedIn("bobby");
        TestClient carol = loggedIn("carol");
        Conversation conversation = create(alice, "", "bobby");

        carol.send(Protocol.SEND_CHAT, new ChatRequest(conversation.id, new ChatMessage("carol", "intruder", 1)));
        carol.expect(Protocol.REQUEST_FAIL);
        assertTrue(db.getConversation(conversation.id).messages.isEmpty());
    }

    @Test
    void editAndDeleteOwnMessages() throws Exception {
        TestClient alice = loggedIn("alice");
        TestClient bobby = loggedIn("bobby");
        Conversation conversation = create(alice, "", "bobby");
        ChatMessage message = new ChatMessage("alice", "helo", 42);
        alice.send(Protocol.SEND_CHAT, new ChatRequest(conversation.id, message));
        bobby.expect(Protocol.REALTIME_CHAT);

        message.content = "hello";
        alice.send(Protocol.EDIT_CHAT, new ChatRequest(conversation.id, message));
        ChatMessage edited = ((ChatRequest) bobby.expect(Protocol.REALTIME_EDIT).getContent()).message();
        assertEquals("hello", edited.content);
        assertTrue(edited.edited);

        alice.send(Protocol.DELETE_CHAT, new ChatRequest(conversation.id, message));
        ChatMessage deleted = ((ChatRequest) bobby.expect(Protocol.REALTIME_DELETE).getContent()).message();
        assertTrue(deleted.isDeleted);

        ChatMessage stored = db.getConversation(conversation.id).messages.getFirst();
        assertEquals("hello", stored.content);
        assertTrue(stored.edited);
        assertTrue(stored.isDeleted);

        // Deleted messages can no longer be edited
        alice.send(Protocol.EDIT_CHAT, new ChatRequest(conversation.id, message));
        alice.expect(Protocol.REQUEST_FAIL);
    }

    @Test
    void cannotEditOrDeleteOtherUsersMessages() throws Exception {
        TestClient alice = loggedIn("alice");
        TestClient bobby = loggedIn("bobby");
        Conversation conversation = create(alice, "", "bobby");
        ChatMessage message = new ChatMessage("alice", "mine", 7);
        alice.send(Protocol.SEND_CHAT, new ChatRequest(conversation.id, message));
        bobby.expect(Protocol.REALTIME_CHAT);

        ChatMessage tampered = new ChatMessage(message);
        tampered.content = "changed by bobby";
        bobby.send(Protocol.EDIT_CHAT, new ChatRequest(conversation.id, tampered));
        bobby.expect(Protocol.REQUEST_FAIL);
        bobby.send(Protocol.DELETE_CHAT, new ChatRequest(conversation.id, tampered));
        bobby.expect(Protocol.REQUEST_FAIL);

        ChatMessage stored = db.getConversation(conversation.id).messages.getFirst();
        assertEquals("mine", stored.content);
        assertFalse(stored.isDeleted);
    }

    @Test
    void readReceipts() throws Exception {
        TestClient alice = loggedIn("alice");
        TestClient bobby = loggedIn("bobby");
        Conversation conversation = create(alice, "", "bobby");
        alice.send(Protocol.SEND_CHAT, new ChatRequest(conversation.id, new ChatMessage("alice", "read me", 1)));
        bobby.expect(Protocol.REALTIME_CHAT);

        bobby.send(Protocol.READ_CONVERSATION, conversation.id);
        MemberRequest receipt = (MemberRequest) alice.expect(Protocol.REALTIME_READ).getContent();
        assertEquals("bobby", receipt.username());
        assertEquals(List.of("bobby"), db.getConversation(conversation.id).messages.getFirst().readBy);
    }

    @Test
    void addMemberAndLeaveGroup() throws Exception {
        TestClient alice = loggedIn("alice");
        TestClient bobby = loggedIn("bobby");
        TestClient carol = loggedIn("carol");
        TestClient dave = loggedIn("davey");
        Conversation group = create(alice, "Friends", "bobby", "carol");
        bobby.expect(Protocol.REALTIME_CONVERSATION);

        alice.send(Protocol.ADD_MEMBER, new MemberRequest(group.id, "davey"));
        Conversation updated = (Conversation) dave.expect(Protocol.REALTIME_CONVERSATION).getContent();
        assertTrue(updated.users.contains("davey"));
        assertEquals(4, ((Conversation) bobby.expect(Protocol.REALTIME_CONVERSATION).getContent()).users.size());

        alice.send(Protocol.ADD_MEMBER, new MemberRequest(group.id, "davey"));
        alice.expect(Protocol.REQUEST_FAIL);

        carol.send(Protocol.LEAVE_CONVERSATION, group.id);
        assertEquals(group.id, carol.expect(Protocol.REMOVED_FROM_CONVERSATION).getContent());
        Conversation afterLeave = (Conversation) alice.expect(Protocol.REALTIME_CONVERSATION).getContent();
        assertFalse(afterLeave.users.contains("carol"));

        // Carol can no longer post to the group
        carol.send(Protocol.SEND_CHAT, new ChatRequest(group.id, new ChatMessage("carol", "hi", 1)));
        carol.expect(Protocol.REQUEST_FAIL);
    }

    @Test
    void membersCannotBeAddedToDirectMessages() throws Exception {
        TestClient alice = loggedIn("alice");
        loggedIn("bobby");
        loggedIn("carol");
        Conversation dm = create(alice, "", "bobby");
        alice.send(Protocol.ADD_MEMBER, new MemberRequest(dm.id, "carol"));
        alice.expect(Protocol.REQUEST_FAIL);
    }

    @Test
    void conversationIsDeletedWhenEveryoneLeaves() throws Exception {
        TestClient alice = loggedIn("alice");
        TestClient bobby = loggedIn("bobby");
        Conversation dm = create(alice, "", "bobby");
        alice.send(Protocol.LEAVE_CONVERSATION, dm.id);
        alice.expect(Protocol.REMOVED_FROM_CONVERSATION);
        bobby.send(Protocol.LEAVE_CONVERSATION, dm.id);
        bobby.expect(Protocol.REMOVED_FROM_CONVERSATION);
        assertNull(db.getConversation(dm.id));
    }

    // ---- Robustness ----

    @Test
    void malformedRequestsDoNotKillTheConnection() throws Exception {
        TestClient alice = loggedIn("alice");
        alice.send(Protocol.SEND_CHAT, "not a chat request");
        alice.expect(Protocol.REQUEST_FAIL);
        alice.send(Protocol.GET_CONVERSATIONS, null);
        alice.expect(Protocol.CONVERSATIONS_RECEIVED);
    }

    @Test
    void disallowedClassesAreNotDeserialized() throws Exception {
        TestClient client = connect();
        // java.io.File is outside the allowed classes, so the server must drop the connection
        client.send(Protocol.LOGIN, new File("/"));
        long deadline = System.currentTimeMillis() + 5000;
        while (!client.disconnected && System.currentTimeMillis() < deadline) {
            Thread.sleep(20);
        }
        assertTrue(client.disconnected, "Connection was not closed");
    }
}
