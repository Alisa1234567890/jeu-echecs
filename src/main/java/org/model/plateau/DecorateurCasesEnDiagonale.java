package org.model.plateau;

import org.model.piece.Piece;

import java.util.ArrayList;

public class DecorateurCasesEnDiagonale extends DecorateurCasesAccessibles {

    public DecorateurCasesEnDiagonale(Plateau plateau) {
        super(plateau);
    }

    @Override
    protected ArrayList<Case> getMesCasesAccessibles(Piece piece) {
        ArrayList<Case> res = new ArrayList<>();
        if (piece.getCase() == null) return res;
        int x = piece.getCase().getX();
        int y = piece.getCase().getY();
        res.addAll(collectRay(x, y, Direction.HAUT_DROITE,  piece));
        res.addAll(collectRay(x, y, Direction.HAUT_GAUCHE,  piece));
        res.addAll(collectRay(x, y, Direction.BAS_DROITE,   piece));
        res.addAll(collectRay(x, y, Direction.BAS_GAUCHE,   piece));
        return res;
    }
}
