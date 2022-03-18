
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;
import java.util.*;


public class ChatApp {
    private int myport;
    private InetAddress myIP;
    private Map<Integer, Destination> connections = new TreeMap<>();
    private int clientCounter = 1;
    private Server messageReciever;


    //constructor
    private ChatApp(int port) {
        this.myport = port;
    }


    //getter methods
    private int getMyPort() {
        return myport;
    }


    private String getMyIP() {
        return myIP.getHostAddress();
    }


    private void help() {
        System.out.println("You can select any of these commands:");
        System.out.println("help");
        System.out.println("myip");
        System.out.println("myport");
        System.out.println("connect<ipaddress><portnum>");
        System.out.println("list");
        System.out.println("terminate<connection id>");
        System.out.println("send<connection id><message>");
        System.out.println("exit");
    }


    private void Send(String message) {
        String[] messageArray = message.split(" ");
        if (messageArray.length > 2) {
            try {
                int id = Integer.parseInt(messageArray[1]); // get the connection id
                Destination destinationHost = connections.get(id); //get the connection from the connections list
                System.out.println("id = " + connections.get(id));
                if (destinationHost != null) {
                    StringBuilder text = new StringBuilder(); // Build the string message from the console message
                    for (int i = 2; i < messageArray.length; i++) {
                        text.append(messageArray[i]);
                        text.append(" ");
                    }
                    destinationHost.Send(text.toString()); // call Send method on connection with message as param
                    System.out.println("Message sent");
                } else {
                    System.out.println("Invalid Connection. Check list command");
                }
            } catch (NumberFormatException e) {
                System.out.println("Not a valid ID Number. Check list command");
            }
        }
    }


    private void list() {
        // this iterates through all connections to print list
        System.out.println("id: IP address        Port No.");
        if (!connections.isEmpty()) {
            for (Integer id : connections.keySet()) {
                Destination connection = connections.get(id);
                System.out.println(id + "\t" + connection.toString());
            }
        } else {
            System.out.println("The list is empty");
        }
    }


    private void connect(String message) {
        String[] messageArray = message.split(" ");
        if (messageArray != null && messageArray.length == 3) {
            try {
                InetAddress remoteAddress = InetAddress.getByName(messageArray[1]); //obtain destination ip address from console message
                int remotePort = Integer.parseInt(messageArray[2]); //obtain portnum from console message
                System.out.println("Connecting to " + remoteAddress + " on port: " + remotePort);

                Destination connection = new Destination(remoteAddress, remotePort); //Create Destination object using ip address and portnum
                if (connection.initConnections()) { //initialize connection ( new socket and print writer output stream)
                    connections.put(clientCounter, connection); // add connection to the connections list
                    System.out.println("Connected successfully, client id: " + clientCounter++);
                } else {
                    System.out.println("Unable to establish connection, try again");
                }
            } catch (NumberFormatException ne) {
                System.out.println("Invalid Remote Host Port, unable to connect");
            } catch (UnknownHostException e) {
                System.out.println("Invalid Remote Host Address, unable to connect");
            }
        } else {
            //trying to connect  with no/wrong port
            System.out.println("Invalid command, follow : connect <destination> <port no>");
        }
    }


    private void terminate(String message) {
        String[] messageArray = message.split(" ");
        if (messageArray != null) {
            System.out.println("Attempting to terminate Cid: " + messageArray[1]);
            try {
                int id = Integer.parseInt(messageArray[1]);
                if (connections.containsKey(id) == false) { // if the key is not in the list print error
                    System.out.println("Invalid connection ID, unable to terminate, try list");
                    return;
                }    //continue if theres a valid id

                Destination connection = connections.get(id); //get connection from connection list
                boolean closed = !connection.closeConnection(); //close connection, and if successful then print closed message
                if (closed) {
                    System.out.println("ConnectionID: " + id + " was terminated");
                    connections.remove(id); //remove connection from list
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid connection ID, unable to terminate");
            }
        } else {
            System.out.println("Invalid command, follow : terminate <connectionID>");
        }

    }


    private void exit() throws IOException {
        for (Integer id : connections.keySet()) {
            Destination connection = connections.get(id);
            connection.closeConnection();
        }
        connections.clear();
        messageReciever.shutdown();
    }


    private void runChat() throws IOException {
        Scanner scanner = new Scanner(System.in); //Create scanner for user input
        try {
            myIP = InetAddress.getLocalHost(); //get local host ip address
            messageReciever = new Server(); // create new message reciever server on a new thread
            new Thread(messageReciever).start();

            while (true) { //loop for receiving messages
                String message = scanner.nextLine();
                if (message != null && message.trim().length() > 0) {
                    message = message.trim();
                    switch (message) {
                        case "help":
                            help(); // displays options
                            break;
                        case "myip":
                            System.out.println(getMyIP()); // print host (my computer) ip address
                            break;
                        case "myport":
                            System.out.println(getMyPort()); // print listening port
                            break;
                    }
                    if (message.startsWith("connect")) {
                        connect(message);  // run connect method with console message as param
                    } else if (message.equals("list")) {
                        list(); //display list of connections
                    } else if (message.startsWith("terminate")) {
                        terminate(message); // run terminate method with console message as param
                    } else if (message.startsWith("send")) {
                        Send(message); // run Send method with console message as param
                    } else if (message.startsWith("exit")) { //Exit the program
                        System.out.println("Exiting");
                        exit();
                        System.exit(0);
                    }
                } else {
                    System.out.println("Invalid command \n");
                }
            }
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } finally {
            if (scanner != null)
                scanner.close();
            exit();
        }
    }


    public static void main(String[] args) {
        if (args != null && args.length > 0) { //make sure app is run with a port number
            try {
                int portnumber = Integer.parseInt(args[0]);
                ChatApp chatApp = new ChatApp(portnumber); //insert portnumber as param
                chatApp.runChat(); //run ChatApp
            } catch (NumberFormatException e) {
                System.out.println("Invalid number for port");
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("Invalid Argument: run with 'java ChatApp <PORTNUM>");
        }
    }


    public class Client implements Runnable {

        private Socket client = null;
        private BufferedReader in = null;
        private boolean done = false;

        //Client constructor
        private Client(BufferedReader in, Socket ipAddress) {
            this.in = in;
            this.client = ipAddress;
        }


        @Override
        public void run() {
            while (!client.isClosed() && !this.done) {
                String text;
                try {
                    text = in.readLine();
                    if (text == null) {
                        exit();    //the connection was closed.
                        System.out.println("Connection was terminated by: "
                                + client.getInetAddress().getHostAddress()
                                + ":" + client.getPort());
                        return;
                    }

                    System.out.println("Message from "
                            + client.getInetAddress().getHostAddress()
                            + ":" + client.getPort() + " : " + text);

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }


        public void exit() {
            done = true;
            if (in != null)
                try {
                    in.close();
                } catch (IOException e) {
                }

            if (client != null)
                try {
                    client.close();
                } catch (IOException e) {
                }
            Thread.currentThread().interrupt();
        }

    }


    private class Server implements Runnable {

        private List<Client> clientsList = new ArrayList<Client>();
        private Socket socket;
        private boolean done;
        BufferedReader in;


        @Override
        public void run() {
            ServerSocket s;
            try {
                s = new ServerSocket(getMyPort());
                while (!done) {
                    try {
                        socket = s.accept();
                        in = new BufferedReader(new
                                InputStreamReader(socket.getInputStream()));
                        System.out.println(socket.getInetAddress().getHostAddress()
                                + ":" + socket.getPort() + " : client successfully connected.");

                        Client client = new Client(in, socket);
                        new Thread(client).start();
                        clientsList.add(client);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            } catch (Exception e1) {
                System.out.println("An Error has occurred");
            }
        }

        public void shutdown() throws IOException {
            done = true;
            for (Client clients : clientsList) {
                clients.exit();
            }
            Thread.currentThread().interrupt();
        }

    }
    class Destination{

        private InetAddress remoteHost;
        private int remotePort;
        private Socket connection;
        private PrintWriter out;
        private boolean isConnected;

        public Destination(InetAddress remoteHost, int remotePort) { //constructor

            this.remoteHost = remoteHost;
            this.remotePort = remotePort;
        }

        public boolean initConnections(){ // initialize connections ( create new socket and print writer )
            try {
                this.connection = new Socket(remoteHost, remotePort);
                this.out = new PrintWriter(connection.getOutputStream(), true);
                isConnected = true;
            } catch (IOException e) {

            }
            return isConnected;
        }
        public int getRemotePort() {
            return remotePort;
        }

        public InetAddress getRemoteHost() {
            return remoteHost;
        }

        public void Send(String message){ // send message using print writer
            if(isConnected){
                out.println(message);
            }
        }
        public void setRemoteHost(InetAddress remoteHost) {
            this.remoteHost = remoteHost;
        }

        public void setRemotePort(int remotePort) {
            this.remotePort = remotePort;
        }


        public boolean closeConnection(){ // close Connection
            isConnected = false;
            if(out != null)
                out.close();
            if(connection != null){
                try {
                    connection.close();
                } catch (IOException e) {
                }
            }
            return isConnected;
        }
        @Override
        public String toString() {
            return  remoteHost + "\t" + remotePort;
        }
    }
}


