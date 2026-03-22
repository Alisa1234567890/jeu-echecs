package org.model.piece;

import org.model.plateau.DecorateurCasesCavalier;
import org.model.plateau.PlateauSingleton;

public class Cavalier extends Piece {

    public Cavalier(String color) {
        super(color, new DecorateurCasesCavalier(PlateauSingleton.INSTANCE));
    }

    @Override
    public String getImageName() {
        return getColor().equals("white") ? "wN" : "bN";
    }
}
