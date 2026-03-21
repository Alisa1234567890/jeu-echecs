package org.model.piece;

import org.model.plateau.Case;
import org.model.plateau.Plateau;
import org.model.plateau.PlateauSingleton;

import java.util.ArrayList;

public class Roi extends Piece {

    public Roi(String color) {
        super(color);
    }

    public ArrayList<Case> getCaseAccessible() {
        ArrayList<Case> res = new ArrayList<>();
        if (position == null) return res;

        int x = position.getX();
        int y = position.getY();
        Plateau plateau = findPlateau();
        if (plateau == null) return res;

        //Mouvement de Roi
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx == 0 && dy == 0) continue;
                Case c = plateau.getCase(x + dx, y + dy);
                if (c != null && (c.isEmpty() || c.getPiece().isBlanc() != this.blanc)) {
                    res.add(c);
                }
            }
        }

        int initialRow = blanc ? 7 : 0;
        if (x == initialRow && y == 4) {
            Case sq5 = plateau.getCase(x, 5);
            Case sq6 = plateau.getCase(x, 6);
            Case sq7 = plateau.getCase(x, 7);
            if (sq5 != null && sq5.isEmpty()
                    && sq6 != null && sq6.isEmpty()
                    && sq7 != null && sq7.getPiece() instanceof Tour
                    && sq7.getPiece().isBlanc() == this.blanc) {
                res.add(sq6);
            }

            Case sq3 = plateau.getCase(x, 3);
            Case sq2 = plateau.getCase(x, 2);
            Case sq1 = plateau.getCase(x, 1);
            Case sq0 = plateau.getCase(x, 0);
            if (sq3 != null && sq3.isEmpty()
                    && sq2 != null && sq2.isEmpty()
                    && sq1 != null && sq1.isEmpty()
                    && sq0 != null && sq0.getPiece() instanceof Tour
                    && sq0.getPiece().isBlanc() == this.blanc) {
                res.add(sq2);
            }
        }

        return res;
    }

    private Plateau findPlateau() {
        return PlateauSingleton.INSTANCE;
    }
}
