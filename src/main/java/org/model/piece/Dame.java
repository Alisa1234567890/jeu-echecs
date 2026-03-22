package org.model.piece;

import org.model.plateau.DecorateurCasesEnDiagonale;
import org.model.plateau.DecorateurCasesEnLigne;
import org.model.plateau.Plateau;

public class Dame extends Piece {

    public Dame(String color, Plateau plateau) {
        super(color, new DecorateurCasesEnLigne(plateau,
                     new DecorateurCasesEnDiagonale(plateau)));
    }
}
