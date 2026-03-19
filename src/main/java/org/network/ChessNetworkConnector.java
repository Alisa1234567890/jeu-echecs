package org.network;

import org.controller.ChessController;
import org.model.Coup;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class ChessNetworkConnector implements Closeable {

    private final ChessController controller;
    private final Socket socket;
    private final PrintWriter writer;
    private final BufferedReader reader;
    private final boolean localWhite;
    private volatile String status;
    private volatile boolean running = true;

    public ChessNetworkConnector(ChessController controller, Socket socket, boolean localWhite, String status) throws IOException {
        this.controller = controller;
        this.socket = socket;
        this.localWhite = localWhite;
        this.status = status;
        this.writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
        this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        startReaderThread();
    }

    public static ChessNetworkConnector host(ChessController controller, int port) throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            Socket socket = serverSocket.accept();
            return new ChessNetworkConnector(controller, socket, true, "Connected as host on port " + port);
        }
    }

    public static ChessNetworkConnector join(ChessController controller, String host, int port) throws IOException {
        Socket socket = new Socket(host, port);
        return new ChessNetworkConnector(controller, socket, false, "Connected to " + host + ":" + port);
    }

    public boolean isLocalWhite() {
        return localWhite;
    }

    public String getStatus() {
        return status;
    }

    public void sendTurn(Coup coup) {
        if (coup == null) {
            return;
        }
        sendLine("TURN " + ChessController.formatCoordinateMove(coup));
    }

    public void sendReset() {
        sendLine("RESET");
    }

    public void runAsync(Runnable task) {
        Thread thread = new Thread(task, "Network-Apply-Turn");
        thread.setDaemon(true);
        thread.start();
    }

    private void startReaderThread() {
        Thread thread = new Thread(() -> {
            try {
                String line;
                while (running && (line = reader.readLine()) != null) {
                    handleLine(line.trim());
                }
                if (running) {
                    status = "Disconnected";
                }
            } catch (IOException e) {
                if (running) {
                    status = "Network error: " + e.getMessage();
                }
            } finally {
                running = false;
                try {
                    socket.close();
                } catch (IOException ignored) {
                }
            }
        }, "Chess-Network-Reader");
        thread.setDaemon(true);
        thread.start();
    }

    private void handleLine(String line) {
        if (line.startsWith("TURN ")) {
            Coup coup = ChessController.parseCoordinateMove(line.substring(5).trim());
            controller.receiveRemoteTurn(coup);
            return;
        }
        if ("RESET".equals(line)) {
            controller.resetGameFromRemote();
        }
    }

    private synchronized void sendLine(String line) {
        if (!running) {
            return;
        }
        writer.println(line);
        if (writer.checkError()) {
            status = "Network error while sending";
        }
    }

    @Override
    public void close() throws IOException {
        running = false;
        socket.close();
    }
}
