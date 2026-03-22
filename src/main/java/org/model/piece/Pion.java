package org.model.piece;

import org.model.plateau.DecorateurCasesPion;
import org.model.plateau.Plateau;

public class Pion extends Piece {

    public Pion(String color, Plateau plateau) {
        super(color, new DecorateurCasesPion(plateau));
    }

    @Override
    public String getImageName() {
        return getColor().equals("white") ? "wP" : "bP";
    }
}
