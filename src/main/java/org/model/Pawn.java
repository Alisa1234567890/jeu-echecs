package org.model;

import java.util.ArrayList;

public class Pawn extends Piece {

    public Pawn(String color) {
        super(color);
    }

    public ArrayList<Case> getCaseAccessible() {
        return new ArrayList<>();
    }

    @Override
    public boolean isValidMove(int startRow, int startCol,
                               int endRow, int endCol) {

        int direction = getColor().equals("white") ? -1 : 1;

        if (startCol == endCol && endRow == startRow + direction) {
            return true;
        }

        if (startCol == endCol &&
                ((getColor().equals("white") && startRow == 6 && endRow == 4) ||
                        (getColor().equals("black") && startRow == 1 && endRow == 3))) {
            return true;
        }

        return false;
    }

    @Override
    public String getImageName() {
        return getColor().equals("white") ? "Pieces/wP.png" : "Pieces/bP.png";
    }
}
