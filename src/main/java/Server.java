import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server implements Runnable{

    private ArrayList<ConnectionHandler> connections;
    private ServerSocket server;
    private boolean done;
    private ExecutorService pool; //thread pool
    private int portnumber = 9999; //change to main.portnum

    public Server(){
        connections = new ArrayList<>();
        done = false;
    }


    @Override
    public void run() {
        try {
            server = new ServerSocket(portnumber); //Server
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
        public void run(){

            try {
                String message = "";
                out = new PrintWriter(client.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(client.getInputStream()));

                out.println("Enter a name: ");
                name = in.readLine();
                System.out.println(name+ " connected!");
                broadcast(name + " joined the chat!");
                while((message = in.readLine()) != null){
                    if(message.startsWith("/help")){
                        //Display information about the available user interface options or command manual.
                    }
                    if(message.startsWith("/myip")){
                        out.println("My ip address is: " + InetAddress.getLocalHost().getHostAddress());
                    }
                    if(message.startsWith("/myport")){
                        out.println("This process is listening on port: " + portnumber);
                    }
                    if(message.startsWith("/connect")){
                        //This  command establishes  a  new TCP  connection to  the  specified
                        // <destination> at the specified < port no>. The <destination> is the IP address of the computer. Any attempt
                        // to  connect  to  an  invalid  IP  should  be  rejected
                        //connect  <destination>  <port  no>  :

                        String[] messageSplit = message.split(" ",3);
                        if(messageSplit.length == 3){
                            shutdown();
                            Client client = new Client();
                            client.setIp_address(messageSplit[1]);
                            client.setPortnum(Integer.parseInt(messageSplit[2]));
                            client.run();



                        }
                    }
                    if(message.startsWith("/terminate")){
                        //This  command  will  terminate  the  connection  listed  under  the  specified
                        // number  when  LIST  is  used  to  display  all  connections.  E.g.,  terminate  2.
                        //terminate  <connection  id.>
                    }
                    if(message.startsWith("/send")){
                        //This will
                        // send the message to the host on the connection that is designated by the number 3 when command “list” is
                        // used.

                        //send  <connection id.>  <message>
                    }
                    if(message.startsWith("/exit")){
                        //Close all connections and terminate this process. The other peers should also update their connection
                        // list by removing the peer that exits.
                    }
                    else{
                        broadcast(name + ": " + message);
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
