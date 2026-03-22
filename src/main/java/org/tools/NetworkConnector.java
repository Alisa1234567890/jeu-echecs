package org.tools;
import java.io.*;
import java.net.Socket;
import java.util.function.Consumer;
/**
 * Connecteur TCP generique vers un serveur d'echecs ou un moteur UCI/XBoard.
 * Utiliser connect() pour un serveur TCP, connectToProcess() pour un moteur local (ex. Stockfish).
 */
public class NetworkConnector {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private Thread readerThread;
    public void connect(String host, int port, Consumer<String> onLine) throws IOException {
        socket = new Socket(host, port);
        out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
        in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        readerThread = new Thread(() -> {
            try {
                String line;
                while ((line = in.readLine()) != null) onLine.accept(line);
            } catch (IOException ignored) {}
        }, "NetConnector-Reader");
        readerThread.setDaemon(true);
        readerThread.start();
    }
    public void connectToProcess(String processPath, Consumer<String> onLine) throws IOException {
        ProcessBuilder pb = new ProcessBuilder(processPath);
        pb.redirectErrorStream(true);
        Process process = pb.start();
        out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(process.getOutputStream())), true);
        in  = new BufferedReader(new InputStreamReader(process.getInputStream()));
        readerThread = new Thread(() -> {
            try {
                String line;
                while ((line = in.readLine()) != null) onLine.accept(line);
            } catch (IOException ignored) {}
        }, "NetConnector-ProcessReader");
        readerThread.setDaemon(true);
        readerThread.start();
    }
    public void sendLine(String line) {
        if (out != null) out.println(line);
    }
    public boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }
    public void close() {
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
        if (readerThread != null) readerThread.interrupt();
    }
}
