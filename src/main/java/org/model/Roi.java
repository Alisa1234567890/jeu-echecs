package org.model;

import java.util.ArrayList;

public class Roi extends Piece {

    public Roi(boolean blanc) {
        super(blanc);
    }

    @Override
    public ArrayList<Case> getCaseAccessible() {
        ArrayList<Case> res = new ArrayList<>();
        if (position == null) return res;
        int x = position.getX();
        int y = position.getY();
        Plateau plateau = findPlateau();
        if (plateau == null) return res;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx == 0 && dy == 0) continue;
                Case c = plateau.getCase(x + dx, y + dy);
                if (c != null && (c.isEmpty() || c.getPiece().isBlanc() != this.blanc)) {
                    res.add(c);
                }
            }
        }
        return res;
    }


    private Plateau findPlateau() {
        return PlateauSingleton.INSTANCE;
    }
}
