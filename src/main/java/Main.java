public class Main {
    public static int portnumber;

    public static void main(String[] args) {
        //portnumber = Integer.parseInt(args[0]);
        Server server = new Server();
        Thread s = new Thread(server);
        Client client = new Client();
        Thread c = new Thread(client);
        c.start();
        s.start();
    }
}
