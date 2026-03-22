package org.model.piece;

import org.model.plateau.DecorateurCasesPion;
import org.model.plateau.PlateauSingleton;

public class Pion extends Piece {

    public Pion(String color) {
        super(color, new DecorateurCasesPion(PlateauSingleton.INSTANCE));
    }

    @Override
    public String getImageName() {
        return getColor().equals("white") ? "wP" : "bP";
    }
}
