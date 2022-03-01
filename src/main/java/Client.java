import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class Client implements Runnable{

    private Socket client;
    private BufferedReader in;
    private PrintWriter out;
    private boolean done;
    private String ip_address = "127.0.0.1";
    private int portnum = 9999;


    @Override
    public void run() {
        try {
            client = new Socket("127.0.0.1",9999);
            out = new PrintWriter(client.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(client.getInputStream()));

            InputHandler inHandler = new InputHandler();
            Thread t = new Thread(inHandler);
            t.start();

            String inMessage;
            while((inMessage = in.readLine()) != null){
                System.out.println(inMessage);
            }
        }catch (Exception e){

        }
    }

    public void setPortnum(int portnum) {
        this.portnum = portnum;
    }

    public void setIp_address(String ip_address) {
        this.ip_address = ip_address;
    }

    public void runClientConnection(){
        try {
            client = new Socket(ip_address, portnum);
            out = new PrintWriter(client.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(client.getInputStream()));

            InputHandler inHandler = new InputHandler();
            Thread t = new Thread(inHandler);
            t.start();

            String inMessage;
            while((inMessage = in.readLine()) != null){
                System.out.println(inMessage);
            }
        }catch (Exception e){

        }
    }

    class InputHandler implements Runnable{

        @Override
        public void run() {
            try {
                BufferedReader inReader = new BufferedReader(new InputStreamReader(System.in));
                while(!done){
                    String message = inReader.readLine();
                    if(message.equals("/quit")){
                        inReader.close();
                        shutdown();
                    }else{
                        out.println(message);
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
