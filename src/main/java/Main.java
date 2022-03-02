public class Main {
    public static void main(String[] args) {
        Server server = new Server();
        Thread s = new Thread(server);
        Client client = new Client();
        Thread c = new Thread(client);
        c.start();
        s.start();
    }
}
