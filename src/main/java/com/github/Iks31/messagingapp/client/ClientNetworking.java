package com.github.Iks31.messagingapp.client;

import com.github.Iks31.messagingapp.common.NetworkMessage;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.application.Platform;

import java.io.*;
import java.net.Socket;

public class ClientNetworking {
    private Socket socket;
    private ObjectOutputStream oos;
    private ObjectInputStream ois;

    private MessageListenerService listenerService;
    private MessageHandler handler;

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
            if (handler != null) {
                Platform.runLater(() -> handler.onMessage(msg));
            }

            listenerService.restart();
        });

        listenerService.setOnFailed(event -> {
           System.out.println("Connection failed or error in listener service");
           event.getSource().getException().printStackTrace();
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

    public void close() {
        try {
            if (listenerService != null) {
                listenerService.cancel();
            }
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
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
