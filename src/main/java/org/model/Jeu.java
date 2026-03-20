package org.model;

import org.model.piece.Bishop;
import org.model.piece.King;
import org.model.piece.Knight;
import org.model.piece.Pawn;
import org.model.piece.Piece;
import org.model.piece.Queen;
import org.model.piece.Rook;
import org.model.plateau.Case;
import org.model.plateau.EchiquierModele;
import org.model.plateau.Plateau;
import org.model.plateau.PlateauSingleton;

import javax.swing.JOptionPane;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Jeu implements Runnable {
    public enum GameMode { HUMAN_VS_HUMAN, HUMAN_VS_AI }
    public enum Difficulty { EASY, MEDIUM, HARD }
    private static final int TOTAL_NON_KING_MATERIAL = 39;
    private static final long INITIAL_TIME_MILLIS = 10L * 60L * 1000L;

    private final EchiquierModele echiquier;
    private final Joueur joueur1;
    private final Joueur joueur2;
    private final GameMode mode;
    private final Difficulty difficulty;
    private final List<JeuObserver> observers = new CopyOnWriteArrayList<>();
    private final List<Coup> moveHistory = new ArrayList<>();

    private Joueur joueurCourant;
    private boolean termine;
    private Coup dernierCoup;
    private Coup pendingHumanMove;
    private String statusMessage;
    private String winnerLabel;
    private boolean drawGame;
    private long whiteRemainingMillis;
    private long blackRemainingMillis;
    private long lastClockUpdateMillis;

    private boolean whiteKingMoved;
    private boolean blackKingMoved;
    private boolean whiteQueenRookMoved;
    private boolean whiteKingRookMoved;
    private boolean blackQueenRookMoved;
    private boolean blackKingRookMoved;
    private Point enPassantTarget;

    private static final class MoveState {
        private final Coup coup;
        private final Piece movedPiece;
        private final Piece pieceOnDestination;
        private final Piece capturedPiece;
        private final Point capturedSquare;
        private final Piece rookPiece;
        private final Point rookFrom;
        private final Point rookTo;
        private final Point previousEnPassant;
        private final Coup previousDernierCoup;
        private final boolean previousWhiteKingMoved;
        private final boolean previousBlackKingMoved;
        private final boolean previousWhiteQueenRookMoved;
        private final boolean previousWhiteKingRookMoved;
        private final boolean previousBlackQueenRookMoved;
        private final boolean previousBlackKingRookMoved;

        private MoveState(
                Coup coup,
                Piece movedPiece,
                Piece pieceOnDestination,
                Piece capturedPiece,
                Point capturedSquare,
                Piece rookPiece,
                Point rookFrom,
                Point rookTo,
                Point previousEnPassant,
                Coup previousDernierCoup,
                boolean previousWhiteKingMoved,
                boolean previousBlackKingMoved,
                boolean previousWhiteQueenRookMoved,
                boolean previousWhiteKingRookMoved,
                boolean previousBlackQueenRookMoved,
                boolean previousBlackKingRookMoved
        ) {
            this.coup = coup;
            this.movedPiece = movedPiece;
            this.pieceOnDestination = pieceOnDestination;
            this.capturedPiece = capturedPiece;
            this.capturedSquare = capturedSquare;
            this.rookPiece = rookPiece;
            this.rookFrom = rookFrom;
            this.rookTo = rookTo;
            this.previousEnPassant = previousEnPassant;
            this.previousDernierCoup = previousDernierCoup;
            this.previousWhiteKingMoved = previousWhiteKingMoved;
            this.previousBlackKingMoved = previousBlackKingMoved;
            this.previousWhiteQueenRookMoved = previousWhiteQueenRookMoved;
            this.previousWhiteKingRookMoved = previousWhiteKingRookMoved;
            this.previousBlackQueenRookMoved = previousBlackQueenRookMoved;
            this.previousBlackKingRookMoved = previousBlackKingRookMoved;
        }
    }

    public Jeu() {
        this(GameMode.HUMAN_VS_HUMAN, Difficulty.EASY);
    }

    public Jeu(GameMode mode, Difficulty difficulty) {
        this.mode = mode;
        this.difficulty = difficulty;
        this.echiquier = new EchiquierModele();
        this.joueur1 = new JHumain(this, true, "White");
        this.joueur2 = mode == GameMode.HUMAN_VS_AI
                ? new JIA(this, false, "Black AI", difficulty)
                : new JHumain(this, false, "Black");
        resetGame();
        Thread thread = new Thread(this, "jeu-thread");
        thread.setDaemon(true);
        thread.start();
    }

    @Override
    public void run() {
        jouerPartie();
    }

    public void jouerPartie() {
        while (!partieTerminee()) {
            Joueur joueurSuivant;
            synchronized (this) {
                tickClock();
                joueurSuivant = getJoueurSuivant();
            }
            if (joueurSuivant == null) {
                return;
            }

            Coup coup = joueurSuivant.getCoup();
            if (coup == null) {
                synchronized (this) {
                    if (termine) {
                        return;
                    }
                }
                continue;
            }

            appliquerCoup(coup);
        }
    }

    public synchronized void addObserver(JeuObserver observer) {
        if (observer != null && !observers.contains(observer)) {
            observers.add(observer);
        }
    }

    public synchronized void removeObserver(JeuObserver observer) {
        observers.remove(observer);
    }

    public EchiquierModele getEchiquier() {
        return echiquier;
    }

    public synchronized boolean partieTerminee() {
        return termine;
    }

    public synchronized Joueur getJoueurCourant() {
        return joueurCourant;
    }

    public synchronized Coup getDernierCoup() {
        return dernierCoup == null ? null : copyOf(dernierCoup);
    }

    public GameMode getMode() {
        return mode;
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public synchronized String getStatusMessage() {
        return statusMessage;
    }

    public synchronized String getWinnerLabel() {
        return winnerLabel;
    }

    public synchronized boolean isDraw() {
        return drawGame;
    }

    public synchronized long getRemainingTimeMillis(boolean white) {
        return white ? whiteRemainingMillis : blackRemainingMillis;
    }

    public String getModeLabel() {
        return mode == GameMode.HUMAN_VS_AI
                ? "Mode: IA (" + difficulty.name() + ")"
                : "Mode: 2 joueurs";
    }

    public synchronized boolean estTourHumain() {
        return !termine && joueurCourant instanceof JHumain;
    }

    public synchronized boolean canSelectPiece(int row, int col) {
        if (termine || !(joueurCourant instanceof JHumain) || !inside(row, col)) {
            return false;
        }
        Piece piece = getPieceAt(row, col);
        return piece != null && piece.isBlanc() == joueurCourant.isBlanc();
    }

    public synchronized boolean isMoveAllowedForCurrentTurn(Coup coup) {
        return !termine
                && joueurCourant instanceof JHumain
                && isLegalMove(coup, joueurCourant.isBlanc());
    }

    public synchronized Coup attendreCoup() {
        return attendreCoup(joueurCourant != null && joueurCourant.isBlanc());
    }

    public synchronized Coup attendreCoup(boolean blanc) {
        while (!termine) {
            boolean humanTurn = joueurCourant instanceof JHumain && joueurCourant.isBlanc() == blanc;
            if (humanTurn && pendingHumanMove != null) {
                Coup coup = pendingHumanMove;
                pendingHumanMove = null;
                return coup;
            }
            try {
                wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
        }
        return null;
    }

    public synchronized void setCoup(Coup coup) {
        if (coup == null || termine || !(joueurCourant instanceof JHumain)) {
            return;
        }
        if (!isLegalMove(coup, joueurCourant.isBlanc())) {
            return;
        }
        pendingHumanMove = copyOf(coup);
        notifyAll();
    }

    public synchronized boolean appliquerCoup(Coup coup) {
        if (coup == null || termine || joueurCourant == null) {
            return false;
        }
        tickClock();
        if (!coupValide(coup)) {
            return false;
        }

        Piece movingPiece = getPieceAt(coup.dep.x, coup.dep.y);
        String promotionChoice = getPromotionChoice(movingPiece, coup.arr.x, joueurCourant);
        Coup applied = copyOf(coup);
        MoveState state = applyUnchecked(applied, promotionChoice);
        if (state == null) {
            return false;
        }

        dernierCoup = copyOf(applied);
        moveHistory.add(copyOf(applied));

        Joueur previousPlayer = joueurCourant;
        joueurCourant = opponentOf(previousPlayer);

        boolean inCheck = isKingInCheck(joueurCourant.isBlanc());
        List<Coup> replies = getLegalMoves(joueurCourant.isBlanc());
        if (replies.isEmpty()) {
            termine = true;
            if (inCheck) {
                winnerLabel = previousPlayer.isBlanc() ? "White" : "Black";
                drawGame = false;
                applied.setType("ECHEC ET MAT");
                dernierCoup.setType("ECHEC ET MAT");
                moveHistory.get(moveHistory.size() - 1).setType("ECHEC ET MAT");
                statusMessage = "Checkmate. Winner: " + winnerLabel + ".";
            } else {
                winnerLabel = null;
                drawGame = true;
                applied.setType("PAT");
                dernierCoup.setType("PAT");
                moveHistory.get(moveHistory.size() - 1).setType("PAT");
                statusMessage = "Stalemate.";
            }
        } else if (inCheck) {
            winnerLabel = null;
            drawGame = false;
            if ("NORMAL".equals(applied.getType())) {
                applied.setType("ECHEC");
                dernierCoup.setType("ECHEC");
                moveHistory.get(moveHistory.size() - 1).setType("ECHEC");
            }
            statusMessage = (joueurCourant.isBlanc() ? "White" : "Black") + " is in check.";
        } else {
            winnerLabel = null;
            drawGame = false;
            statusMessage = (joueurCourant.isBlanc() ? "White" : "Black") + " to move.";
        }

        syncBoard();
        lastClockUpdateMillis = System.currentTimeMillis();
        notifyAll();
        notifyGameObservers(applied);
        return true;
    }

    public synchronized boolean isLegalMove(Coup coup, Joueur joueur) {
        return joueur != null && isLegalMove(coup, joueur.isBlanc());
    }

    public synchronized boolean coupValide(Coup coup) {
        return joueurCourant != null && isLegalMove(coup, joueurCourant);
    }

    public synchronized boolean isLegalMove(Coup coup, boolean blanc) {
        if (coup == null || coup.dep == null || coup.arr == null) {
            return false;
        }
        if (!inside(coup.dep.x, coup.dep.y) || !inside(coup.arr.x, coup.arr.y) || coup.dep.equals(coup.arr)) {
            return false;
        }
        Piece piece = getPieceAt(coup.dep.x, coup.dep.y);
        if (piece == null || piece.isBlanc() != blanc) {
            return false;
        }
        Piece target = getPieceAt(coup.arr.x, coup.arr.y);
        if (target instanceof King) {
            return false;
        }
        if (!isPseudoLegalMove(piece, coup.dep, coup.arr)) {
            return false;
        }
        MoveState state = applyUnchecked(copyOf(coup), "QUEEN");
        if (state == null) {
            return false;
        }
        boolean legal = !isKingInCheck(blanc);
        undo(state);
        return legal;
    }

    public synchronized List<Coup> getLegalMoves(Joueur joueur) {
        return joueur == null ? new ArrayList<>() : getLegalMoves(joueur.isBlanc());
    }

    public synchronized List<Coup> getLegalMoves(boolean blanc) {
        List<Coup> moves = new ArrayList<>();
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                Piece piece = getPieceAt(row, col);
                if (piece == null || piece.isBlanc() != blanc) {
                    continue;
                }
                Point from = new Point(row, col);
                for (Point target : getPseudoLegalDestinations(piece, from)) {
                    Coup coup = new Coup(new Point(from), target);
                    if (isLegalMove(coup, blanc)) {
                        moves.add(coup);
                    }
                }
            }
        }
        return moves;
    }

    public synchronized List<Point> getLegalDestinations(int row, int col) {
        List<Point> destinations = new ArrayList<>();
        Piece piece = getPieceAt(row, col);
        if (piece == null || joueurCourant == null || piece.isBlanc() != joueurCourant.isBlanc() || termine) {
            return destinations;
        }
        for (Point point : getPseudoLegalDestinations(piece, new Point(row, col))) {
            Coup coup = new Coup(new Point(row, col), point);
            if (isLegalMove(coup, joueurCourant.isBlanc())) {
                destinations.add(point);
            }
        }
        return destinations;
    }

    public synchronized boolean isKingInCheck(Joueur joueur) {
        return joueur != null && isKingInCheck(joueur.isBlanc());
    }

    public synchronized boolean isKingInCheck(boolean blanc) {
        Point king = findKing(blanc);
        return king != null && isSquareAttacked(king.x, king.y, !blanc);
    }

    public synchronized boolean estEchecEtMat(Joueur joueur) {
        return joueur != null && isKingInCheck(joueur) && getLegalMoves(joueur).isEmpty();
    }

    public synchronized boolean estPat(Joueur joueur) {
        return joueur != null && !isKingInCheck(joueur) && getLegalMoves(joueur).isEmpty();
    }

    public synchronized boolean echec(Joueur joueur) {
        return isKingInCheck(joueur);
    }

    public synchronized boolean mat(Joueur joueur) {
        return estEchecEtMat(joueur);
    }

    public synchronized boolean aGagne(Joueur joueur) {
        return joueur != null && opponentOf(joueur) != null && estEchecEtMat(opponentOf(joueur));
    }

    public synchronized List<Coup> getMoveHistory() {
        List<Coup> history = new ArrayList<>();
        for (Coup coup : moveHistory) {
            history.add(copyOf(coup));
        }
        return history;
    }

    public synchronized void resetGame() {
        Plateau plateau = PlateauSingleton.INSTANCE;
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                plateau.getCase(row, col).setPiece(null);
            }
        }
        setupInitialPosition(plateau);
        whiteKingMoved = false;
        blackKingMoved = false;
        whiteQueenRookMoved = false;
        whiteKingRookMoved = false;
        blackQueenRookMoved = false;
        blackKingRookMoved = false;
        enPassantTarget = null;
        pendingHumanMove = null;
        dernierCoup = null;
        moveHistory.clear();
        joueurCourant = joueur1;
        termine = false;
        winnerLabel = null;
        drawGame = false;
        whiteRemainingMillis = INITIAL_TIME_MILLIS;
        blackRemainingMillis = INITIAL_TIME_MILLIS;
        lastClockUpdateMillis = System.currentTimeMillis();
        statusMessage = "White to move.";
        syncBoard();
        notifyAll();
        notifyGameObservers("Game reset.");
    }

    public synchronized void stopGame() {
        termine = true;
        lastClockUpdateMillis = System.currentTimeMillis();
        notifyAll();
    }

    public synchronized void importPgnMoves(List<String> moves) {
        resetGame();
        for (String move : moves) {
            Coup coup = parseCoordinateMove(move);
            if (coup == null || !appliquerCoup(coup)) {
                throw new IllegalArgumentException("Invalid move: " + move);
            }
        }
    }

    public synchronized boolean applyRemoteTurn(Coup coup, boolean whiteClock, long timeoutMillis) {
        long deadline = System.currentTimeMillis() + Math.max(250L, timeoutMillis);
        while (!termine && (joueurCourant == null || joueurCourant.isBlanc() != whiteClock || !(joueurCourant instanceof JHumain))) {
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
        setCoup(coup);
        return true;
    }

    public synchronized boolean pressClock(boolean whiteClock) {
        return false;
    }

    public synchronized boolean canPressClock(boolean whiteClock) {
        return false;
    }

    public synchronized void tickClock() {
        if (termine || joueurCourant == null) {
            lastClockUpdateMillis = System.currentTimeMillis();
            return;
        }

        long now = System.currentTimeMillis();
        if (lastClockUpdateMillis == 0L) {
            lastClockUpdateMillis = now;
            return;
        }

        long elapsed = Math.max(0L, now - lastClockUpdateMillis);
        lastClockUpdateMillis = now;

        if (elapsed == 0L) {
            return;
        }

        if (joueurCourant.isBlanc()) {
            whiteRemainingMillis = Math.max(0L, whiteRemainingMillis - elapsed);
            if (whiteRemainingMillis == 0L) {
                handleTimeout(true);
            }
        } else {
            blackRemainingMillis = Math.max(0L, blackRemainingMillis - elapsed);
            if (blackRemainingMillis == 0L) {
                handleTimeout(false);
            }
        }
    }

    public synchronized boolean isClockActive(boolean white) {
        return !termine && joueurCourant != null && joueurCourant.isBlanc() == white;
    }

    public synchronized int scoreMove(Coup coup, boolean blanc) {
        if (!isLegalMove(coup, blanc)) {
            return Integer.MIN_VALUE;
        }
        Piece target = getPieceAt(coup.arr.x, coup.arr.y);
        int score = target == null ? 0 : pieceValue(target);
        MoveState state = applyUnchecked(copyOf(coup), "QUEEN");
        if (state == null) {
            return Integer.MIN_VALUE;
        }
        if (state.capturedPiece != null && target == null) {
            score += pieceValue(state.capturedPiece);
        }
        if (isKingInCheck(!blanc)) {
            score += 2;
        }
        score += 3 - (Math.abs(3 - coup.arr.x) + Math.abs(3 - coup.arr.y));
        if (isSquareAttacked(coup.arr.x, coup.arr.y, !blanc)) {
            score -= Math.max(1, pieceValue(getPieceAt(coup.arr.x, coup.arr.y)) / 2);
        }
        undo(state);
        return score;
    }

    public synchronized int getPieceValueAt(int row, int col) {
        return pieceValue(getPieceAt(row, col));
    }

    public synchronized int getMaterialScore(boolean white) {
        int score = 0;
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                Piece piece = getPieceAt(row, col);
                if (piece != null && piece.isBlanc() == white) {
                    score += displayPieceValue(piece);
                }
            }
        }
        return score;
    }

    public synchronized int getCapturedScore(boolean white) {
        return TOTAL_NON_KING_MATERIAL - getMaterialScore(!white);
    }

    public synchronized int getMaterialAdvantage(boolean white) {
        return getMaterialScore(white) - getMaterialScore(!white);
    }

    public synchronized boolean isWhiteToMove() {
        return joueurCourant != null && joueurCourant.isBlanc();
    }

    public synchronized boolean canResign() {
        return !termine;
    }

    public synchronized String getResignLabel() {
        if (mode == GameMode.HUMAN_VS_AI) {
            return "White resigns";
        }
        if (joueurCourant == null) {
            return "Resign";
        }
        return (joueurCourant.isBlanc() ? "White" : "Black") + " resigns";
    }

    public synchronized void resignCurrentPlayer() {
        if (termine) {
            return;
        }
        String resigned = mode == GameMode.HUMAN_VS_AI
                ? "White"
                : (joueurCourant != null && joueurCourant.isBlanc() ? "White" : "Black");
        String winner = "White".equals(resigned) ? "Black" : "White";
        termine = true;
        drawGame = false;
        winnerLabel = winner;
        statusMessage = resigned + " resigned. Winner: " + winner + ".";
        lastClockUpdateMillis = System.currentTimeMillis();
        notifyAll();
        notifyGameObservers("RESIGN");
    }

    private List<Point> getPseudoLegalDestinations(Piece piece, Point from) {
        List<Point> destinations = new ArrayList<>();
        if (piece instanceof Pawn) {
            addPawnDestinations(destinations, piece, from);
            return destinations;
        }
        if (piece instanceof Knight) {
            addKnightDestinations(destinations, piece, from);
            return destinations;
        }
        if (piece instanceof Bishop) {
            addRayDestinations(destinations, piece, from, new int[][]{{1, 1}, {1, -1}, {-1, 1}, {-1, -1}});
            return destinations;
        }
        if (piece instanceof Rook) {
            addRayDestinations(destinations, piece, from, new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}});
            return destinations;
        }
        if (piece instanceof Queen) {
            addRayDestinations(destinations, piece, from, new int[][]{
                    {1, 1}, {1, -1}, {-1, 1}, {-1, -1}, {1, 0}, {-1, 0}, {0, 1}, {0, -1}
            });
            return destinations;
        }
        if (piece instanceof King) {
            addKingDestinations(destinations, piece, from);
        }
        return destinations;
    }

    private boolean isPseudoLegalMove(Piece piece, Point from, Point to) {
        for (Point target : getPseudoLegalDestinations(piece, from)) {
            if (target.equals(to)) {
                return true;
            }
        }
        return false;
    }

    private void addPawnDestinations(List<Point> destinations, Piece piece, Point from) {
        int dir = piece.isBlanc() ? -1 : 1;
        int startRow = piece.isBlanc() ? 6 : 1;
        int nextRow = from.x + dir;
        if (inside(nextRow, from.y) && getPieceAt(nextRow, from.y) == null) {
            destinations.add(new Point(nextRow, from.y));
            int jumpRow = from.x + 2 * dir;
            if (from.x == startRow && inside(jumpRow, from.y) && getPieceAt(jumpRow, from.y) == null) {
                destinations.add(new Point(jumpRow, from.y));
            }
        }
        for (int dy : new int[]{-1, 1}) {
            int col = from.y + dy;
            if (!inside(nextRow, col)) {
                continue;
            }
            Piece target = getPieceAt(nextRow, col);
            if (target != null && target.isBlanc() != piece.isBlanc()) {
                destinations.add(new Point(nextRow, col));
            } else if (enPassantTarget != null && enPassantTarget.x == nextRow && enPassantTarget.y == col) {
                Piece adjacent = getPieceAt(from.x, col);
                if (adjacent instanceof Pawn && adjacent.isBlanc() != piece.isBlanc()) {
                    destinations.add(new Point(nextRow, col));
                }
            }
        }
    }

    private void addKnightDestinations(List<Point> destinations, Piece piece, Point from) {
        int[][] offsets = {{-2, -1}, {-2, 1}, {-1, -2}, {-1, 2}, {1, -2}, {1, 2}, {2, -1}, {2, 1}};
        for (int[] offset : offsets) {
            int row = from.x + offset[0];
            int col = from.y + offset[1];
            if (!inside(row, col)) {
                continue;
            }
            Piece target = getPieceAt(row, col);
            if (target == null || target.isBlanc() != piece.isBlanc()) {
                destinations.add(new Point(row, col));
            }
        }
    }

    private void addRayDestinations(List<Point> destinations, Piece piece, Point from, int[][] directions) {
        for (int[] direction : directions) {
            int row = from.x + direction[0];
            int col = from.y + direction[1];
            while (inside(row, col)) {
                Piece target = getPieceAt(row, col);
                if (target == null) {
                    destinations.add(new Point(row, col));
                } else {
                    if (target.isBlanc() != piece.isBlanc()) {
                        destinations.add(new Point(row, col));
                    }
                    break;
                }
                row += direction[0];
                col += direction[1];
            }
        }
    }

    private void addKingDestinations(List<Point> destinations, Piece piece, Point from) {
        for (int row = from.x - 1; row <= from.x + 1; row++) {
            for (int col = from.y - 1; col <= from.y + 1; col++) {
                if ((row == from.x && col == from.y) || !inside(row, col)) {
                    continue;
                }
                Piece target = getPieceAt(row, col);
                if (target == null || target.isBlanc() != piece.isBlanc()) {
                    destinations.add(new Point(row, col));
                }
            }
        }
        if (canCastle(piece.isBlanc(), true)) {
            destinations.add(new Point(from.x, from.y + 2));
        }
        if (canCastle(piece.isBlanc(), false)) {
            destinations.add(new Point(from.x, from.y - 2));
        }
    }

    private boolean canCastle(boolean white, boolean kingSide) {
        int row = white ? 7 : 0;
        if (white ? whiteKingMoved : blackKingMoved) {
            return false;
        }
        if (kingSide ? (white ? whiteKingRookMoved : blackKingRookMoved) : (white ? whiteQueenRookMoved : blackQueenRookMoved)) {
            return false;
        }
        Piece king = getPieceAt(row, 4);
        Piece rook = getPieceAt(row, kingSide ? 7 : 0);
        if (!(king instanceof King) || !(rook instanceof Rook) || king.isBlanc() != white || rook.isBlanc() != white) {
            return false;
        }
        int[] emptyCols = kingSide ? new int[]{5, 6} : new int[]{1, 2, 3};
        for (int col : emptyCols) {
            if (getPieceAt(row, col) != null) {
                return false;
            }
        }
        if (isSquareAttacked(row, 4, !white)) {
            return false;
        }
        int transitCol = kingSide ? 5 : 3;
        int destinationCol = kingSide ? 6 : 2;
        return !isSquareAttacked(row, transitCol, !white) && !isSquareAttacked(row, destinationCol, !white);
    }

    private boolean isSquareAttacked(int row, int col, boolean byWhite) {
        int pawnRow = row + (byWhite ? 1 : -1);
        for (int pawnCol : new int[]{col - 1, col + 1}) {
            Piece pawn = getPieceAt(pawnRow, pawnCol);
            if (pawn instanceof Pawn && pawn.isBlanc() == byWhite) {
                return true;
            }
        }

        int[][] knightOffsets = {{-2, -1}, {-2, 1}, {-1, -2}, {-1, 2}, {1, -2}, {1, 2}, {2, -1}, {2, 1}};
        for (int[] offset : knightOffsets) {
            Piece knight = getPieceAt(row + offset[0], col + offset[1]);
            if (knight instanceof Knight && knight.isBlanc() == byWhite) {
                return true;
            }
        }

        if (isRayAttack(row, col, byWhite, new int[][]{{1, 1}, {1, -1}, {-1, 1}, {-1, -1}}, Bishop.class, Queen.class)) {
            return true;
        }
        if (isRayAttack(row, col, byWhite, new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}, Rook.class, Queen.class)) {
            return true;
        }

        for (int kingRow = row - 1; kingRow <= row + 1; kingRow++) {
            for (int kingCol = col - 1; kingCol <= col + 1; kingCol++) {
                if ((kingRow == row && kingCol == col) || !inside(kingRow, kingCol)) {
                    continue;
                }
                Piece king = getPieceAt(kingRow, kingCol);
                if (king instanceof King && king.isBlanc() == byWhite) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isRayAttack(int row, int col, boolean byWhite, int[][] directions, Class<?> a, Class<?> b) {
        for (int[] direction : directions) {
            int r = row + direction[0];
            int c = col + direction[1];
            while (inside(r, c)) {
                Piece piece = getPieceAt(r, c);
                if (piece != null) {
                    return piece.isBlanc() == byWhite && (a.isInstance(piece) || b.isInstance(piece));
                }
                r += direction[0];
                c += direction[1];
            }
        }
        return false;
    }

    private MoveState applyUnchecked(Coup coup, String promotionChoice) {
        Plateau plateau = PlateauSingleton.INSTANCE;
        Case from = plateau.getCase(coup.dep);
        Case to = plateau.getCase(coup.arr);
        if (from == null || to == null || from.isEmpty()) {
            return null;
        }

        Piece moved = from.getPiece();
        Piece captured = to.getPiece();
        Point capturedSquare = new Point(coup.arr);
        Piece rookPiece = null;
        Point rookFrom = null;
        Point rookTo = null;
        Point previousEnPassant = enPassantTarget == null ? null : new Point(enPassantTarget);
        Coup previousDernierCoup = dernierCoup == null ? null : copyOf(dernierCoup);
        boolean previousWhiteKingMoved = whiteKingMoved;
        boolean previousBlackKingMoved = blackKingMoved;
        boolean previousWhiteQueenRookMoved = whiteQueenRookMoved;
        boolean previousWhiteKingRookMoved = whiteKingRookMoved;
        boolean previousBlackQueenRookMoved = blackQueenRookMoved;
        boolean previousBlackKingRookMoved = blackKingRookMoved;

        if (moved instanceof Pawn && captured == null && coup.dep.y != coup.arr.y && enPassantTarget != null && enPassantTarget.equals(coup.arr)) {
            int capturedRow = coup.arr.x + (moved.isBlanc() ? 1 : -1);
            capturedSquare = new Point(capturedRow, coup.arr.y);
            Case capturedCase = plateau.getCase(capturedSquare);
            captured = capturedCase == null ? null : capturedCase.getPiece();
            if (capturedCase != null) {
                capturedCase.setPiece(null);
            }
            coup.setType("PRISE EN PASSANT");
        }

        from.setPiece(null);
        Piece destinationPiece = moved;

        if (moved instanceof King && Math.abs(coup.arr.y - coup.dep.y) == 2) {
            int row = coup.dep.x;
            if (coup.arr.y > coup.dep.y) {
                rookFrom = new Point(row, 7);
                rookTo = new Point(row, 5);
            } else {
                rookFrom = new Point(row, 0);
                rookTo = new Point(row, 3);
            }
            Case rookFromCase = plateau.getCase(rookFrom);
            Case rookToCase = plateau.getCase(rookTo);
            rookPiece = rookFromCase == null ? null : rookFromCase.getPiece();
            if (rookFromCase != null) {
                rookFromCase.setPiece(null);
            }
            if (rookToCase != null) {
                rookToCase.setPiece(rookPiece);
            }
            coup.setType("ROQUE");
        }

        if (moved instanceof Pawn && (coup.arr.x == 0 || coup.arr.x == 7)) {
            destinationPiece = createPromotionPiece(moved.isBlanc(), promotionChoice);
            coup.setType("PROMOTION");
        }

        to.setPiece(destinationPiece);
        updateCastleFlagsAfterMove(coup.dep, moved);
        if (captured instanceof Rook) {
            updateCastleFlagsAfterRookCapture(capturedSquare);
        }

        if (moved instanceof Pawn && Math.abs(coup.arr.x - coup.dep.x) == 2) {
            enPassantTarget = new Point((coup.dep.x + coup.arr.x) / 2, coup.dep.y);
        } else {
            enPassantTarget = null;
        }

        return new MoveState(
                copyOf(coup),
                moved,
                destinationPiece,
                captured,
                capturedSquare,
                rookPiece,
                rookFrom,
                rookTo,
                previousEnPassant,
                previousDernierCoup,
                previousWhiteKingMoved,
                previousBlackKingMoved,
                previousWhiteQueenRookMoved,
                previousWhiteKingRookMoved,
                previousBlackQueenRookMoved,
                previousBlackKingRookMoved
        );
    }

    private void undo(MoveState state) {
        Plateau plateau = PlateauSingleton.INSTANCE;
        Case from = plateau.getCase(state.coup.dep);
        Case to = plateau.getCase(state.coup.arr);
        if (from == null || to == null) {
            return;
        }

        whiteKingMoved = state.previousWhiteKingMoved;
        blackKingMoved = state.previousBlackKingMoved;
        whiteQueenRookMoved = state.previousWhiteQueenRookMoved;
        whiteKingRookMoved = state.previousWhiteKingRookMoved;
        blackQueenRookMoved = state.previousBlackQueenRookMoved;
        blackKingRookMoved = state.previousBlackKingRookMoved;
        enPassantTarget = state.previousEnPassant == null ? null : new Point(state.previousEnPassant);
        dernierCoup = state.previousDernierCoup == null ? null : copyOf(state.previousDernierCoup);

        if (state.rookFrom != null && state.rookTo != null) {
            Case rookFromCase = plateau.getCase(state.rookFrom);
            Case rookToCase = plateau.getCase(state.rookTo);
            if (rookToCase != null) {
                rookToCase.setPiece(null);
            }
            if (rookFromCase != null) {
                rookFromCase.setPiece(state.rookPiece);
            }
        }

        to.setPiece(null);
        from.setPiece(state.movedPiece);
        if (state.capturedSquare != null) {
            Case capturedCase = plateau.getCase(state.capturedSquare);
            if (capturedCase != null) {
                capturedCase.setPiece(state.capturedPiece);
            }
        }
        syncBoard();
    }

    private void updateCastleFlagsAfterMove(Point from, Piece moved) {
        if (moved instanceof King) {
            if (moved.isBlanc()) {
                whiteKingMoved = true;
            } else {
                blackKingMoved = true;
            }
        } else if (moved instanceof Rook) {
            if (from.x == 7 && from.y == 0) {
                whiteQueenRookMoved = true;
            } else if (from.x == 7 && from.y == 7) {
                whiteKingRookMoved = true;
            } else if (from.x == 0 && from.y == 0) {
                blackQueenRookMoved = true;
            } else if (from.x == 0 && from.y == 7) {
                blackKingRookMoved = true;
            }
        }
    }

    private void updateCastleFlagsAfterRookCapture(Point square) {
        if (square.x == 7 && square.y == 0) {
            whiteQueenRookMoved = true;
        } else if (square.x == 7 && square.y == 7) {
            whiteKingRookMoved = true;
        } else if (square.x == 0 && square.y == 0) {
            blackQueenRookMoved = true;
        } else if (square.x == 0 && square.y == 7) {
            blackKingRookMoved = true;
        }
    }

    private void setupInitialPosition(Plateau plateau) {
        put(plateau, 0, 0, new Rook("black"));
        put(plateau, 0, 1, new Knight("black"));
        put(plateau, 0, 2, new Bishop("black"));
        put(plateau, 0, 3, new Queen("black"));
        put(plateau, 0, 4, new King("black"));
        put(plateau, 0, 5, new Bishop("black"));
        put(plateau, 0, 6, new Knight("black"));
        put(plateau, 0, 7, new Rook("black"));
        for (int col = 0; col < 8; col++) {
            put(plateau, 1, col, new Pawn("black"));
        }

        put(plateau, 7, 0, new Rook("white"));
        put(plateau, 7, 1, new Knight("white"));
        put(plateau, 7, 2, new Bishop("white"));
        put(plateau, 7, 3, new Queen("white"));
        put(plateau, 7, 4, new King("white"));
        put(plateau, 7, 5, new Bishop("white"));
        put(plateau, 7, 6, new Knight("white"));
        put(plateau, 7, 7, new Rook("white"));
        for (int col = 0; col < 8; col++) {
            put(plateau, 6, col, new Pawn("white"));
        }
    }

    private void put(Plateau plateau, int row, int col, Piece piece) {
        plateau.getCase(row, col).setPiece(piece);
        echiquier.setPiece(row, col, piece);
    }

    private void syncBoard() {
        echiquier.syncFromPlateau(PlateauSingleton.INSTANCE);
    }

    private Piece createPromotionPiece(boolean white, String choice) {
        String color = white ? "white" : "black";
        String normalized = choice == null ? "QUEEN" : choice.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "ROOK" -> new Rook(color);
            case "BISHOP" -> new Bishop(color);
            case "KNIGHT" -> new Knight(color);
            default -> new Queen(color);
        };
    }

    private String getPromotionChoice(Piece piece, int targetRow, Joueur joueur) {
        if (!(piece instanceof Pawn) || !((piece.isBlanc() && targetRow == 0) || (!piece.isBlanc() && targetRow == 7))) {
            return "QUEEN";
        }
        if (!(joueur instanceof JHumain) || GraphicsEnvironment.isHeadless()) {
            return "QUEEN";
        }
        Object[] options = {"Queen", "Rook", "Bishop", "Knight"};
        int choice = JOptionPane.showOptionDialog(
                null,
                "Choose the promotion piece",
                "Promotion",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]
        );
        return switch (choice) {
            case 1 -> "ROOK";
            case 2 -> "BISHOP";
            case 3 -> "KNIGHT";
            default -> "QUEEN";
        };
    }

    private Coup parseCoordinateMove(String move) {
        Matcher matcher = Pattern.compile("([a-h][1-8])([a-h][1-8])([qrbn])?").matcher(move.trim().toLowerCase(Locale.ROOT));
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
        int col = square.charAt(0) - 'a';
        int row = 8 - (square.charAt(1) - '0');
        return inside(row, col) ? new Point(row, col) : null;
    }

    private Joueur opponentOf(Joueur joueur) {
        if (joueur == joueur1) {
            return joueur2;
        }
        if (joueur == joueur2) {
            return joueur1;
        }
        return null;
    }

    private Joueur getJoueurSuivant() {
        return joueurCourant;
    }

    private Point findKing(boolean white) {
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                Piece piece = getPieceAt(row, col);
                if (piece instanceof King && piece.isBlanc() == white) {
                    return new Point(row, col);
                }
            }
        }
        return null;
    }

    private void handleTimeout(boolean whiteFlagged) {
        if (termine) {
            return;
        }
        termine = true;
        boolean opponentWhite = !whiteFlagged;
        if (hasSufficientMatingMaterial(opponentWhite)) {
            drawGame = false;
            winnerLabel = opponentWhite ? "White" : "Black";
            statusMessage = (whiteFlagged ? "White" : "Black") + " lost on time. Winner: " + winnerLabel + ".";
        } else {
            drawGame = true;
            winnerLabel = null;
            statusMessage = "Draw on time: insufficient mating material.";
        }
        notifyAll();
        notifyGameObservers("TIMEOUT");
    }

    private boolean hasSufficientMatingMaterial(boolean white) {
        int bishops = 0;
        int knights = 0;
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                Piece piece = getPieceAt(row, col);
                if (piece == null || piece.isBlanc() != white) {
                    continue;
                }
                if (piece instanceof Queen || piece instanceof Rook || piece instanceof Pawn) {
                    return true;
                }
                if (piece instanceof Bishop) {
                    bishops++;
                } else if (piece instanceof Knight) {
                    knights++;
                }
            }
        }
        return bishops >= 2 || (bishops >= 1 && knights >= 1);
    }

    private Piece getPieceAt(int row, int col) {
        if (!inside(row, col)) {
            return null;
        }
        return PlateauSingleton.INSTANCE.getCase(row, col).getPiece();
    }

    private boolean inside(int row, int col) {
        return row >= 0 && row < 8 && col >= 0 && col < 8;
    }

    private int pieceValue(Piece piece) {
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

    private int displayPieceValue(Piece piece) {
        return piece instanceof King ? 0 : pieceValue(piece);
    }

    private Coup copyOf(Coup coup) {
        return new Coup(new Point(coup.dep), new Point(coup.arr), coup.getType());
    }

    private void notifyGameObservers(Object arg) {
        for (JeuObserver observer : observers) {
            observer.update(arg);
        }
    }
}
