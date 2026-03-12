package org.model.plateau;

import org.model.Piece;

import java.util.ArrayList;


public abstract class DecorateurCasesAccessibles {

    protected final Plateau plateau;
    protected final DecorateurCasesAccessibles base;

    public DecorateurCasesAccessibles(Plateau plateau) {
        this(plateau, null);
    }

    public DecorateurCasesAccessibles(Plateau plateau, DecorateurCasesAccessibles base) {
        this.plateau = plateau;
        this.base = base;
    }

    /**
     * Public entry: combine this decorator's cases with base decorator (if any).
     */
    public ArrayList<Case> getAccessibleCases(Piece piece) {
        ArrayList<Case> res = getMesCasesAccessibles(piece);
        if (res == null) res = new ArrayList<>();
        if (base != null) {
            ArrayList<Case> baseList = base.getAccessibleCases(piece);
            if (baseList != null && !baseList.isEmpty()) {
                res.addAll(baseList);
            }
        }
        return res;
    }

    /**
     * Implemented by concrete decorators: return the cases this decorator contributes.
     */
    protected abstract ArrayList<Case> getMesCasesAccessibles(Piece piece);

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
