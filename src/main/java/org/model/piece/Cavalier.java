package org.model.piece;

import org.model.plateau.Case;
import org.model.plateau.Direction;
import org.model.plateau.Plateau;
import org.model.plateau.PlateauSingleton;

import java.util.ArrayList;

public class Cavalier extends Piece {

    public Cavalier(String color) {
        super(color);
    }

    @Override
    public ArrayList<Case> getCaseAccessible() {
        ArrayList<Case> res = new ArrayList<>();
        if (position == null) return res;

        int x = position.getX();
        int y = position.getY();
        Plateau plateau = PlateauSingleton.INSTANCE;

        for (Direction d : Direction.sauts()) {
            Case c = plateau.getCase(x + d.dx, y + d.dy);
            if (c != null && (c.isEmpty() || c.getPiece().isBlanc() != this.isBlanc())) {
                res.add(c);
            }
        }
        return res;
    }

    @Override
    public String getImageName() {
        return getColor().equals("white") ? "wN" : "bN";
    }
}
