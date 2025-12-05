package symbiosis.server;

import symbiosis.common.net.*;

import java.io.*;
import java.net.Socket;

public class ClientHandler implements Runnable {

    private final Socket socket;
    private final GameServer server;
    private final ProtocolEncoder encoder;
    private final ProtocolDecoder decoder;
    private BufferedReader in;
    private PrintWriter out;
    private String clientId;

    public ClientHandler(Socket socket,
                         GameServer server,
                         ProtocolEncoder encoder,
                         ProtocolDecoder decoder) {
        this.socket = socket;
        this.server = server;
        this.encoder = encoder;
        this.decoder = decoder;
    }

    public String getClientId() {
        return clientId;
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);

            String line;
            while ((line = in.readLine()) != null) {
                try {
                    Message msg = decoder.decode(line);
                    handleMessage(msg);
                } catch (IllegalArgumentException e) {
                    send(new ErrorMessage("BAD_MESSAGE", e.getMessage()));
                }
            }
        } catch (IOException e) {
            System.out.println("Client disconnected: " + e.getMessage());
        } finally {
            close();
        }
    }

    private void handleMessage(Message msg) {
        if (msg instanceof JoinMessage join) {
            this.clientId = "C" + System.nanoTime();
            System.out.println("JOIN from " + join.getPlayerName() + " -> clientId=" + clientId);
            server.getGameLogic().handleJoin(this, join);
        } else if (msg instanceof InputMessage input) {
            server.getGameLogic().handleInput(input);
        } else if (msg instanceof ChatMessage chat) {
            server.getGameLogic().handleChat(chat);
        }
    }

    public void send(Message msg) {
        String line = encoder.encode(msg);
        out.println(line);
    }

    private void close() {
        server.getGameLogic();

        server.removeClient(this);
        try {
            socket.close();
        } catch (IOException ignored) {
        }
    }

}
