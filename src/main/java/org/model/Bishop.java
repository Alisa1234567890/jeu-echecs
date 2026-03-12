package org.model;

import org.model.plateau.Case;
import org.model.plateau.DecorateurCasesEnDiagonale;
import org.model.plateau.Plateau;
import org.model.plateau.PlateauSingleton;

import java.util.ArrayList;

public class Bishop extends Piece {

    public Bishop(String color) {
        super(color);
    }

    @Override
    public ArrayList<Case> getCaseAccessible() {
        Plateau p = PlateauSingleton.INSTANCE;
        DecorateurCasesEnDiagonale d = new DecorateurCasesEnDiagonale(p);
        return d.getAccessibleCases(this);
    }

    @Override
    public boolean isValidMove(int startRow, int startCol,
                               int endRow, int endCol) {

        return Math.abs(endRow - startRow) ==
                Math.abs(endCol - startCol);
    }

    @Override
    public String getImageName() {
        return color.equals("white") ? "Pieces/wB.jpeg" : "Pieces/bB.jpeg";
    }
}
