package org.model;

public class Queen extends Piece {

    public Queen(String color) {
        super(color);
    }

    @Override
    public boolean isValidMove(int startRow, int startCol,
                               int endRow, int endCol) {

        boolean rookMove = startRow == endRow || startCol == endCol;
        boolean bishopMove = Math.abs(endRow - startRow) ==
                Math.abs(endCol - startCol);

        return rookMove || bishopMove;
    }

    @Override
    public String getImageName() {
        return color.equals("white") ? "Pieces/wQ.png" : "Pieces/bQ.png";
    }
}
