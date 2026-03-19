package org.boardgame.checkers;

import org.boardgame.AbstractGridGame;
import org.boardgame.GridCoordinate;
import org.boardgame.GridGame;
import org.boardgame.GridMove;
import org.boardgame.GridPiece;

import java.util.ArrayList;
import java.util.List;

public class CheckersGame extends AbstractGridGame implements GridGame {

    private final CheckersPiece[][] board = new CheckersPiece[8][8];

    public CheckersGame() {
        super(8, "White", "White to move.");
        reset();
    }

    @Override
    public String getTitle() {
        return "Checkers";
    }

    @Override
    public GridPiece getPieceAt(int row, int col) {
        if (!isInside(row, col)) {
            return null;
        }
        return board[row][col];
    }

    @Override
    public List<GridMove> getLegalMovesFrom(GridCoordinate from) {
        List<GridMove> allCaptures = getAllCapturesForCurrentPlayer();
        if (!allCaptures.isEmpty()) {
            return allCaptures.stream().filter(move -> move.from().equals(from)).toList();
        }

        List<GridMove> moves = new ArrayList<>();
        CheckersPiece piece = pieceAt(from.row(), from.col());
        if (piece == null || !piece.owner().equals(currentPlayer)) {
            return moves;
        }

        for (int[] dir : directionsFor(piece, false)) {
            int targetRow = from.row() + dir[0];
            int targetCol = from.col() + dir[1];
            if (isInside(targetRow, targetCol) && pieceAt(targetRow, targetCol) == null) {
                moves.add(new GridMove(from, new GridCoordinate(targetRow, targetCol)));
            }
        }
        return moves;
    }

    @Override
    public boolean playMove(GridMove move) {
        if (finished) {
            return false;
        }
        List<GridMove> legalMoves = getLegalMovesFrom(move.from());
        if (legalMoves.stream().noneMatch(move::equals)) {
            return false;
        }

        CheckersPiece piece = pieceAt(move.from().row(), move.from().col());
        if (piece == null) {
            return false;
        }

        int rowDiff = move.to().row() - move.from().row();
        int colDiff = move.to().col() - move.from().col();
        board[move.from().row()][move.from().col()] = null;
        board[move.to().row()][move.to().col()] = piece;

        if (Math.abs(rowDiff) == 2 && Math.abs(colDiff) == 2) {
            int capturedRow = move.from().row() + rowDiff / 2;
            int capturedCol = move.from().col() + colDiff / 2;
            board[capturedRow][capturedCol] = null;
        }

        if ("White".equals(piece.owner()) && move.to().row() == 0) {
            piece.crown();
        }
        if ("Black".equals(piece.owner()) && move.to().row() == 7) {
            piece.crown();
        }

        switchPlayer();
        if (getAllMovesForCurrentPlayer().isEmpty()) {
            finished = true;
            statusMessage = "No legal moves. Winner: " + ("White".equals(currentPlayer) ? "Black" : "White") + ".";
        } else {
            statusMessage = currentPlayer + " to move.";
        }
        return true;
    }

    @Override
    public void reset() {
        finished = false;
        currentPlayer = "White";
        statusMessage = "White to move.";
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                board[row][col] = null;
            }
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 8; col++) {
                if ((row + col) % 2 == 1) {
                    board[row][col] = new CheckersPiece("Black");
                }
            }
        }
        for (int row = 5; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                if ((row + col) % 2 == 1) {
                    board[row][col] = new CheckersPiece("White");
                }
            }
        }
    }

    private List<GridMove> getAllMovesForCurrentPlayer() {
        List<GridMove> captures = getAllCapturesForCurrentPlayer();
        if (!captures.isEmpty()) {
            return captures;
        }

        List<GridMove> moves = new ArrayList<>();
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                moves.addAll(getLegalMovesFrom(new GridCoordinate(row, col)));
            }
        }
        return moves;
    }

    private List<GridMove> getAllCapturesForCurrentPlayer() {
        List<GridMove> captures = new ArrayList<>();
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                CheckersPiece piece = pieceAt(row, col);
                if (piece == null || !piece.owner().equals(currentPlayer)) {
                    continue;
                }
                GridCoordinate from = new GridCoordinate(row, col);
                for (int[] dir : directionsFor(piece, true)) {
                    int middleRow = row + dir[0];
                    int middleCol = col + dir[1];
                    int targetRow = row + 2 * dir[0];
                    int targetCol = col + 2 * dir[1];
                    if (!isInside(targetRow, targetCol) || !isInside(middleRow, middleCol)) {
                        continue;
                    }
                    CheckersPiece middlePiece = pieceAt(middleRow, middleCol);
                    if (middlePiece != null && !middlePiece.owner().equals(piece.owner()) && pieceAt(targetRow, targetCol) == null) {
                        captures.add(new GridMove(from, new GridCoordinate(targetRow, targetCol)));
                    }
                }
            }
        }
        return captures;
    }

    private int[][] directionsFor(CheckersPiece piece, boolean capture) {
        if (piece.isKing()) {
            return new int[][]{{-1, -1}, {-1, 1}, {1, -1}, {1, 1}};
        }
        if ("White".equals(piece.owner())) {
            return new int[][]{{-1, -1}, {-1, 1}};
        }
        return new int[][]{{1, -1}, {1, 1}};
    }

    private CheckersPiece pieceAt(int row, int col) {
        return isInside(row, col) ? board[row][col] : null;
    }

    private boolean isInside(int row, int col) {
        return row >= 0 && row < 8 && col >= 0 && col < 8;
    }
}
