package symbiosis.client.net;

import symbiosis.common.net.*;

import java.io.*;
import java.net.Socket;
import java.util.function.Consumer;

public class GameClient {

    private final String host;
    private final int port;
    private final ProtocolEncoder encoder = new ProtocolEncoder();
    private final ProtocolDecoder decoder = new ProtocolDecoder();
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private Thread networkThread;
    private volatile boolean running;

    private Consumer<Message> onMessage;

    public GameClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void setOnMessage(Consumer<Message> onMessage) {
        this.onMessage = onMessage;
    }

    public void connect() throws IOException {
        socket = new Socket(host, port);
        out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        running = true;
        networkThread = new Thread(this::networkLoop, "NetworkThread");
        networkThread.start();
    }

    private void networkLoop() {
        String line;
        try {
            while (running && (line = in.readLine()) != null) {
                try {
                    Message msg = decoder.decode(line);
                    if (onMessage != null) {
                        onMessage.accept(msg);
                    }
                } catch (IllegalArgumentException e) {
                    System.out.println("Decode error: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            if (running) {
                System.out.println("Network error: " + e.getMessage());
            }
        } finally {
            disconnect();
        }
    }

    public void send(Message msg) {
        if (out != null) {
            String line = encoder.encode(msg);
            out.println(line);
        }
    }

    public void disconnect() {
        running = false;
        try {
            if (socket != null) socket.close();
        } catch (IOException ignored) {
        }
    }
}
