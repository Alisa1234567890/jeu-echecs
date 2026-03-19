package org.model;

import org.model.piece.*;
import org.model.plateau.EchiquierModele;
import org.model.plateau.Plateau;
import org.model.plateau.PlateauSingleton;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Observable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Jeu extends Observable implements Runnable {

    public enum GameMode {
        HUMAN_VS_HUMAN,
        HUMAN_VS_AI
    }

    public enum Difficulty {
        EASY,
        MEDIUM,
        HARD
    }

    private static final long INITIAL_CLOCK_MILLIS = 10L * 60L * 1000L;

    public Coup nextC;

    private Joueur joueur1;
    private Joueur joueur2;
    private Joueur joueurCourant;
    private boolean termine = false;

    private EchiquierModele echiquier;
    private Coup dernierCoup;
    // true uniquement quand le jeu est bloqué dans attendreCoup() - empêche de jouer 2x
    private volatile boolean attenteCoup = false;
    private final GameMode mode;
    private final Difficulty difficulty;
    private long whiteRemainingMillis = INITIAL_CLOCK_MILLIS;
    private long blackRemainingMillis = INITIAL_CLOCK_MILLIS;
    private boolean activeClockWhite = true;
    private long lastClockUpdateMillis = System.currentTimeMillis();
    private String statusMessage = "White to move.";
    private final List<Coup> moveHistory = new ArrayList<>();

    public Jeu() {
        this(GameMode.HUMAN_VS_HUMAN, Difficulty.EASY);
    }

    public Jeu(GameMode mode, Difficulty difficulty) {
        this.mode = mode;
        this.difficulty = difficulty;
        echiquier = new EchiquierModele();
        joueur1 = new JHumain(this, true);   // Joueur blanc
        joueur2 = mode == GameMode.HUMAN_VS_AI
                ? new JIA(this, false, difficulty)
                : new JHumain(this, false);  // Joueur noir
        joueurCourant = joueur1;             // Blanc joue en premier
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
            Coup c = joueurCourant.getCoup();
            if (c == null) {
                if (!joueurCourant.aDesCoupsLegaux()) {
                    termine = true;
                    statusMessage = joueurCourant.estEnEchec() ? "Checkmate." : "Stalemate.";
                    setChanged();
                    notifyObservers(statusMessage);
                }
                continue;
            }

            if (!appliquerCoup(c)) continue;

            Joueur joueurSuivant = (joueurCourant == joueur1) ? joueur2 : joueur1;

            if (estEchecEtMat(joueurSuivant)) {
                String gagnant = joueurCourant.isBlanc() ? "White" : "Black";
                if (nextC != null) nextC.setType("ECHEC ET MAT");
                termine = true;
                statusMessage = "Checkmate. Winner: " + gagnant + ".";
                setChanged();
                notifyObservers(statusMessage);
                break;
            }

            if (estPat(joueurSuivant)) {
                if (nextC != null) nextC.setType("PAT");
                termine = true;
                statusMessage = "Stalemate.";
                setChanged();
                notifyObservers(statusMessage);
                break;
            }

            switchToPlayer(joueurSuivant);
        }
    }

    public boolean partieTerminee() {
        return termine;
    }

    /**
     * Bloque le Jeu-Thread jusqu'à ce que l'utilisateur clique (via setCoup).
     * Le flag attenteCoup est true uniquement pendant ce wait(), ce qui empêche
     * setCoup d'accepter un coup en dehors du bon moment.
     */
    public synchronized Coup attendreCoup() {
        attenteCoup = true;
        try {
            while (nextC == null && !termine) {
                wait();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        if (termine) {
            return null;
        }
        Coup coup = nextC;
        nextC = null;
        attenteCoup = false;
        return coup;
    }

    /**
     * Appelé par la vue quand l'utilisateur clique.
     * Ignoré si le jeu n'est pas en train d'attendre un coup (évite de jouer 2x).
     */
    public void setCoup(Coup c) {
        if (c == null) return;
        synchronized (this) {
            if (!(joueurCourant instanceof JHumain) || termine) {
                return;
            }
            if (nextC != null) {
                return;
            }
            this.nextC = c;
            this.attenteCoup = false;
            this.notifyAll();
        }
    }

    public boolean appliquerCoup(Coup c) {
        if (c == null) return false;
        synchronized (this) {
            applyClockTickLocked();
            nextC = c;

            Plateau plateau = PlateauSingleton.INSTANCE;
            Piece piece = plateau.getCase(c.dep).getPiece();
            Piece originalPiece = piece;

            if (piece == null) {
                return false;
            }

            if (piece.isBlanc() != joueurCourant.isBlanc()) {
                return false;
            }

            // Save en passant target for possible rollback
            org.model.plateau.Case oldEnPassantTarget = plateau.getEnPassantTarget();

            Piece captured = plateau.getCase(c.arr).getPiece();
            Piece capturedEnPassant = null;
            boolean isEnPassant = false;

            // Detect en passant: diagonal pawn move to the current ep-target square (empty)
            org.model.plateau.Case epTarget = plateau.getEnPassantTarget();
            if (piece instanceof Pawn && captured == null && c.dep.y != c.arr.y
                    && epTarget != null
                    && epTarget.getX() == c.arr.x && epTarget.getY() == c.arr.y) {
                int dirEnPassant = piece.isBlanc() ? 1 : -1;
                org.model.plateau.Case capturedCase = plateau.getCase(c.arr.x + dirEnPassant, c.arr.y);
                if (capturedCase != null && capturedCase.getPiece() instanceof Pawn
                        && capturedCase.getPiece().isBlanc() != piece.isBlanc()) {
                    isEnPassant = true;
                    capturedEnPassant = capturedCase.getPiece();
                    c.setType("PRISE EN PASSANT");
                }
            }

            boolean ok = plateau.deplacer(c.dep, c.arr);
            if (!ok) {
                echiquier.syncFromPlateau(plateau);
                setChanged();
                notifyObservers(c);
                return false;
            }

            // Remove the captured pawn (en passant)
            if (isEnPassant) {
                int dirEnPassant = piece.isBlanc() ? 1 : -1;
                plateau.getCase(c.arr.x + dirEnPassant, c.arr.y).setPiece(null);
            }

            boolean isCastling = false;
            if (piece instanceof King) {
                if (Math.abs(c.arr.y - c.dep.y) == 2) {
                    isCastling = true;
                    c.setType("ROQUE");
                    if (c.arr.y > c.dep.y) {
                        Piece rook = plateau.getCase(c.arr.x, 7).getPiece();
                        if (rook instanceof Rook) {
                            plateau.getCase(c.arr.x, 7).setPiece(null);
                            plateau.getCase(c.arr.x, 5).setPiece(rook);
                        }
                    } else {
                        Piece rook = plateau.getCase(c.arr.x, 0).getPiece();
                        if (rook instanceof Rook) {
                            plateau.getCase(c.arr.x, 0).setPiece(null);
                            plateau.getCase(c.arr.x, 3).setPiece(rook);
                        }
                    }
                }
            }

            if (piece instanceof Pawn) {
                int endRow = c.arr.x;
                if ((piece.isBlanc() && endRow == 0) || (!piece.isBlanc() && endRow == 7)) {
                    Piece newQueen = new Queen(piece.getColor());
                    plateau.getCase(c.arr).setPiece(newQueen);
                    c.setType("PROMOTION");
                }
            }

            echiquier.syncFromPlateau(plateau);
            dernierCoup = c;

            // Update en passant target: set after double pawn push, clear otherwise
            if (piece instanceof Pawn && Math.abs(c.arr.x - c.dep.x) == 2) {
                int passedRow = (c.dep.x + c.arr.x) / 2;
                plateau.setEnPassantTarget(plateau.getCase(passedRow, c.arr.y));
            } else {
                plateau.setEnPassantTarget(null);
            }

            if (joueurCourant.estEnEchec()) {
                plateau.getCase(c.dep).setPiece(originalPiece);
                plateau.getCase(c.arr).setPiece(captured);

                if (isEnPassant && capturedEnPassant != null) {
                    int dirEnPassant = piece.isBlanc() ? 1 : -1;
                    plateau.getCase(c.arr.x + dirEnPassant, c.arr.y).setPiece(capturedEnPassant);
                }
                if (isCastling) {
                    if (c.arr.y > c.dep.y) {
                        Piece rook = plateau.getCase(c.arr.x, 5).getPiece();
                        if (rook instanceof Rook) {
                            plateau.getCase(c.arr.x, 5).setPiece(null);
                            plateau.getCase(c.arr.x, 7).setPiece(rook);
                        }
                    } else {
                        Piece rook = plateau.getCase(c.arr.x, 3).getPiece();
                        if (rook instanceof Rook) {
                            plateau.getCase(c.arr.x, 3).setPiece(null);
                            plateau.getCase(c.arr.x, 0).setPiece(rook);
                        }
                    }
                }

                plateau.setEnPassantTarget(oldEnPassantTarget);
                echiquier.syncFromPlateau(plateau);
                setChanged();
                notifyObservers(c);
                return false;
            }

            // Vérifier si l'adversaire est en échec simple
            Joueur adversaire = (joueurCourant == joueur1) ? joueur2 : joueur1;
            if (adversaire.estEnEchec()) {
                c.setType("ECHEC");
            }

            moveHistory.add(new Coup(new Point(c.dep), new Point(c.arr), c.getType()));
            statusMessage = "Move played.";
            setChanged();
            notifyObservers(c);
            notifyAll();
            return true;
        }
    }

    public boolean estEchecEtMat(Joueur joueur) {
        return joueur.estEnEchec() && !joueur.aDesCoupsLegaux();
    }

    public boolean estPat(Joueur joueur) {
        return !joueur.estEnEchec() && !joueur.aDesCoupsLegaux();
    }

    public boolean aGagne(Joueur joueur) {
        Joueur adversaire = (joueur == joueur1) ? joueur2 : joueur1;
        return estEchecEtMat(adversaire);
    }

    public Coup getDernierCoup() {
        return dernierCoup;
    }

    public Joueur getJoueurCourant() {
        return joueurCourant;
    }

    public GameMode getMode() {
        return mode;
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public synchronized boolean estTourHumain() {
        return joueurCourant instanceof JHumain && !termine;
    }

    public synchronized List<Point> getLegalDestinations(int row, int col) {
        List<Point> destinations = new ArrayList<>();
        Piece piece = echiquier.getPiece(row, col);
        if (piece == null || joueurCourant == null || piece.isBlanc() != joueurCourant.isBlanc()) {
            return destinations;
        }
        for (org.model.plateau.Case square : piece.getCaseAccessible()) {
            Point from = new Point(row, col);
            Point to = new Point(square.getX(), square.getY());
            if (isMoveLegalForCurrentPlayer(from, to)) {
                destinations.add(to);
            }
        }
        return destinations;
    }

    public synchronized List<Coup> getLegalMoves(Joueur joueur) {
        List<Coup> legalMoves = new ArrayList<>();
        Joueur savedCurrent = joueurCourant;
        joueurCourant = joueur;
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                Piece piece = echiquier.getPiece(row, col);
                if (piece == null || piece.isBlanc() != joueur.isBlanc()) {
                    continue;
                }
                for (Point target : getLegalDestinations(row, col)) {
                    legalMoves.add(new Coup(new Point(row, col), target));
                }
            }
        }
        joueurCourant = savedCurrent;
        return legalMoves;
    }

    public synchronized boolean isKingInCheck(Joueur joueur) {
        Piece roi = echiquier.getRoi(joueur);
        if (roi == null) {
            return false;
        }
        for (Piece piece : echiquier.getPiecesAdverses(joueur)) {
            if (piece.getCaseAccessible().contains(roi.getCase())) {
                return true;
            }
        }
        return false;
    }

    public synchronized boolean pressClock(boolean whiteClock) {
        return false;
    }

    public synchronized boolean canPressClock(boolean whiteClock) {
        return false;
    }

    public synchronized void tickClock() {
        if (!termine) {
            applyClockTickLocked();
        }
    }

    public synchronized long getRemainingTimeMillis(boolean white) {
        return white ? whiteRemainingMillis : blackRemainingMillis;
    }

    public synchronized boolean isClockActive(boolean white) {
        return !termine && activeClockWhite == white;
    }

    public synchronized String getStatusMessage() {
        return statusMessage;
    }

    public synchronized List<Coup> getMoveHistory() {
        List<Coup> history = new ArrayList<>();
        for (Coup coup : moveHistory) {
            history.add(new Coup(new Point(coup.dep), new Point(coup.arr), coup.getType()));
        }
        return history;
    }

    public synchronized void resetGame() {
        echiquier = new EchiquierModele();
        Plateau plateau = PlateauSingleton.INSTANCE;
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                plateau.getCase(r, c).setPiece(echiquier.getPiece(r, c));
            }
        }
        joueurCourant = joueur1;
        termine = false;
        nextC = null;
        dernierCoup = null;
        attenteCoup = false;
        whiteRemainingMillis = INITIAL_CLOCK_MILLIS;
        blackRemainingMillis = INITIAL_CLOCK_MILLIS;
        activeClockWhite = true;
        lastClockUpdateMillis = System.currentTimeMillis();
        statusMessage = "White to move.";
        moveHistory.clear();
        setChanged();
        notifyObservers("Game reset.");
        notifyAll();
    }

    public synchronized void importPgnMoves(List<String> moves) {
        resetGame();
        for (String move : moves) {
            Coup coup = parseCoordinateMove(move);
            if (coup == null || !appliquerCoup(coup)) {
                throw new IllegalArgumentException("Invalid PGN move: " + move);
            }
            switchToPlayer((joueurCourant == joueur1) ? joueur2 : joueur1);
        }
        attenteCoup = false;
        statusMessage = "PGN imported.";
        setChanged();
        notifyObservers(statusMessage);
        notifyAll();
    }

    public synchronized boolean applyRemoteTurn(Coup coup, boolean whiteClock, long timeoutMillis) {
        long deadline = System.currentTimeMillis() + Math.max(250L, timeoutMillis);
        while (!termine && (joueurCourant == null || joueurCourant.isBlanc() != whiteClock || nextC != null)) {
            long remaining = deadline - System.currentTimeMillis();
            if (remaining <= 0L) {
                return false;
            }
            try {
                wait(remaining);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        if (termine) {
            return false;
        }
        nextC = coup;
        notifyAll();
        while (!termine && (nextC != null || (joueurCourant != null && joueurCourant.isBlanc() == whiteClock))) {
            long remaining = deadline - System.currentTimeMillis();
            if (remaining <= 0L) {
                return false;
            }
            try {
                wait(remaining);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return !termine;
    }

    private boolean isMoveLegalForCurrentPlayer(Point from, Point to) {
        Plateau plateau = PlateauSingleton.INSTANCE;
        Piece moving = plateau.getCase(from).getPiece();
        Piece captured = plateau.getCase(to).getPiece();
        if (moving == null) {
            return false;
        }
        boolean moved = plateau.deplacer(from, to);
        if (!moved) {
            return false;
        }
        echiquier.syncFromPlateau(plateau);
        boolean legal = !joueurCourant.estEnEchec();
        plateau.getCase(from).setPiece(moving);
        plateau.getCase(to).setPiece(captured);
        echiquier.syncFromPlateau(plateau);
        return legal;
    }

    private void switchToPlayer(Joueur nextPlayer) {
        joueurCourant = nextPlayer;
        activeClockWhite = nextPlayer != null && nextPlayer.isBlanc();
        lastClockUpdateMillis = System.currentTimeMillis();
        statusMessage = (activeClockWhite ? "White" : "Black") + " to move.";
        setChanged();
        notifyObservers(statusMessage);
        notifyAll();
    }

    private void applyClockTickLocked() {
        long now = System.currentTimeMillis();
        long elapsed = Math.max(0L, now - lastClockUpdateMillis);
        lastClockUpdateMillis = now;
        if (elapsed == 0L || termine) {
            return;
        }
        if (activeClockWhite) {
            whiteRemainingMillis = Math.max(0L, whiteRemainingMillis - elapsed);
            if (whiteRemainingMillis == 0L) {
                termine = true;
                statusMessage = "White lost on time.";
            }
        } else {
            blackRemainingMillis = Math.max(0L, blackRemainingMillis - elapsed);
            if (blackRemainingMillis == 0L) {
                termine = true;
                statusMessage = "Black lost on time.";
            }
        }
        if (termine) {
            setChanged();
            notifyObservers(statusMessage);
            notifyAll();
        }
    }

    private Coup parseCoordinateMove(String move) {
        String normalized = move.trim().toLowerCase(Locale.ROOT);
        Matcher matcher = Pattern.compile("([a-h][1-8])([a-h][1-8])([qrbn])?").matcher(normalized);
        if (!matcher.matches()) {
            return null;
        }
        Point from = algebraicToPoint(matcher.group(1));
        Point to = algebraicToPoint(matcher.group(2));
        if (from == null || to == null) {
            return null;
        }
        return new Coup(from, to);
    }

    private Point algebraicToPoint(String square) {
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
