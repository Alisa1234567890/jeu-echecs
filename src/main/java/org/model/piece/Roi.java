package org.model.piece;

import org.model.plateau.DecorateurCasesRoi;
import org.model.plateau.PlateauSingleton;

public class Roi extends Piece {

    public Roi(String color) {
        super(color, new DecorateurCasesRoi(PlateauSingleton.INSTANCE));
    }

    @Override
    public String getImageName() {
        return isBlanc() ? "wK" : "bK";
    }
}
