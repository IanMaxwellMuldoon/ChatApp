import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server implements Runnable{

    private ArrayList<ConnectionHandler> connections;
    private ServerSocket server;
    private boolean done;
    private ExecutorService pool; //thread pool

    public Server(){
        connections = new ArrayList<>();
        done = false;
    }


    @Override
    public void run() {
        try {
            //System.out.println("Enter a port number: ");
           // Scanner sc = new Scanner(System.in);  //Im not sure if we are supposed to use scanner to get port number
           // int portnum = sc.nextInt();
            server = new ServerSocket(9999); //Server
            pool = Executors.newCachedThreadPool();
            while(!done) {
                Socket client = server.accept();  //accept method returns client socket
                ConnectionHandler handler = new ConnectionHandler(client);
                connections.add(handler);
                pool.execute(handler);
            }} catch (IOException e) {  //Handle Server Shutdown
            e.printStackTrace();
        }
    }
    public void shutdown() throws IOException {
        done = true;
        if(!server.isClosed()){
            server.close();
        }
        for(ConnectionHandler ch : connections){
            ch.shutdown();
        }
    }

    class ConnectionHandler implements Runnable{  //Handler for each client that connects to server
        private Socket client;
        private BufferedReader in;
        private PrintWriter out;
        private String name;



        public ConnectionHandler(Socket client) {
            this.client = client;
        }

        @Override
        public void run() {

            try {
                String message = "";
                out = new PrintWriter(client.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                out.println("Enter a name: ");
                name = in.readLine();
                System.out.println(name+ " connected!");
                broadcast(name + " joined the chat!");
                while((message = in.readLine()) != null){
                    if(message.startsWith("/name")){
                        //TODO: change nickname
                    }
                    if(message.startsWith("/quit")){
                        broadcast(name + " has left the server");
                        shutdown();
                    }
                    else{
                        broadcast(name + " : " + message);
                    }


                }

            } catch (IOException e) {
                e.printStackTrace();
            }


        }
        public void sendMessage(String message){
            out.println(message);
        }
        public void broadcast(String message){
            for(ConnectionHandler ch : connections){
                if(ch != null){
                    ch.sendMessage(message);
                }
            }
        }
        public void shutdown() throws IOException { //client shutdown
            in.close();
            out.close();
            if(!client.isClosed()){
                client.close();
            }
        }
    }

    public static void main(String[] args) {
        Server server = new Server();
        server.run();
    }
}
