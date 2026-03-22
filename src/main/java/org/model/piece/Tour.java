package org.model.piece;

import org.model.plateau.DecorateurCasesEnLigne;
import org.model.plateau.PlateauSingleton;

public class Tour extends Piece {

    public Tour(String color) {
        super(color, new DecorateurCasesEnLigne(PlateauSingleton.INSTANCE));
    }

    @Override
    public String getImageName() {
        return color.equals("white") ? "wR" : "bR";
    }
}
