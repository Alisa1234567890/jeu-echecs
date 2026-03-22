package org.model.plateau;

import org.model.piece.Piece;

import java.util.ArrayList;

public class DecorateurCasesEnLigne extends DecorateurCasesAccessibles {

    public DecorateurCasesEnLigne(Plateau plateau) {
        super(plateau);
    }

    public DecorateurCasesEnLigne(Plateau plateau, DecorateurCasesAccessibles base) {
        super(plateau, base);
    }

    @Override
    protected ArrayList<Case> getMesCasesAccessibles(Piece piece) {
        ArrayList<Case> res = new ArrayList<>();
        if (piece.getCase() == null) return res;
        int x = piece.getCase().getX();
        int y = piece.getCase().getY();
        res.addAll(collectRay(x, y, Direction.HAUT,   piece));
        res.addAll(collectRay(x, y, Direction.BAS,    piece));
        res.addAll(collectRay(x, y, Direction.GAUCHE, piece));
        res.addAll(collectRay(x, y, Direction.DROITE, piece));
        return res;
    }
}
