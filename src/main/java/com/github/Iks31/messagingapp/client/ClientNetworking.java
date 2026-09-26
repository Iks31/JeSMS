package com.github.Iks31.messagingapp.client;

import com.github.Iks31.messagingapp.common.ChatMessage;
import com.github.Iks31.messagingapp.common.Conversation;
import com.github.Iks31.messagingapp.common.NetworkMessage;
import com.github.Iks31.messagingapp.common.Protocol;
import com.github.Iks31.messagingapp.common.Protocol.ChatRequest;
import com.github.Iks31.messagingapp.common.Protocol.Credentials;
import com.github.Iks31.messagingapp.common.Protocol.MemberRequest;
import com.github.Iks31.messagingapp.common.Protocol.PasswordChange;
import javafx.application.Platform;

import java.io.*;
import java.net.Socket;

public class ClientNetworking {
    private Socket socket;
    private ObjectOutputStream oos;
    private ObjectInputStream ois;
    private volatile boolean closing;

    private volatile MessageHandler handler;
    private Runnable onConnectionLost;
    private String username;
    private volatile String serverWelcome;

    public void setMessageHandler(MessageHandler handler) {
        this.handler = handler;
    }

    // Called on the JavaFX thread if the connection to the server drops unexpectedly
    public void setOnConnectionLost(Runnable onConnectionLost) {
        this.onConnectionLost = onConnectionLost;
    }

    public void connect(String host, int port) throws IOException {
        // Sets up input and output streams on socket
        socket = new Socket(host, port);
        oos = new ObjectOutputStream(socket.getOutputStream());
        oos.flush();
        ois = new ObjectInputStream(socket.getInputStream());

        // Listener thread that continually reads server messages and passes them to the current handler
        Thread listener = new Thread(this::listen, "server-listener");
        listener.setDaemon(true);
        listener.start();
    }

    private void listen() {
        try {
            while (true) {
                NetworkMessage<?> msg = (NetworkMessage<?>) ois.readObject();
                // Remembered so it can be shown even if it arrives before a handler is set
                if (Protocol.INIT_SUCCESS.equals(msg.getFlag())) {
                    serverWelcome = (String) msg.getContent();
                }
                Platform.runLater(() -> {
                    MessageHandler current = handler;
                    if (current != null) current.onMessage(msg);
                });
            }
        } catch (IOException | ClassNotFoundException | ClassCastException e) {
            if (!closing) {
                System.out.println("Connection to the server was lost: " + e.getMessage());
                Platform.runLater(() -> {
                    if (onConnectionLost != null) onConnectionLost.run();
                });
            }
        }
    }

    // Sends a network message object to the server
    public synchronized void sendMessage(NetworkMessage<?> msg) {
        try {
            oos.writeObject(msg);
            // Prevents the stream from caching objects so later changes are always sent
            oos.reset();
            oos.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Takes entered credentials and sends them to the server
    public void loginRequest(String username, String password) {
        sendMessage(new NetworkMessage<>(Protocol.LOGIN, new Credentials(username, password)));
    }

    // Sends registration credentials to the server
    public void registrationRequest(String username, String password) {
        sendMessage(new NetworkMessage<>(Protocol.REGISTER, new Credentials(username, password)));
    }

    public void changePasswordRequest(String oldPassword, String newPassword) {
        sendMessage(new NetworkMessage<>(Protocol.CHANGE_PASSWORD, new PasswordChange(oldPassword, newPassword)));
    }

    // Requests all conversation data from the server
    public void conversationsRequest() {
        sendMessage(new NetworkMessage<>(Protocol.GET_CONVERSATIONS, null));
    }

    // Requests that a message is sent in a conversation
    public void messageRequest(String conversationId, ChatMessage message) {
        sendMessage(new NetworkMessage<>(Protocol.SEND_CHAT, new ChatRequest(conversationId, message)));
    }

    // Requests that a message's content is replaced with the content of the given message
    public void editMessageRequest(String conversationId, ChatMessage message) {
        sendMessage(new NetworkMessage<>(Protocol.EDIT_CHAT, new ChatRequest(conversationId, message)));
    }

    public void deleteMessageRequest(String conversationId, ChatMessage message) {
        sendMessage(new NetworkMessage<>(Protocol.DELETE_CHAT, new ChatRequest(conversationId, message)));
    }

    // Marks every message in the conversation as read by the current user
    public void readConversationRequest(String conversationId) {
        sendMessage(new NetworkMessage<>(Protocol.READ_CONVERSATION, conversationId));
    }

    // Requests that a conversation is created
    public void createConversationRequest(Conversation conversation) {
        sendMessage(new NetworkMessage<>(Protocol.CREATE_CONVERSATION, conversation));
    }

    public void addMemberRequest(String conversationId, String user) {
        sendMessage(new NetworkMessage<>(Protocol.ADD_MEMBER, new MemberRequest(conversationId, user)));
    }

    public void leaveConversationRequest(String conversationId) {
        sendMessage(new NetworkMessage<>(Protocol.LEAVE_CONVERSATION, conversationId));
    }

    // Logs out the current user from the current connection
    public void logoutRequest() {
        sendMessage(new NetworkMessage<>(Protocol.LOGOUT, null));
    }

    // Closes the current connection between the client and server
    public void close() {
        closing = true;
        try {
            if (oos != null) {
                sendMessage(new NetworkMessage<>(Protocol.DISCONNECT, "Client shutting down"));
            }
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) { this.username = username; }

    public String getServerWelcome() { return serverWelcome; }
}
