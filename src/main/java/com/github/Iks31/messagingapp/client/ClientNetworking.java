package com.github.Iks31.messagingapp.client;

import com.github.Iks31.messagingapp.common.ChatMessage;
import com.github.Iks31.messagingapp.common.Conversation;
import com.github.Iks31.messagingapp.common.NetworkMessage;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.application.Platform;
import javafx.scene.control.Alert;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;

public class ClientNetworking {
    private Socket socket;
    private ObjectOutputStream oos;
    private ObjectInputStream ois;

    private MessageListenerService listenerService;
    private MessageHandler handler;
    private String username;

    public void setMessageHandler(MessageHandler handler) {
        this.handler = handler;
    }

    public void connect(String host, int port) throws IOException {
        // Sets up input and output streams on socket
        socket = new Socket(host, port);
        oos = new ObjectOutputStream(socket.getOutputStream());
        oos.flush();
        ois = new ObjectInputStream(socket.getInputStream());

        // Creation of listener service that listens to server messages
        listenerService = new MessageListenerService(ois);
        // Handles messages based on the current handler being used
        listenerService.setOnSucceeded(event -> {
            NetworkMessage msg = listenerService.getValue();
            if (handler != null) {
                Platform.runLater(() -> handler.onMessage(msg));
            }

            listenerService.restart();
        });

        // Shows error dialog if server connection or listener fails
        listenerService.setOnFailed(event -> {
           System.out.println("Connection failed or error in listener service");
           ClientApp.showErrorDialog(Alert.AlertType.ERROR,"Connection Error", "Server Connection Problem","Connection failed or error in listener service");
           event.getSource().getException().printStackTrace();
            Platform.exit();
            System.exit(0);
        });

        listenerService.start();
    }

    // Sends a network message object to the server
    public void sendMessage(NetworkMessage msg) {
        try {
            oos.writeObject(msg);
            oos.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Takes entered credentials and sends them to the server
    public void loginRequest(String username, String password) {
        ArrayList<String> creds = new ArrayList<>();
        creds.add(username);
        creds.add(password);
        this.username = username;
        sendMessage(new NetworkMessage("LOGIN", creds));
    }

    // Sends registration credentials to the server
    public void registrationRequest(String username, String password) {
        ArrayList<String> creds = new ArrayList<>();
        creds.add(username);
        creds.add(password);
        sendMessage(new NetworkMessage("REGISTER", creds));
    }

    // Requests all conversation data from the server
    public void conversationsRequest() {
        sendMessage(new NetworkMessage("GET_CONVERSATIONS", null));
    }

    // Requests that a particular message is sent in a chat
    public void messageRequest(ArrayList<Object> conversationData) {
        sendMessage(new NetworkMessage("SEND_CHAT", conversationData));
    }

    // Requests that a conversation is created
    public void createConversationRequest(Conversation conversation) {
        sendMessage(new NetworkMessage("CREATE_CONVERSATION", conversation));
    }

    // Logs out the current user from the current connection
    public void logoutRequest() {
        sendMessage(new NetworkMessage("LOGOUT", null));
    }

    // Closes the current connection between the client and server
    public void close() {
        try {
            if (oos != null) {
                oos.writeObject(new NetworkMessage("DISCONNECT", "Client shutting down"));
                oos.flush();
            }
            if (listenerService != null) {
                listenerService.cancel();
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

    // Listener service continually reads network messages from the input stream
    private static class MessageListenerService extends Service<NetworkMessage> {
        private final ObjectInputStream ois;

        public MessageListenerService(ObjectInputStream ois) {
            this.ois = ois;
        }

        @Override
        protected Task<NetworkMessage> createTask() {
            return new Task<NetworkMessage>() {
                protected NetworkMessage call() throws Exception {
                    return (NetworkMessage) ois.readObject();
                }
            };
        }
    }


}
