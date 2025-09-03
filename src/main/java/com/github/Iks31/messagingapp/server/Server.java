package com.github.Iks31.messagingapp.server;

import com.github.Iks31.messagingapp.common.NetworkMessage;
import com.github.Iks31.messagingapp.server.db.DBResult;
import com.github.Iks31.messagingapp.server.db.MongoDatabase;
import com.github.Iks31.messagingapp.server.db.MongoDatabase.*;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.github.Iks31.messagingapp.server.db.MongoDatabase.*;


// Current server code analysis:
// - Server will accept connections initially regardless of login - inactivity for too long can disconnect a user to prevent too many connections while still not in
// - Ability to broadcast will be useful just not used very often
// - Main challenge will be realtime messaging - inter thread communication needed to inform another connected user of a new message and update

public class Server implements Runnable {

    private ArrayList<ConnectionHandler> connections;
    private ServerSocket server;
    private boolean done;
    private ExecutorService pool;
    private int noOfUsers;
    Dictionary<String,ConnectionHandler> connection;
    private MongoDatabase db;


    public Server() {
        connections = new ArrayList<>();
        done = false;
        db = new MongoDatabase();
    }

    public void run(){
        try {
            server = new ServerSocket(9999);
            pool = Executors.newCachedThreadPool();
            System.out.println("[START] Server started on port 9999...");
            while(!done) {
                Socket client = server.accept();
                System.out.println("[CONNECTION] Accepted connection from " + client.getInetAddress()); // returns client socket
                noOfUsers++;

                ObjectOutputStream oos = new ObjectOutputStream(client.getOutputStream());
                oos.flush();
                ObjectInputStream ois = new ObjectInputStream(client.getInputStream());

                oos.writeObject(new NetworkMessage("INIT_SUCCESS", "You have successfully connected to a JeSMS server!"));
                oos.flush();
                ConnectionHandler handler = new ConnectionHandler(client, client.getInetAddress(), ois, oos);
                connections.add(handler);
                pool.execute(handler);
            }
        } catch (Exception e) {
            e.printStackTrace();
            shutDown();
        }
    }

    public void broadcast(String message){

        for(ConnectionHandler ch : connections){
            if(ch != null){
                ch.sendMessage(new NetworkMessage("BROADCAST", message));
            }
        }
    }

    public void shutDown(){
        try{
            done = true;
            pool.shutdown();
            if(!server.isClosed()){
                server.close();
            }
            for(ConnectionHandler ch : connections){
                ch.shutdown();
            }
        } catch (IOException e) {
            //ignore
        }
    }


    class ConnectionHandler implements Runnable{
        private Socket client;
        private ObjectInputStream ois;
        private ObjectOutputStream oos;
        private InetAddress address;
        private String username = null;

        public ConnectionHandler(Socket client, InetAddress address, ObjectInputStream ois, ObjectOutputStream oos){
            this.client = client;
            this.address = address;
            this.ois = ois;
            this.oos = oos;
        }

        public InetAddress getAddress() {return address;}
        public String getUsername() {return username;}

        @Override
        public void run() {
            try{
                NetworkMessage message;
                while((message = (NetworkMessage) ois.readObject()) != null){
                    System.out.println("[RECEIVED] " + message.getFlag() + " from " + address);
                    if (message.getFlag().equals("DISCONNECT")) {
                        System.out.println("[DISCONNECT] " + address + " disconnected from the server");
                        shutdown();
                    } else if (message.getFlag().equals("LOGIN")) {
                        serveLoginRequest((ArrayList<String>) message.getContent());
                    } else if (message.getFlag().equals("REGISTER")) {
                        serveRegistrationRequest((ArrayList<String>) message.getContent());
                    } else if (message.getFlag().equals("GET_CONVERSATIONS")) {
                        serveConversationsRequest();
                    } else if (message.getFlag().equals("SEND_CHAT")) {
                        serveSendChatRequest(message.getContent());
                    }
                }
            }
            catch(IOException | ClassNotFoundException e){
                shutdown();
            }
        }

        public void sendMessage(NetworkMessage message){
            try {
                oos.writeObject(message);
                oos.flush();
            } catch (IOException e) {
                shutdown();
            }
        }

        public void serveLoginRequest(ArrayList<String> credentials) {
            System.out.println("[LOGIN ATTEMPT] " + address + " attempted to login");
            DBResult<String> log = db.login(credentials.getFirst());
            if (log.isSuccess() && log.getResult().getLast().equals(credentials.getLast())) {
                username = credentials.getFirst();
                System.out.println("[LOGIN SUCCESS] " + address + " successfully logged in as " + username);
                sendMessage(new NetworkMessage("LOGIN_SUCCESS", null));
            } else {
                System.out.println("[LOGIN FAILURE] " + address + " failed to login");
                sendMessage(new NetworkMessage("LOGIN_FAIL", "Username or password is incorrect. Please try again."));
            }
        }

        public void serveRegistrationRequest(ArrayList<String> credentials) {
            System.out.println("[REGISTER ATTEMPT] " + address + " attempted to register");
            boolean success = true;// Success here should be based on method call to database
            DBResult<String> result = db.newUser(credentials.getFirst(), credentials.getLast());
            if (result.isSuccess()) {
                System.out.println("[REGISTER SUCCESS] " + address + " successfully registered an account");
                sendMessage(new NetworkMessage("REGISTER_SUCCESS", null));
            } else {
                System.out.println("[REGISTER FAILURE] " + address + " failed to register an account");
                sendMessage(new NetworkMessage("REGISTER_FAIL", null));
            }
        }

        public void serveConversationsRequest() {
            System.out.println("[GET CONVERSATIONS] " + address + " requested their conversations");
            DBResult<String> log = db.getConversations(username);
            String conversations = "These are your conversations"; // Conversation message content here based on database result
            sendMessage(new NetworkMessage("GET_CONVERSATIONS", log.getResult().getLast()));
        }

        public void serveSendChatRequest(Object chatContent) {
            System.out.println("[SEND CHAT] " + address + " sent a chat");
            // Simulating active user
            String userTemplate = "user123";
            boolean realtime = false;
            // More efficient way of searching for active recipient here
            for (ConnectionHandler ch: connections) {
                if (ch.getUsername().equals(userTemplate)) {
                    realtime = true;
                    break;
                }
            }
            if (realtime) {
                realtimeChat();
            } else {
                regularChat();
            }
        }

        public void realtimeChat() {}
        public void regularChat() {

        }

        public void shutdown(){
            try {
                if (ois != null) {ois.close();};
                if (oos != null) {oos.close();};
                if (!client.isClosed()) {
                    client.close();
                }
            } catch(IOException ignored) {}
        }

    }

    public static void main(String[] args) {
        Server server = new Server();
        server.run();
    }
}
