package org.model.piece;

import org.model.plateau.Case;
import org.model.plateau.Plateau;
import org.model.plateau.PlateauSingleton;

import java.util.ArrayList;

public class Pion extends Piece {

    public Pion(String color) {
        super(color);
    }

    @Override
    public ArrayList<Case> getCaseAccessible() {
        ArrayList<Case> res = new ArrayList<>();
        if (position == null) return res;

        int x = position.getX();
        int y = position.getY();
        Plateau plateau = PlateauSingleton.INSTANCE;


        int dir = isBlanc() ? -1 : 1;


        Case devant = plateau.getCase(x + dir, y);
        if (devant != null && devant.isEmpty()) {
            res.add(devant);

            boolean estAuDepart = (isBlanc() && x == 6) || (!isBlanc() && x == 1);
            if (estAuDepart) {
                Case devant2 = plateau.getCase(x + (2 * dir), y);
                if (devant2 != null && devant2.isEmpty()) {
                    res.add(devant2);
                }
            }
        }


        // Diagonal
        int[] colonnesDiagonales = {y - 1, y + 1};
        for (int c : colonnesDiagonales) {
            Case diag = plateau.getCase(x + dir, c);
            if (diag != null && !diag.isEmpty() && diag.getPiece().isBlanc() != this.isBlanc()) {
                res.add(diag);
            }
        }


        // En passant
        Case epTarget = plateau.getEnPassantTarget();
        if (epTarget != null) {
            int epX = epTarget.getX();
            int epY = epTarget.getY();
            if (epX == x + dir && (epY == y - 1 || epY == y + 1)) {
                if (!res.contains(epTarget)) {
                    res.add(epTarget);
                }
            }
        }

        return res;
    }

    @Override
    public String getImageName() {
        return getColor().equals("white") ? "wP" : "bP";
    }
}
