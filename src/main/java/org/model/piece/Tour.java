package org.model.piece;

import org.model.plateau.Case;
import org.model.plateau.DecorateurCasesEnLigne;
import org.model.plateau.Plateau;
import org.model.plateau.PlateauSingleton;

import java.util.ArrayList;

public class Tour extends Piece {

    public Tour(String color) {
        super(color);
    }

    @Override
    public ArrayList<Case> getCaseAccessible() {
        Plateau p = PlateauSingleton.INSTANCE;
        DecorateurCasesEnLigne d = new DecorateurCasesEnLigne(p);
        return d.getAccessibleCases(this);
    }

    @Override
    public String getImageName() {
        return color.equals("white") ? "wR" : "bR";
    }
}
