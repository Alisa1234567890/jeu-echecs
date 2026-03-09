package org.model;

import java.awt.Point;

public class Plateau {

    private final int size = 8;
    private final Case[][] cases;

    public Plateau() {
        cases = new Case[size][size];
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                cases[x][y] = new Case(x, y);
            }
        }
    }

    public Case getCase(int x, int y) {
        if (x < 0 || x >= size || y < 0 || y >= size) return null;
        return cases[x][y];
    }

    public Case getCase(Point p) {
        return getCase(p.x, p.y);
    }

    public boolean deplacer(Point dep, Point arr) {
        Case cDep = getCase(dep);
        Case cArr = getCase(arr);
        if (cDep == null || cArr == null) return false;
        if (cDep.isEmpty()) return false;
        Piece p = cDep.getPiece();
        java.util.ArrayList<Case> acces = p.getCaseAccessible();
        boolean allowed = false;
        for (Case c : acces) {
            if (c.getX() == arr.x && c.getY() == arr.y) {
                allowed = true;
                break;
            }
        }
        // fallback to simple isValidMove if getCaseAccessible wasn't implemented for this piece
        if (!allowed) {
            try {
                allowed = p.isValidMove(dep.x, dep.y, arr.x, arr.y);
            } catch (Exception ignored) {
            }
        }
        if (!allowed) return false;
        cDep.setPiece(null);
        cArr.setPiece(p);
        return true;
    }
}
