import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;

public class Server implements Runnable{

    private ArrayList<ConnectionHandler> serverList;


    @Override
    public void run() {
        try {
            System.out.println("Enter a port number: ");
            Scanner sc = new Scanner(System.in);  //Im not sure if we are supposed to use scanner to get port number
            int portnum = sc.nextInt();
            ServerSocket server = new ServerSocket(portnum); //Server
            Socket client = server.accept();  //accept method returns client socket
            ConnectionHandler handler = new ConnectionHandler(client);
            serverList.add(handler);
        } catch (IOException e) {  //Handle Server Shutdown
            e.printStackTrace();
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
                out = new PrintWriter(client.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                out.println("Enter a name: ");
                name = in.readLine();
                System.out.println(name+ " connected!");
                broadcast(name + " joined the chat!");
                String message;
                while(message = in.readLine() != null){
                    if(message.startsWith("/name")){
                        //TODO: change nickname
                    }
                    if(message.startsWith("/quit")){
                        //TODO: quit
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
            for(ConnectionHandler ch : serverList){
                if(ch != null){
                    ch.sendMessage(message);
                }
            }
        }
    }
}
