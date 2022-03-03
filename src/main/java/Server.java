import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
// added interface to use server objects in the handler
interface Listener {
    void Send(String message);
    void list(PrintWriter out);
    void terminate(String message);
    //TODO: add functions for everything that needs to be done on server side; list,
    // TODO: also add broadcast and string message/ call broadcast in forloop to send everyone message
}

public class Server implements Runnable, Listener {

    private ArrayList<ConnectionHandler> connections;
    private ServerSocket server;
    private boolean done;
    private ExecutorService pool; //thread pool


    public Server() {
        connections = new ArrayList<>();
        done = false;
    }



    @Override
    public void run() {
        try {

            server = new ServerSocket(ChatApp.portnumber); //Server
            pool = Executors.newCachedThreadPool();
            int id = 1;
            while (!done) {
                Socket client = server.accept();  //accept method returns client socket
                ConnectionHandler handler = new ConnectionHandler(client, this, id++);
                connections.add(handler);
                pool.execute(handler);
            }
        } catch (IOException e) {  //Handle Server Shutdown
            e.printStackTrace();
        }
    }

    public void shutdown() throws IOException {
        done = true;
        if (!server.isClosed()) {
            server.close();
        }
        for (ConnectionHandler ch : connections) {
            ch.shutdown();
        }
    }

    @Override
    public void Send(String message) {
        System.out.println("Send called");
        String[] temp = message.split(" ");
        String temp2;
        for (ConnectionHandler ch : connections){
            temp2 = ch.id + "";
            if(temp2.equals(temp[1])){
                // not sure if it is suppose to be remote socket address or local
                ch.broadcast("Message received from "+ ch.client.getRemoteSocketAddress());
                ch.broadcast("Sender’s Port: " + ch.client.getPort() );
                ch.broadcast("Message: " + temp[2]);

            }
        }
        //this.connections
    }
    @Override
    public void list(PrintWriter out){

        out.println("id: IP address        Port No.");
        // this iterates through all connections to print list
        // I dont' know why address has a / in front of it. Not sure if it matter if it is there or not.
        if(!connections.isEmpty()) {
            for (ConnectionHandler ch : connections) {
                out.print(" " + ch.id + ": " + ch.client.getRemoteSocketAddress());
                out.print(" " + ch.id + ": " + ch.client.getLocalSocketAddress());
//            System.out.print(" " + ch.id + " " + ch.client.getRemoteSocketAddress());
//            System.out.print(" " + ch.id + " " + ch.client.getLocalSocketAddress());
                //TODO: not sure if im suppose to output local port or just port
//            out.println("             " + ch.client.getLocalPort());
                out.println("             " + ch.client.getLocalPort()); //Server listening port
            }
        }else{
            out.println("The List is Empty");
        }
    }

    @Override
    public void terminate(String message) {
        System.out.println("terminate called");
        String[] temp = message.split(" ");
        // weirdly Need to use this String variable to make the if work
        String temp2;
        for (ConnectionHandler ch : connections){
            temp2 = ch.id + "";
            if(temp2.equals(temp[1])){
                try {
                    ch.client.close();
                    ch.broadcast(temp[1] + "was terminated");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    class ConnectionHandler implements Runnable {  //Handler for each client that connects to server
        private Socket client;
        private BufferedReader in;
        private PrintWriter out;
        private String name;
        private int id;
        private Listener listener;


        public ConnectionHandler(Socket client, Listener listener, int id) {
            this.id = id;
            this.listener = listener;
            this.client = client;
        }


        @Override
        public void run(){

            try {
                String message = "";
                out = new PrintWriter(client.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(client.getInputStream()));

                while ((message = in.readLine()) != null) {
                    if (message.startsWith("send")) {
                        this.listener.Send(message);
                    } else if (message.startsWith("list")) {
                        this.listener.list(out);
                    } else if (message.startsWith("terminate")) {
                        this.listener.terminate(message);
                    } else {
                        broadcast("Id = " + id + " : " + message);// this sends to everyone
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }


        }

        public void sendMessage(String message) {
            out.println(message);
        }

        public void broadcast(String message) {
            for (ConnectionHandler ch : connections) {
                if (ch != null) {
                    ch.sendMessage(message);
                }
            }
        }

        public void shutdown() throws IOException { //client shutdown
            in.close();
            out.close();
            if (!client.isClosed()) {
                client.close();
            }
        }
    }

    public static void main(String[] args) {
        Server server = new Server();
        server.run();
    }
}
