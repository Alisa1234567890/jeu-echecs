package org.model.piece;

import org.model.plateau.DecorateurCasesEnDiagonale;
import org.model.plateau.PlateauSingleton;

public class Fou extends Piece {

    public Fou(String color) {
        super(color, new DecorateurCasesEnDiagonale(PlateauSingleton.INSTANCE));
    }

    @Override
    public String getImageName() {
        return color.equals("white") ? "wB" : "bB";
    }
}
