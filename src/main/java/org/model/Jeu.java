package org.model;

import org.model.piece.Bishop;
import org.model.piece.King;
import org.model.piece.Knight;
import org.model.piece.Pawn;
import org.model.piece.Piece;
import org.model.piece.Queen;
import org.model.piece.Rook;
import org.model.plateau.EchiquierModele;
import org.model.plateau.Plateau;
import org.model.plateau.PlateauSingleton;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

public class Jeu implements Runnable {

    public enum GameMode {
        HUMAN_VS_HUMAN,
        HUMAN_VS_AI
    }

    public enum Difficulty {
        EASY,
        MEDIUM,
        HARD
    }

    private Joueur joueurBlanc;
    private Joueur joueurNoir;
    private boolean termine = false;
    private boolean blancDoitJouer = true;
    private Coup pendingHumanMove;
    private String statusMessage;
    private final GameMode gameMode;
    private final Difficulty difficulty;
    private final List<JeuObserver> observers = new ArrayList<>();

    private final EchiquierModele echiquier;

    public Jeu() {
        this(GameMode.HUMAN_VS_HUMAN, Difficulty.EASY);
    }

    public Jeu(GameMode gameMode, Difficulty difficulty) {
        this.gameMode = gameMode;
        this.difficulty = difficulty;
        echiquier = new EchiquierModele();
        joueurBlanc = new JHumain(this, true, "Blanc");
        joueurNoir = gameMode == GameMode.HUMAN_VS_AI
                ? new JIA(this, false, "IA", difficulty)
                : new JHumain(this, false, "Noir");
        statusMessage = buildTurnMessage();

        Plateau p = PlateauSingleton.INSTANCE;
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece piece = echiquier.getPiece(r, c);
                p.getCase(r, c).setPiece(piece);
            }
        }
        new Thread(this, "Jeu-Thread").start();
    }

    public EchiquierModele getEchiquier() {
        return echiquier;
    }

    @Override
    public void run() {
        jouerPartie();
    }

    public void jouerPartie() {
        while (!partieTerminee()) {
            Joueur joueur = getJoueurSuivant();
            Coup coup = joueur.getCoup();
            if (coup != null) {
                appliquerCoup(coup);
            } else if (!partieTerminee() && getLegalMoves(blancDoitJouer).isEmpty()) {
                terminerPartie("Aucun coup possible pour " + couleurCourante() + ".");
            }
        }
    }

    public boolean partieTerminee() {
        return termine;
    }

    public Joueur getJoueurSuivant() {
        return blancDoitJouer ? joueurBlanc : joueurNoir;
    }

    public synchronized void appliquerCoup(Coup coup) {
        if (coup == null || !isMoveAllowedForCurrentTurn(coup)) {
            statusMessage = "Coup invalide pour " + couleurCourante() + ".";
            notifyObservers(coup);
            return;
        }

        PlateauSingleton.INSTANCE.deplacer(coup.dep, coup.arr);
        echiquier.syncFromPlateau(PlateauSingleton.INSTANCE);
        blancDoitJouer = !blancDoitJouer;
        statusMessage = buildTurnMessage();
        checkGameOver();
        notifyObservers(coup);
    }

    public synchronized void setCoup(Coup coup) {
        if (coup == null) {
            return;
        }
        if (!(getJoueurSuivant() instanceof JHumain)) {
            statusMessage = "Patientez : l'IA joue.";
            notifyObservers(coup);
            return;
        }
        if (!isMoveAllowedForCurrentTurn(coup)) {
            statusMessage = "Coup invalide pour " + couleurCourante() + ".";
            notifyObservers(coup);
            return;
        }
        pendingHumanMove = coup;
        notifyAll();
    }

    public synchronized Coup consumePendingHumanMove(boolean blanc) {
        if (blanc != blancDoitJouer) {
            return null;
        }
        Coup coup = pendingHumanMove;
        pendingHumanMove = null;
        return coup;
    }

    public synchronized boolean isHumanTurn() {
        return getJoueurSuivant() instanceof JHumain;
    }

    public synchronized boolean canSelectPiece(int row, int col) {
        if (!isHumanTurn()) {
            return false;
        }
        Piece piece = echiquier.getPiece(row, col);
        return piece != null && piece.isBlanc() == blancDoitJouer;
    }

    public synchronized boolean isMoveAllowedForCurrentTurn(Coup coup) {
        if (coup == null || coup.dep == null || coup.arr == null) {
            return false;
        }
        Piece piece = echiquier.getPiece(coup.dep.x, coup.dep.y);
        if (piece == null || piece.isBlanc() != blancDoitJouer) {
            return false;
        }
        return PlateauSingleton.INSTANCE.canMove(coup.dep, coup.arr);
    }

    public synchronized List<Coup> getLegalMoves(boolean blanc) {
        List<Coup> coups = new ArrayList<>();
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                Piece piece = echiquier.getPiece(row, col);
                if (piece == null || piece.isBlanc() != blanc) {
                    continue;
                }
                Point dep = new Point(row, col);
                for (int targetRow = 0; targetRow < 8; targetRow++) {
                    for (int targetCol = 0; targetCol < 8; targetCol++) {
                        Point arr = new Point(targetRow, targetCol);
                        if (PlateauSingleton.INSTANCE.canMove(dep, arr)) {
                            coups.add(new Coup(new Point(dep), new Point(arr)));
                        }
                    }
                }
            }
        }
        return coups;
    }

    public synchronized int scoreMove(Coup coup, boolean blanc) {
        int score = 0;
        Piece movingPiece = echiquier.getPiece(coup.dep.x, coup.dep.y);
        Piece capturedPiece = echiquier.getPiece(coup.arr.x, coup.arr.y);
        if (capturedPiece != null && capturedPiece.isBlanc() != blanc) {
            score += getPieceValue(capturedPiece) * 10;
        }
        if (movingPiece instanceof Pawn) {
            score += blanc ? (7 - coup.arr.x) : coup.arr.x;
        }
        score += 3 - Math.abs(3 - coup.arr.x);
        score += 3 - Math.abs(3 - coup.arr.y);
        return score;
    }

    public synchronized int getPieceValueAt(int row, int col) {
        return getPieceValue(echiquier.getPiece(row, col));
    }

    public synchronized String getStatusMessage() {
        return statusMessage;
    }

    public synchronized String getModeLabel() {
        if (gameMode == GameMode.HUMAN_VS_HUMAN) {
            return "Mode: joueur vs joueur";
        }
        return "Mode: joueur vs IA (" + difficulty.name().toLowerCase(java.util.Locale.ROOT) + ")";
    }

    public synchronized boolean isWhiteToMove() {
        return blancDoitJouer;
    }

    public synchronized void addObserver(JeuObserver observer) {
        if (observer != null && !observers.contains(observer)) {
            observers.add(observer);
        }
    }

    public synchronized void removeObserver(JeuObserver observer) {
        observers.remove(observer);
    }

    private void checkGameOver() {
        boolean whiteKingAlive = hasKing(true);
        boolean blackKingAlive = hasKing(false);
        if (!whiteKingAlive || !blackKingAlive) {
            termine = true;
            if (whiteKingAlive && !blackKingAlive) {
                statusMessage = "Victoire des blancs.";
            } else if (!whiteKingAlive && blackKingAlive) {
                statusMessage = "Victoire des noirs.";
            } else {
                statusMessage = "Partie terminee.";
            }
            notifyAll();
            return;
        }

        if (getLegalMoves(blancDoitJouer).isEmpty()) {
            terminerPartie("Aucun coup possible pour " + couleurCourante() + ".");
        }
    }

    private void terminerPartie(String message) {
        termine = true;
        statusMessage = message;
        notifyAll();
    }

    private boolean hasKing(boolean blanc) {
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                Piece piece = echiquier.getPiece(row, col);
                if (piece instanceof King && piece.isBlanc() == blanc) {
                    return true;
                }
            }
        }
        return false;
    }

    private int getPieceValue(Piece piece) {
        if (piece == null) {
            return 0;
        }
        if (piece instanceof Pawn) {
            return 1;
        }
        if (piece instanceof Knight || piece instanceof Bishop) {
            return 3;
        }
        if (piece instanceof Rook) {
            return 5;
        }
        if (piece instanceof Queen) {
            return 9;
        }
        if (piece instanceof King) {
            return 100;
        }
        return 0;
    }

    private String buildTurnMessage() {
        return "Tour des " + couleurCourante() + ".";
    }

    private String couleurCourante() {
        return blancDoitJouer ? "blancs" : "noirs";
    }

    private void notifyObservers(Object arg) {
        List<JeuObserver> snapshot;
        synchronized (this) {
            snapshot = new ArrayList<>(observers);
        }
        for (JeuObserver observer : snapshot) {
            observer.update(arg);
        }
    }
}
