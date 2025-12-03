package symbiosis.server;

public class ServerMain {
    public static void main(String[] args) {
        int port = 5555;
        GameServer server = new GameServer(port);
        server.start();
    }
}
