package org.model;

import org.model.plateau.Case;

import java.util.ArrayList;

public class Knight extends Piece {

    public Knight(String color) {
        super(color);
    }

    public ArrayList<Case> getCaseAccessible() {
        return new ArrayList<>();
    }

    @Override
    public boolean isValidMove(int startRow, int startCol,
                               int endRow, int endCol) {

        int rowDiff = Math.abs(endRow - startRow);
        int colDiff = Math.abs(endCol - startCol);

        return (rowDiff == 2 && colDiff == 1) ||
                (rowDiff == 1 && colDiff == 2);
    }

    @Override
    public String getImageName() {
        return getColor().equals("white") ? "Pieces/wN.png" : "Pieces/bN.png";
    }
}
