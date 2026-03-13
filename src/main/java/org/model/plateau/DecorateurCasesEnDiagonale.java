package org.model.plateau;

import org.model.piece.Piece;

import java.util.ArrayList;

public class DecorateurCasesEnDiagonale extends DecorateurCasesAccessibles {

    public DecorateurCasesEnDiagonale(Plateau plateau) {
        super(plateau);
    }

    public DecorateurCasesEnDiagonale(Plateau plateau, DecorateurCasesAccessibles base) {
        super(plateau, base);
    }

    @Override
    protected ArrayList<Case> getMesCasesAccessibles(Piece piece) {
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
