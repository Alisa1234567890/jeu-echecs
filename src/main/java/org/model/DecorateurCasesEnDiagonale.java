package org.model;

import java.util.ArrayList;

public class DecorateurCasesEnDiagonale extends DecorateurCasesAccessibles {

    public DecorateurCasesEnDiagonale(Plateau plateau) {
        super(plateau);
    }

    @Override
    public ArrayList<Case> getAccessibleCases(Piece piece) {
        ArrayList<Case> res = new ArrayList<>();
        if (piece.getCase() == null) return res;
        int x = piece.getCase().getX();
        int y = piece.getCase().getY();
        res.addAll(collectRay(x, y, 1, 1, piece));
        res.addAll(collectRay(x, y, 1, -1, piece));
        res.addAll(collectRay(x, y, -1, 1, piece));
        res.addAll(collectRay(x, y, -1, -1, piece));
        return res;
    }
}
