package org.view;

import org.model.Coup;
import org.model.JeuObserver;
import org.model.plateau.PlateauSingleton;

/**
 * Console view with Unicode symbols for displaying the chessboard state.
 */
public class VueConsole implements JeuObserver {

    private int halfMove = 0;

    @Override
    public void update(Object arg) {
        if (arg instanceof Coup coup) {
            halfMove++;
            afficherCoup(coup, halfMove);
            afficherPlateau();
            afficherTour();
        } else if (arg instanceof String msg) {
            // End of game
            System.out.println();
            System.out.printf(" %-38s%n", "FIN : " + msg);
            System.out.println();
            System.out.println("=== PNG File ===");
            System.out.println("Fichier PNG : " + System.getProperty("user.dir") + "/partie_echecs.png");
        } else {
            halfMove = 0;
            System.out.println();
            System.out.println("NOUVELLE PARTIE");
            afficherPlateau();
            afficherTour();
        }
    }

    private void afficherCoup(Coup coup, int moveNumber) {
        String moveNum = (moveNumber % 2 == 1) ? (moveNumber / 2 + 1) + ". " : "";
        String from = "" + (char) ('a' + coup.dep.x) + (8 - coup.dep.y);
        String to = "" + (char) ('a' + coup.arr.x) + (8 - coup.arr.y);
        System.out.printf("%-4s %s %s -> %s %n", moveNum, coup.getType(), from, to);
    }

    private void afficherPlateau() {
        System.out.println();
        System.out.println("  a b c d e f g h");
        System.out.println("  +-+-+-+-+-+-+-+-+");

        var plateau = PlateauSingleton.INSTANCE;
        for (int row = 0; row < 8; row++) {
            System.out.print((8 - row) + " ");
            for (int col = 0; col < 8; col++) {
                var cas = plateau.getCase(row, col);
                if (cas != null && !cas.isEmpty()) {
                    System.out.print("|" + getPieceSymbol(cas.getPiece()));
                } else {
                    System.out.print("| ");
                }
            }
            System.out.println("|" + (8 - row));
        }
        System.out.println("  +-+-+-+-+-+-+-+-+");
        System.out.println("  a b c d e f g h");
    }

    private void afficherTour() {
        System.out.println();
    }

    private String getPieceSymbol(org.model.piece.Piece piece) {
        if (piece == null) return " ";

        String type = piece.getClass().getSimpleName();
        boolean isWhite = piece.isBlanc();

        // Unicode chess symbols
        // White pieces: ♔ ♕ ♖ ♗ ♘ ♙
        // Black pieces: ♚ ♛ ♜ ♝ ♞ ♟
        return switch (type) {
            case "King" -> isWhite ? "♔" : "♚";
            case "Queen" -> isWhite ? "♕" : "♛";
            case "Rook" -> isWhite ? "♖" : "♜";
            case "Bishop" -> isWhite ? "♗" : "♝";
            case "Knight" -> isWhite ? "♘" : "♞";
            case "Pawn" -> isWhite ? "♙" : "♟";
            default -> "?";
        };
    }
}

