import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;

public class Client implements Runnable{

    private Socket client;
    private BufferedReader in;
    private PrintWriter out;
    private boolean done;


    @Override
    public void run() {
        try {
            Socket client = new Socket("127.0.0.1", 9999);
            out = new PrintWriter(client.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(client.getInputStream()));

            InputHandler inHandler = new InputHandler(client);
            Thread t = new Thread(inHandler);
            t.start();

            String inMessage;
            while((inMessage = in.readLine()) != null){
                System.out.println(inMessage);
            }
        }catch (IOException e){

        }
    }
    class InputHandler implements Runnable{
        private Socket client = null;
        InputHandler(Socket client){
            this.client = client;
        }

        @Override
        public void run() {
            try {
                BufferedReader inReader = new BufferedReader(new InputStreamReader(System.in));
                while(!done){
                    String message = inReader.readLine();
                    if(message.equals("help")) {
                        System.out.println ("You can select any of these commands:");
                        System.out.println ("help");
                        System.out.println ("myip");
                        System.out.println ("myport");
                        System.out.println ("Connect");
                        System.out.println ("list");
                        System.out.println ("terminate<connection id>");
                        System.out.println ("send<connection id><message>");
                        System.out.println ("exit");

                    }
                    else if(message.equals("myip")){// if not connected do nothing.
                        //we may want to do this in server side
                        System.out.println ("Your IP address : "+ InetAddress.getLocalHost().getHostAddress());
                        //System.out.println ("Your IP address : "+ this.client.getInetAddress().toString());
                    }
                    else if(message.equals("myport")){
                        // we may want to do this in server side
                        System.out.println ("Your port : "+ this.client.getLocalPort());
                    }
                    else if(message.startsWith("connect")){
                        String[] temp = message.split(" ");
                        for (int i = 1; i < temp.length; i++){
                            System.out.println ("" + temp[i]);
                        }
                        try {
                            //this might work
                            client = new Socket(InetAddress.getByName( temp[1]),Integer.parseInt(temp[2]));
                            out = new PrintWriter(client.getOutputStream(), true);
                            in = new BufferedReader(new InputStreamReader(client.getInputStream()));

                            InputHandler inHandler = new InputHandler(client);
                            Thread t = new Thread(inHandler);
                            t.start();
                            t.stop();

                        }catch (IOException e){

                        }

                        //TODO:
                    }
                    else if(message.equals("quit")){
                        inReader.close();
                        shutdown();
                    }else{
                        out.println(message);// TODO: this should be removed as we don't want this to output the message
                        //I dont think this commented code below is important, but could be useful.
                        //this.client.getOutputStream().write(message.getBytes());//set message to byte array and send to server
                    }

                }
            }catch (IOException e){
                shutdown();
            }
        }
    }

    private void shutdown() {
        done = true;
        try{
            in.close();
            out.close();
            if(!client.isClosed()){
                client.close();
            }
        }catch (Exception e){

        }
    }


    public static void main(String[] args) {
        Server server = new Server();
        Thread s = new Thread(server);
        s.start();
        Client client = new Client();
        client.run();
    }
}
