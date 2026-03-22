package org.model.piece;

import org.model.plateau.Case;
import org.model.plateau.Direction;
import org.model.plateau.Plateau;
import org.model.plateau.PlateauSingleton;
import org.model.plateau.DecorateurCasesEnLigne;
import org.model.plateau.DecorateurCasesEnDiagonale;

import java.util.ArrayList;

public class Dame extends Piece {

    public Dame(String color) {
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
        for (Direction d : Direction.glissement()) {
            int nx = x + d.dx;
            int ny = y + d.dy;
            while (true) {
                Case c = plateau.getCase(nx, ny);
                if (c == null) break;
                if (c.isEmpty()) {
                    res.add(c);
                } else {
                    if (c.getPiece().isBlanc() != this.blanc) res.add(c);
                    break;
                }
                nx += d.dx;
                ny += d.dy;
            }
        }
        return res;
    }

    private Plateau findPlateau() {
        return PlateauSingleton.INSTANCE;
    }
}
