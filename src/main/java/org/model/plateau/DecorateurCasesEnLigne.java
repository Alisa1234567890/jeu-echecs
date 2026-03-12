package org.model.plateau;

import org.model.Piece;

import java.util.ArrayList;

public class DecorateurCasesEnLigne extends DecorateurCasesAccessibles {

    public DecorateurCasesEnLigne(Plateau plateau) {
        super(plateau);
    }

    @Override
    protected ArrayList<Case> getMesCasesAccessibles(Piece piece) {
        ArrayList<Case> res = new ArrayList<>();
        if (piece.getCase() == null) return res;
        int x = piece.getCase().getX();
        int y = piece.getCase().getY();
        res.addAll(collectRay(x, y, 1, 0, piece));
        res.addAll(collectRay(x, y, -1, 0, piece));
        res.addAll(collectRay(x, y, 0, 1, piece));
        res.addAll(collectRay(x, y, 0, -1, piece));
        return res;
    }
}
