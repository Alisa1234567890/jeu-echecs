package org.model.piece;

import org.model.plateau.DecorateurCasesEnDiagonale;
import org.model.plateau.DecorateurCasesEnLigne;
import org.model.plateau.PlateauSingleton;

public class Dame extends Piece {

    public Dame(String color) {
        super(color, new DecorateurCasesEnLigne(PlateauSingleton.INSTANCE,
                     new DecorateurCasesEnDiagonale(PlateauSingleton.INSTANCE)));
    }
}
