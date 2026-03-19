package org.model.piece;

import org.model.plateau.Case;
import org.model.plateau.Plateau;
import org.model.plateau.PlateauSingleton;

import java.util.ArrayList;

public class Pawn extends Piece {

    public Pawn(String color) {
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


        // Forward move (only if square is empty)
        Case devant = plateau.getCase(x + dir, y);
        if (devant != null && devant.isEmpty()) {
            res.add(devant);


            // Double push from starting rank
            boolean estAuDepart = (isBlanc() && x == 6) || (!isBlanc() && x == 1);
            if (estAuDepart) {
                Case devant2 = plateau.getCase(x + (2 * dir), y);
                if (devant2 != null && devant2.isEmpty()) {
                    res.add(devant2);
                }
            }
        }


        // Diagonal captures (only if enemy piece present)
        int[] colonnesDiagonales = {y - 1, y + 1};
        for (int c : colonnesDiagonales) {
            Case diag = plateau.getCase(x + dir, c);
            if (diag != null && !diag.isEmpty() && diag.getPiece().isBlanc() != this.isBlanc()) {
                res.add(diag);
            }
        }


        // En passant: add target square if it is diagonally forward from this pawn
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
    public boolean isValidMove(int startRow, int startCol,
                               int endRow, int endCol) {

        int direction = getColor().equals("white") ? -1 : 1;

        if (startCol == endCol && endRow == startRow + direction) {
            return true;
        }

        if (startCol == endCol &&
                ((getColor().equals("white") && startRow == 6 && endRow == 4) ||
                        (getColor().equals("black") && startRow == 1 && endRow == 3))) {
            return true;
        }

        return false;
    }

    @Override
    public String getImageName() {
        return getColor().equals("white") ? "wP" : "bP";
    }
}
