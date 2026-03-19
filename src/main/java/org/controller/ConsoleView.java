package org.controller;

import org.model.Coup;
import org.model.Jeu;
import org.model.piece.Bishop;
import org.model.piece.King;
import org.model.piece.Knight;
import org.model.piece.Pawn;
import org.model.piece.Piece;
import org.model.piece.Queen;
import org.model.piece.Rook;

import java.awt.Point;
import java.util.Observable;
import java.util.Observer;
import java.util.Scanner;

public class ConsoleView implements Observer, Runnable {

    private final Jeu jeu;
    private final ChessController controller;
    private final Scanner scanner = new Scanner(System.in);

    public ConsoleView(Jeu jeu, ChessController controller) {
        this.jeu = jeu;
        this.controller = controller;
    }

    public void start() {
        Thread thread = new Thread(this, "ConsoleView-Thread");
        thread.setDaemon(true);
        thread.start();
        printWelcome();
        printBoard();
    }

    @Override
    public void run() {
        while (!jeu.partieTerminee()) {
            try {
                if (!scanner.hasNextLine()) {
                    return;
                }
                String line = scanner.nextLine().trim();
                if (line.isEmpty()) {
                    continue;
                }
                handleCommand(line);
            } catch (Exception ex) {
                System.out.println("Console error: " + ex.getMessage());
            }
        }
    }

    @Override
    public void update(Observable o, Object arg) {
        printBoard();
        System.out.println(jeu.getStatusMessage());
        printPrompt();
    }

    private void handleCommand(String line) {
        String command = line.trim().toLowerCase();

        if ("help".equals(command)) {
            printHelp();
            return;
        }
        if ("board".equals(command)) {
            printBoard();
            printPrompt();
            return;
        }
        if ("clock".equals(command)) {
            boolean ok = controller.pressClock(controller.isWhiteToMove());
            if (!ok) {
                System.out.println("Clock press rejected.");
            }
            printPrompt();
            return;
        }
        if ("quit".equals(command) || "exit".equals(command)) {
            System.exit(0);
            return;
        }

        Coup coup = parseMove(command);
        if (coup == null) {
            System.out.println("Invalid command. Use e2e4, board, clock, help, quit.");
            printPrompt();
            return;
        }

        jeu.setCoup(coup);
        printPrompt();
    }

    private Coup parseMove(String text) {
        String compact = text.replace(" ", "");
        if (compact.length() != 4) {
            return null;
        }

        Point from = parseSquare(compact.substring(0, 2));
        Point to = parseSquare(compact.substring(2, 4));
        if (from == null || to == null) {
            return null;
        }
        return new Coup(from, to);
    }

    private Point parseSquare(String square) {
        if (square.length() != 2) {
            return null;
        }
        char file = square.charAt(0);
        char rank = square.charAt(1);
        if (file < 'a' || file > 'h' || rank < '1' || rank > '8') {
            return null;
        }
        int col = file - 'a';
        int row = 8 - (rank - '0');
        return new Point(row, col);
    }

    private void printWelcome() {
        System.out.println("Console view enabled.");
        printHelp();
        printPrompt();
    }

    private void printHelp() {
        System.out.println("Commands:");
        System.out.println("  e2e4   play a move");
        System.out.println("  board  print the board");
        System.out.println("  clock  press the active player's clock");
        System.out.println("  help   show commands");
        System.out.println("  quit   exit");
    }

    private void printPrompt() {
        String side = controller.isWhiteToMove() ? "white" : "black";
        System.out.print("[" + side + "]> ");
    }

    private void printBoard() {
        System.out.println();
        for (int row = 0; row < 8; row++) {
            System.out.print(8 - row + " ");
            for (int col = 0; col < 8; col++) {
                Piece piece = controller.getPiece(row, col);
                System.out.print(symbolFor(piece) + " ");
            }
            System.out.println();
        }
        System.out.println("  a b c d e f g h");
        System.out.println("White: " + formatMillis(controller.getRemainingTimeMillis(true))
                + " | Black: " + formatMillis(controller.getRemainingTimeMillis(false)));
    }

    private char symbolFor(Piece piece) {
        if (piece == null) {
            return '.';
        }
        char symbol;
        if (piece instanceof Pawn) symbol = 'p';
        else if (piece instanceof Rook) symbol = 'r';
        else if (piece instanceof Knight) symbol = 'n';
        else if (piece instanceof Bishop) symbol = 'b';
        else if (piece instanceof Queen) symbol = 'q';
        else if (piece instanceof King) symbol = 'k';
        else symbol = '?';
        return piece.isBlanc() ? Character.toUpperCase(symbol) : symbol;
    }

    private String formatMillis(long millis) {
        long totalSeconds = Math.max(0L, millis / 1000L);
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;
        return String.format("%02d:%02d", minutes, seconds);
    }
}
