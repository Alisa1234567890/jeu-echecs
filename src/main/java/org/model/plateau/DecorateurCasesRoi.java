package org.model.plateau;

import org.model.piece.Piece;
import org.model.piece.Tour;

import java.util.ArrayList;

public class DecorateurCasesRoi extends DecorateurCasesAccessibles {

    public DecorateurCasesRoi(Plateau plateau) {
        super(plateau);
    }

    @Override
    protected ArrayList<Case> getMesCasesAccessibles(Piece piece) {
        ArrayList<Case> res = new ArrayList<>();
        if (piece.getCase() == null) return res;

        int x = piece.getCase().getX();
        int y = piece.getCase().getY();

        // Mouvements normaux : 1 case dans les 8 directions
        for (Direction d : Direction.glissement()) {
            Case c = plateau.getCase(x + d.dx, y + d.dy);
            if (c != null && (c.isEmpty() || c.getPiece().isBlanc() != piece.isBlanc())) {
                res.add(c);
            }
        }

        // Roque (uniquement si le roi est sur sa case initiale)
        int initialRow = piece.isBlanc() ? 7 : 0;
        if (x == initialRow && y == 4) {

            // Roque côté roi (petit roque)
            Case sq5 = plateau.getCase(x, 5);
            Case sq6 = plateau.getCase(x, 6);
            Case sq7 = plateau.getCase(x, 7);
            if (sq5 != null && sq5.isEmpty()
                    && sq6 != null && sq6.isEmpty()
                    && sq7 != null && sq7.getPiece() instanceof Tour
                    && sq7.getPiece().isBlanc() == piece.isBlanc()) {
                res.add(sq6);
            }

            // Roque côté dame (grand roque)
            Case sq3 = plateau.getCase(x, 3);
            Case sq2 = plateau.getCase(x, 2);
            Case sq1 = plateau.getCase(x, 1);
            Case sq0 = plateau.getCase(x, 0);
            if (sq3 != null && sq3.isEmpty()
                    && sq2 != null && sq2.isEmpty()
                    && sq1 != null && sq1.isEmpty()
                    && sq0 != null && sq0.getPiece() instanceof Tour
                    && sq0.getPiece().isBlanc() == piece.isBlanc()) {
                res.add(sq2);
            }
        }

        return res;
    }
}

