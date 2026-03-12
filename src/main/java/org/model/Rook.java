package org.model;

import org.model.plateau.Case;
import org.model.plateau.DecorateurCasesEnLigne;
import org.model.plateau.Plateau;
import org.model.plateau.PlateauSingleton;

import java.util.ArrayList;

public class Rook extends Piece {

    public Rook(String color) {
        super(color);
    }

    @Override
    public ArrayList<Case> getCaseAccessible() {
        Plateau p = PlateauSingleton.INSTANCE;
        DecorateurCasesEnLigne d = new DecorateurCasesEnLigne(p);
        return d.getAccessibleCases(this);
    }

    @Override
    public boolean isValidMove(int startRow, int startCol,
                               int endRow, int endCol) {
        return startRow == endRow || startCol == endCol;
    }

    @Override
    public String getImageName() {
        return color.equals("white") ? "Pieces/wR.jpeg" : "Pieces/bR.jpeg";
    }
}
