package org.model.piece;

import org.model.plateau.DecorateurCasesEnDiagonale;
import org.model.plateau.Plateau;

public class Fou extends Piece {

    public Fou(String color, Plateau plateau) {
        super(color, new DecorateurCasesEnDiagonale(plateau));
    }

    @Override
    public String getImageName() {
        return color.equals("white") ? "wB" : "bB";
    }
}
