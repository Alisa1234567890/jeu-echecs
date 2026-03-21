package org.model.piece;

import org.model.plateau.Case;
import org.model.plateau.DecorateurCasesEnDiagonale;
import org.model.plateau.Plateau;
import org.model.plateau.PlateauSingleton;

import java.util.ArrayList;

public class Fou extends Piece {

    public Fou(String color) {
        super(color);
    }

    @Override
    public ArrayList<Case> getCaseAccessible() {
        Plateau p = PlateauSingleton.INSTANCE;
        DecorateurCasesEnDiagonale d = new DecorateurCasesEnDiagonale(p);
        return d.getAccessibleCases(this);
    }


    @Override
    public String getImageName() {
        return color.equals("white") ? "wB" : "bB";
    }
}
