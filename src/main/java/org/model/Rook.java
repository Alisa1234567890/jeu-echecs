package org.model;

public class Rook extends Piece {

    public Rook(String color) {
        super(color);
    }

    @Override
    public boolean isValidMove(int startRow, int startCol,
                               int endRow, int endCol) {
        return startRow == endRow || startCol == endCol;
    }

    @Override
    public String getImageName() {
        return color.equals("white") ? "Pieces/wR.png" : "Pieces/bR.png";
    }
}
