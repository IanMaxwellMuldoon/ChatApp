import javax.print.attribute.standard.Destination;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
        System.out.println("Connect");
        System.out.println("list");
        System.out.println("terminate<connection id>");
        System.out.println("send<connection id><message>");
        System.out.println("exit");
    }


    private void Send(String message) {
        String[] messageArray = message.split(" ");
        if (messageArray.length > 2) {
            try {
                int id = Integer.parseInt(messageArray[1]);
                Destination destinationHost = connections.get(id);
                System.out.println("id = " + connections.get(id));
                if (destinationHost != null) {
                    StringBuilder text = new StringBuilder();
                    for (int i = 2; i < messageArray.length; i++) {
                        text.append(messageArray[i]);
                        text.append(" ");
                    }
                    destinationHost.Send(text.toString());
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
                InetAddress remoteAddress = InetAddress.getByName(messageArray[1]);
                int remotePort = Integer.parseInt(messageArray[2]);
                System.out.println("Connecting to " + remoteAddress + " on port: " + remotePort);

                Destination connection = new Destination(remoteAddress, remotePort);
                if (connection.initConnections()) {
                    connections.put(clientCounter, connection);
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
                if (connections.containsKey(id) == false) {
                    System.out.println("Invalid connection ID, unable to terminate, try list");
                    return;
                }    //continue if theres a valid id

                Destination connection = connections.get(id);
                boolean closed = !connection.closeConnection();
                if (closed) {
                    System.out.println("ConnectionID: " + id + " was terminated");
                    connections.remove(id);
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
        Scanner scanner = new Scanner(System.in);
        try {
            myIP = InetAddress.getLocalHost();
            messageReciever = new Server();
            new Thread(messageReciever).start();

            while (true) {
                String message = scanner.nextLine();
                if (message != null && message.trim().length() > 0) {
                    message = message.trim();
                    switch (message) {
                        case "help":
                            help();
                            break;
                        case "myip":
                            System.out.println(getMyIP());
                            break;
                        case "myport":
                            System.out.println(getMyPort());
                            break;
                    }
                    if (message.startsWith("connect")) {
                        connect(message);
                    } else if (message.equals("list")) {
                        list();
                    } else if (message.startsWith("terminate")) {
                        terminate(message);
                    } else if (message.startsWith("send")) {
                        Send(message);
                    } else if (message.startsWith("exit")) {
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
        if (args != null && args.length > 0) {
            try {
                int portnumber = Integer.parseInt(args[0]);
                ChatApp chatApp = new ChatApp(portnumber);
                chatApp.runChat();
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

        public Destination(InetAddress remoteHost, int remotePort) {

            this.remoteHost = remoteHost;
            this.remotePort = remotePort;
        }

        public boolean initConnections(){
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
        public void Send(String message){
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


        public boolean closeConnection(){
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


