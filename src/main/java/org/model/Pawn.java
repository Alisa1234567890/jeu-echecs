package org.model;

public class Pawn extends Piece {

    public Pawn(String color) {
        super(color);
    }

    @Override
    public boolean isValidMove(int startRow, int startCol,
                               int endRow, int endCol) {

        int direction = color.equals("white") ? -1 : 1;

        // Move forward one square
        if (startCol == endCol && endRow == startRow + direction) {
            return true;
        }

        // First move: two squares
        if (startCol == endCol &&
                ((color.equals("white") && startRow == 6 && endRow == 4) ||
                        (color.equals("black") && startRow == 1 && endRow == 3))) {
            return true;
        }

        return false;
    }

    @Override
    public String getImageName() {
        return color.equals("white") ? "Pieces/wP.png" : "Pieces/bP.png";
    }
}
