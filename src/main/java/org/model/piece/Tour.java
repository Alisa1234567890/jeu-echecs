package org.model.piece;

import org.model.plateau.DecorateurCasesEnLigne;
import org.model.plateau.Plateau;

public class Tour extends Piece {

    public Tour(String color, Plateau plateau) {
        super(color, new DecorateurCasesEnLigne(plateau));
    }

    @Override
    public String getImageName() {
        return color.equals("white") ? "wR" : "bR";
    }
}
