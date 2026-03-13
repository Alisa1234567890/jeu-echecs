package org.model.piece;

import org.model.plateau.Case;
import org.model.plateau.Plateau;
import org.model.plateau.PlateauSingleton;
import org.model.plateau.DecorateurCasesEnLigne;
import org.model.plateau.DecorateurCasesEnDiagonale;

import java.util.ArrayList;

public class Queen extends Piece {

    public Queen(String color) {
        super(color, new DecorateurCasesEnLigne(PlateauSingleton.INSTANCE,
                new DecorateurCasesEnDiagonale(PlateauSingleton.INSTANCE)));
    }

    public ArrayList<Case> getCaseAccessible() {
        ArrayList<Case> res = new ArrayList<>();
        if (position == null) return res;
        int x = position.getX();
        int y = position.getY();
        Plateau plateau = findPlateau();
        if (plateau == null) return res;
        int[][] dirs = { {1,0},{-1,0},{0,1},{0,-1},{1,1},{1,-1},{-1,1},{-1,-1} };
        for (int[] d : dirs) {
            int nx = x + d[0];
            int ny = y + d[1];
            while (true) {
                Case c = plateau.getCase(nx, ny);
                if (c == null) break;
                if (c.isEmpty()) {
                    res.add(c);
                } else {
                    if (c.getPiece().isBlanc() != this.blanc) res.add(c);
                    break;
                }
                nx += d[0];
                ny += d[1];
            }
        }
        return res;
    }

    private Plateau findPlateau() {
        return PlateauSingleton.INSTANCE;
    }
}
