package org.model.piece;

import org.model.plateau.DecorateurCasesRoi;
import org.model.plateau.Plateau;

public class Roi extends Piece {

    public Roi(String color, Plateau plateau) {
        super(color, new DecorateurCasesRoi(plateau));
    }

    @Override
    public String getImageName() {
        return isBlanc() ? "wK" : "bK";
    }
}
