package org.model.piece;

import org.model.plateau.DecorateurCasesCavalier;
import org.model.plateau.Plateau;

public class Cavalier extends Piece {

    public Cavalier(String color, Plateau plateau) {
        super(color, new DecorateurCasesCavalier(plateau));
    }

    @Override
    public String getImageName() {
        return getColor().equals("white") ? "wN" : "bN";
    }
}
