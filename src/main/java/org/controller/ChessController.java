package org.controller;

import org.model.Coup;
import org.model.Jeu;
import org.model.piece.Piece;
import org.network.ChessNetworkConnector;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

public class ChessController {

    private final Jeu jeu;
    private ChessNetworkConnector networkConnector;
    private boolean networkGame;
    private boolean localWhite = true;

    public ChessController(Jeu jeu) {
        this.jeu = jeu;
    }

    public boolean isHumanTurn() {
        return jeu.estTourHumain() && (!networkGame || isWhiteToMove() == localWhite);
    }

    public boolean canSelectPiece(int row, int col) {
        Piece piece = jeu.getEchiquier().getPiece(row, col);
        return piece != null
                && jeu.getJoueurCourant() != null
                && piece.isBlanc() == jeu.getJoueurCourant().isBlanc()
                && (!networkGame || piece.isBlanc() == localWhite)
                && jeu.estTourHumain();
    }

    public List<Point> getLegalDestinations(int row, int col) {
        return jeu.getLegalDestinations(row, col);
    }

    public void submitMove(Point source, Point target) {
        jeu.setCoup(new Coup(source, target));
    }

    public boolean pressClock(boolean white) {
        boolean pressed = jeu.pressClock(white);
        if (pressed && networkGame && networkConnector != null && white == localWhite) {
            List<Coup> history = jeu.getMoveHistory();
            if (!history.isEmpty()) {
                networkConnector.sendTurn(history.get(history.size() - 1));
            }
        }
        return pressed;
    }

    public long getRemainingTimeMillis(boolean white) {
        return jeu.getRemainingTimeMillis(white);
    }

    public boolean isClockActive(boolean white) {
        return jeu.isClockActive(white);
    }

    public boolean canPressClock(boolean white) {
        return jeu.canPressClock(white) && (!networkGame || white == localWhite);
    }

    public String getStatusMessage() {
        return jeu.getStatusMessage();
    }

    public Jeu.GameMode getMode() {
        return jeu.getMode();
    }

    public Jeu.Difficulty getDifficulty() {
        return jeu.getDifficulty();
    }

    public Piece getPiece(int row, int col) {
        return jeu.getEchiquier().getPiece(row, col);
    }

    public boolean isWhiteToMove() {
        return jeu.getJoueurCourant() != null && jeu.getJoueurCourant().isBlanc();
    }

    public String exportPgn() {
        StringBuilder sb = new StringBuilder();
        sb.append("[Event \"POO Chess\"]\n");
        sb.append("[White \"White\"]\n");
        sb.append("[Black \"Black\"]\n");
        sb.append("[Result \"*\"]\n\n");

        List<Coup> history = jeu.getMoveHistory();
        for (int i = 0; i < history.size(); i++) {
            if (i % 2 == 0) {
                sb.append((i / 2) + 1).append(". ");
            }
            sb.append(formatMove(history.get(i))).append(' ');
        }
        sb.append('*');
        return sb.toString().trim();
    }

    public void importPgn(List<String> moves) {
        jeu.importPgnMoves(new ArrayList<>(moves));
    }

    public void resetGame() {
        jeu.resetGame();
        if (networkGame && networkConnector != null) {
            networkConnector.sendReset();
        }
    }

    public void resetGameFromRemote() {
        jeu.resetGame();
    }

    public void configureNetwork(ChessNetworkConnector connector, boolean localWhite) {
        this.networkConnector = connector;
        this.networkGame = connector != null;
        this.localWhite = localWhite;
    }

    public boolean isNetworkGame() {
        return networkGame;
    }

    public boolean isLocalWhite() {
        return localWhite;
    }

    public String getConnectionStatus() {
        if (!networkGame || networkConnector == null) {
            return "Offline";
        }
        return networkConnector.getStatus();
    }

    public void receiveRemoteTurn(Coup coup) {
        if (coup == null) {
            return;
        }
        boolean remoteWhite = !localWhite;
        networkConnector.runAsync(() -> jeu.applyRemoteTurn(coup, remoteWhite, 5000L));
    }

    public static String formatCoordinateMove(Coup coup) {
        return toAlgebraic(coup.dep) + toAlgebraic(coup.arr);
    }

    public static Coup parseCoordinateMove(String text) {
        if (text == null || text.length() < 4) {
            return null;
        }
        Point from = fromAlgebraic(text.substring(0, 2));
        Point to = fromAlgebraic(text.substring(2, 4));
        if (from == null || to == null) {
            return null;
        }
        return new Coup(from, to);
    }

    private String formatMove(Coup coup) {
        return formatCoordinateMove(coup);
    }

    private static String toAlgebraic(Point point) {
        char file = (char) ('a' + point.y);
        char rank = (char) ('8' - point.x);
        return "" + file + rank;
    }

    private static Point fromAlgebraic(String square) {
        if (square == null || square.length() != 2) {
            return null;
        }
        int col = square.charAt(0) - 'a';
        int row = 8 - (square.charAt(1) - '0');
        if (row < 0 || row > 7 || col < 0 || col > 7) {
            return null;
        }
        return new Point(row, col);
    }
}
