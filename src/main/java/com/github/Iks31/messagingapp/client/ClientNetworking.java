package com.github.Iks31.messagingapp.client;

import com.github.Iks31.messagingapp.common.ChatMessage;
import com.github.Iks31.messagingapp.common.Conversation;
import com.github.Iks31.messagingapp.common.NetworkMessage;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.application.Platform;

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
        socket = new Socket(host, port);

        oos = new ObjectOutputStream(socket.getOutputStream());
        oos.flush();
        ois = new ObjectInputStream(socket.getInputStream());

        listenerService = new MessageListenerService(ois);
        listenerService.setOnSucceeded(event -> {
            NetworkMessage msg = listenerService.getValue();
            System.out.println(msg.getContent());
            if (handler != null) {
                Platform.runLater(() -> handler.onMessage(msg));
            }

            listenerService.restart();
        });

        listenerService.setOnFailed(event -> {
           System.out.println("Connection failed or error in listener service");
           ClientApp.showErrorDialog("Connection failed or error in listener service");
           event.getSource().getException().printStackTrace();
            Platform.exit();
            System.exit(0);
        });
        listenerService.start();
    }

    public void sendMessage(NetworkMessage msg) {
        try {
            oos.writeObject(msg);
            oos.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loginRequest(String username, String password) {
        ArrayList<String> creds = new ArrayList<>();
        creds.add(username);
        creds.add(password);
        this.username = username;
        sendMessage(new NetworkMessage("LOGIN", creds));
    }

    public void registrationRequest(String username, String password) {
        ArrayList<String> creds = new ArrayList<>();
        creds.add(username);
        creds.add(password);
        sendMessage(new NetworkMessage("REGISTER", creds));
    }

    public void conversationsRequest() {
        sendMessage(new NetworkMessage("GET_CONVERSATIONS", null));
    }

    //messageRequest sends a message over to the server to send a chat
    public void messageRequest(ArrayList<Object> conversationData) {
        sendMessage(new NetworkMessage("SEND_CHAT", conversationData));
    }

    public void createConversationRequest(Conversation conversation) {
        sendMessage(new NetworkMessage("CREATE_CONVERSATION", conversation));
    }

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
