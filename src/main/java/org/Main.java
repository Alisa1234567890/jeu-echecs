package org;

import org.boardgame.checkers.CheckersGame;
import org.boardgame.ui.GridGameFrame;
import org.controller.ChessController;
import org.controller.ConsoleView;
import org.controller.MF;
import org.controller.VC;
import org.model.Jeu;
import org.network.ChessNetworkConnector;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import java.io.IOException;
import javax.swing.JOptionPane;

public class Main {
    public static void main(String[] args) {
        if (choisirJeu().equals("Checkers")) {
            GridGameFrame frame = new GridGameFrame(new CheckersGame());
            frame.setVisible(true);
            return;
        }

        ChessStartup startup = choisirMode();
        Jeu.GameMode mode = startup.networkMode ? Jeu.GameMode.HUMAN_VS_HUMAN : startup.mode;
        Jeu.Difficulty difficulty = mode == Jeu.GameMode.HUMAN_VS_AI
                ? choisirDifficulte()
                : Jeu.Difficulty.EASY;

        Jeu j = new Jeu(mode, difficulty);
        ChessController controller = new ChessController(j);
        MF mf = new MF(controller);
        mf.setJeu(j);
        j.addObserver(mf);
        VC vue = new VC(j, controller);
        mf.setVC(vue);
        j.addObserver(vue);
        if (activerVueConsole()) {
            ConsoleView consoleView = new ConsoleView(j, controller);
            j.addObserver(consoleView);
            consoleView.start();
        }
        if (startup.networkMode) {
            ChessNetworkConnector connector = connectNetwork(controller, startup);
            if (connector == null) {
                System.exit(0);
                return;
            }
            controller.configureNetwork(connector, connector.isLocalWhite());
        }
        mf.setVisible(true);
    }

    private static ChessNetworkConnector connectNetwork(ChessController controller, ChessStartup startup) {
        try {
            if (startup.hostMode) {
                JOptionPane.showMessageDialog(
                        null,
                        "Waiting for a client on port " + startup.port + ".\nHost plays White.",
                        "Network Host",
                        JOptionPane.INFORMATION_MESSAGE
                );
                return ChessNetworkConnector.host(controller, startup.port);
            }
            return ChessNetworkConnector.join(controller, startup.host, startup.port);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(
                    null,
                    "Network connection failed:\n" + e.getMessage(),
                    "Network Error",
                    JOptionPane.ERROR_MESSAGE
            );
            return null;
        }
    }

    private static String choisirJeu() {
        Object[] options = {"Chess", "Checkers"};
        int choice = JOptionPane.showOptionDialog(
                null,
                "Choose a board game",
                "Game",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]
        );
        return choice == 1 ? "Checkers" : "Chess";
    }

    private static ChessStartup choisirMode() {
        Object[] options = {"Play with a friend", "Play against AI", "Host network game", "Join network game"};
        int choice = JOptionPane.showOptionDialog(
                null,
                "Choose game mode",
                "Game Mode",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]
        );
        if (choice == 2) {
            return new ChessStartup(Jeu.GameMode.HUMAN_VS_HUMAN, true, true, null, askPort());
        }
        if (choice == 3) {
            return new ChessStartup(Jeu.GameMode.HUMAN_VS_HUMAN, true, false, askHost(), askPort());
        }
        if (choice == 1) {
            return new ChessStartup(Jeu.GameMode.HUMAN_VS_AI, false, false, null, 0);
        }
        return new ChessStartup(Jeu.GameMode.HUMAN_VS_HUMAN, false, false, null, 0);
    }

    private static Jeu.Difficulty choisirDifficulte() {
        Object[] options = {"Easy", "Medium", "Hard"};
        int choice = JOptionPane.showOptionDialog(
                null,
                "Choose AI difficulty",
                "AI Difficulty",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[1]
        );
        if (choice == 2) {
            return Jeu.Difficulty.HARD;
        }
        if (choice == 1) {
            return Jeu.Difficulty.MEDIUM;
        }
        return Jeu.Difficulty.EASY;
    }

    private static boolean activerVueConsole() {
        int choice = JOptionPane.showConfirmDialog(
                null,
                "Enable console view too?",
                "Console View",
                JOptionPane.YES_NO_OPTION
        );
        return choice == JOptionPane.YES_OPTION;
    }

    private static String askHost() {
        String host = JOptionPane.showInputDialog(null, "Server host", "localhost");
        return (host == null || host.isBlank()) ? "localhost" : host.trim();
    }

    private static int askPort() {
        String value = JOptionPane.showInputDialog(null, "TCP port", "5000");
        if (value == null || value.isBlank()) {
            return 5000;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return 5000;
        }
    }

    private static class ChessStartup {
        private final Jeu.GameMode mode;
        private final boolean networkMode;
        private final boolean hostMode;
        private final String host;
        private final int port;

        private ChessStartup(Jeu.GameMode mode, boolean networkMode, boolean hostMode, String host, int port) {
            this.mode = mode;
            this.networkMode = networkMode;
            this.hostMode = hostMode;
            this.host = host;
            this.port = port;
        }
    }
}
