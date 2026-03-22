package org.model.plateau;

import org.model.piece.Piece;

import java.util.ArrayList;

public class DecorateurCasesCavalier extends DecorateurCasesAccessibles {

    public DecorateurCasesCavalier(Plateau plateau) {
        super(plateau);
    }

    @Override
    protected ArrayList<Case> getMesCasesAccessibles(Piece piece) {
        ArrayList<Case> res = new ArrayList<>();
        if (piece.getCase() == null) return res;

        int x = piece.getCase().getX();
        int y = piece.getCase().getY();

        // 8 sauts en L via Direction.sauts()
        for (Direction d : Direction.sauts()) {
            Case c = plateau.getCase(x + d.dx, y + d.dy);
            if (c != null && (c.isEmpty() || c.getPiece().isBlanc() != piece.isBlanc())) {
                res.add(c);
            }
        }

        return res;
    }
}

