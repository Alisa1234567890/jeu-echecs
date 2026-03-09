package org.model;

public class Bishop extends Piece {

    public Bishop(String color) {
        super(color);
    }

    @Override
    public boolean isValidMove(int startRow, int startCol,
                               int endRow, int endCol) {

        return Math.abs(endRow - startRow) ==
                Math.abs(endCol - startCol);
    }

    @Override
    public String getImageName() {
        return color.equals("white") ? "Pieces/wB.png" : "Pieces/bB.png";
    }
}
