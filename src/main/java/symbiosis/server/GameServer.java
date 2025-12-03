package symbiosis.server;

import symbiosis.common.net.ProtocolDecoder;
import symbiosis.common.net.ProtocolEncoder;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class GameServer {

    private final int port;
    private final List<ClientHandler> clients = new ArrayList<>();
    private final ProtocolEncoder encoder = new ProtocolEncoder();
    private final ProtocolDecoder decoder = new ProtocolDecoder();
    private volatile boolean running = false;
    private final ServerGameLogic gameLogic;

    public GameServer(int port) {
        this.port = port;
        this.gameLogic = new ServerGameLogic(this);
    }

    public void start() {
        running = true;
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("GameServer listening on port " + port);
            while (running) {
                Socket socket = serverSocket.accept();
                System.out.println("Client connected: " + socket);
                ClientHandler handler = new ClientHandler(socket, this, encoder, decoder);
                synchronized (clients) {
                    clients.add(handler);
                }
                new Thread(handler, "ClientHandler-" + socket.getPort()).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void removeClient(ClientHandler handler) {
        synchronized (clients) {
            clients.remove(handler);
        }
    }

    public List<ClientHandler> getClients() {
        return clients;
    }

    public ServerGameLogic getGameLogic() {
        return gameLogic;
    }
}
