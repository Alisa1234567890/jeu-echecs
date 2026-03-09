package org.model;

import java.util.ArrayList;


public abstract class DecorateurCasesAccessibles {

    protected final Plateau plateau;

    public DecorateurCasesAccessibles(Plateau plateau) {
        this.plateau = plateau;
    }

    public abstract ArrayList<Case> getAccessibleCases(Piece piece);

    protected ArrayList<Case> collectRay(int startX, int startY, int dx, int dy, Piece piece) {
        ArrayList<Case> res = new ArrayList<>();
        int x = startX + dx;
        int y = startY + dy;
        while (true) {
            Case c = plateau.getCase(x, y);
            if (c == null) break;
            if (c.isEmpty()) {
                res.add(c);
            } else {
                Piece other = c.getPiece();
                if (other != null && other.isBlanc() != piece.isBlanc()) {
                    res.add(c);
                }
                break;
            }
            x += dx;
            y += dy;
        }
        return res;
    }
}
