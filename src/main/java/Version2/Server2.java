package Version2;// create a Server.java file and paste the code
import java.io.IOException; // libraries 
import java.net.ServerSocket;
import java.net.Socket;

public class Server2 {
    // create serverSocket class 
    private ServerSocket serverSocket;

    // constructor of ServerSocket class
    public Server2(ServerSocket serverSocket){
        this.serverSocket = serverSocket;
    }

    public void serverStart(){

        try{
            // check and loop the serverSocket
            while(!serverSocket.isClosed()){
                Socket socket = serverSocket.accept();
                System.out.println("New Friend Connected");
                // implemented an object which handle runnable class
                ClientHandler clientHandler = new ClientHandler(socket);

                Thread thread = new Thread(clientHandler);
                thread.start();
            }
        } catch (IOException e){

        }
    }
    // this will close the server
    public void closerServer(){

        try{
            if(serverSocket != null){
                serverSocket.close();
            }
        } catch(IOException e){
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws Exception {
        ServerSocket serverSocket = new ServerSocket(1234);
        Server2 server = new Server2(serverSocket);
        server.serverStart();
    }
}