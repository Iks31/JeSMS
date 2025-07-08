import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server implements Runnable{

    private ArrayList<ConnectionHandler> connections;
    private ServerSocket server;
    private boolean done;
    private ExecutorService pool;
    private int noOfUsers;
    Dictionary<String,ConnectionHandler> connection;

    public Server() {
        connections = new ArrayList<ConnectionHandler>();
        done = false;
    }

    public void run(){
        try {
            server = new ServerSocket(9999);
            pool = Executors.newCachedThreadPool();
            System.out.println("Server started on port 9999...");
            while(!done){
                Socket client = server.accept();
                System.out.println("Accepted connection from " + client.getInetAddress());// returns client socket
                //login
                BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                PrintWriter out = new PrintWriter(client.getOutputStream(), true);

                out.println("Please enter nickname:");
                out.flush();
                String nickname = in.readLine();
                ConnectionHandler handler = null;
                if (nickname != null && !nickname.trim().isEmpty()) {
                    handler = new ConnectionHandler(client,nickname);
                    connections.add(handler);
                    System.out.println(handler.getUID());
                    pool.execute(handler);
                } else {
                    out.println("Invalid nickname. Connection closed.");
                    client.close();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            shutDown();
        }
    }
    public String login(Socket client) {
        while(!done){
            try{
                PrintWriter out = new PrintWriter(client.getOutputStream(), true); //get what the client is outputting
                BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));//pass inputstreamreader and need to pass
                // inputstream got from client.get
                //out.println("hey" ); to send
                //in.readLine();  to receive
                out.println("Pls enter nickname");
                return in.readLine();
            }
            catch (Exception e) {
            }
        }
        return null;
    }

    public void broadcast(String message){

        for(ConnectionHandler ch : connections){
            if(ch != null){
                ch.sendMessage(message);
            }
        }
    }

    public void directMessage(String message, String name){
        for(ConnectionHandler ch : connections){
            if(ch.getUID().equals(name)){
                ch.sendMessage(message);
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
        private BufferedReader in;
        private PrintWriter out;
        private String nickname;

        public ConnectionHandler(Socket client, String nickname){
            this.client = client;
            this.nickname = nickname;
        }
        @Override
        public void run() {
            try{
                out = new PrintWriter(client.getOutputStream(), true); //get what the client is outputting
                in = new BufferedReader(new InputStreamReader(client.getInputStream()));//pass inputstreamreader and need to pass
                String message;
                while((message = in.readLine()) != null){
                    if(message.startsWith("/nick")){
                        String[] messageSplit = message.split(" ", 2);
                        if(messageSplit.length == 2){
                            while(!(message = in.readLine()).contains("/leave")){
                                directMessage(nickname + ": " + message, messageSplit[1]);
                            }
                        }
                        else{
                            System.out.println("Not a valid nickname");
                        }
                    }
                    else if (message.startsWith("/quit")){
                        broadcast(nickname + "left the chat");
                        shutdown();
                    }
                    else{
                        broadcast(nickname + ": " + message);
                    }
                }
            }
            catch(IOException e){
                shutdown();
            }
        }
        public String getUID(){
            return nickname;
        }
        public void sendMessage(String message){
            out.println(message);
        }

        public void shutdown(){
            try {
                in.close();
                out.close();
                if (!client.isClosed()) {
                    client.close();
                }
            }catch(IOException e){
                //ignore
            }
        }

    }
    public static void main(String[] args) {
        Server server = new Server();
        server.run();
    }
}
