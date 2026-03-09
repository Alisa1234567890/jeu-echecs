package org.model;

public class King extends Piece {

    public King(String color) {
        super(color);
    }

    @Override
    public boolean isValidMove(int startRow, int startCol,
                               int endRow, int endCol) {

        int rowDiff = Math.abs(endRow - startRow);
        int colDiff = Math.abs(endCol - startCol);

        return rowDiff <= 1 && colDiff <= 1;
    }

    @Override
    public String getImageName() {
        return color.equals("white") ? "Pieces/wK.png" : "Pieces/bK.png";
    }
}
