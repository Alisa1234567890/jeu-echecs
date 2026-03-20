package org.model.piece;

import org.model.plateau.Case;
import org.model.plateau.Plateau;
import org.model.plateau.PlateauSingleton;

import java.util.ArrayList;

public class Knight extends Piece {

    public Knight(String color) {
        super(color);
    }

    @Override
    public ArrayList<Case> getCasesAccessibles() {
        ArrayList<Case> res = new ArrayList<>();
        if (position == null) return res;

        int x = position.getX();
        int y = position.getY();
        Plateau plateau = PlateauSingleton.INSTANCE;


        int[][] sauts = {
                {-2, -1}, {-2, 1}, {2, -1}, {2, 1},
                {-1, -2}, {-1, 2}, {1, -2}, {1, 2}
        };

        for (int[] s : sauts) {
            Case c = plateau.getCase(x + s[0], y + s[1]);
            if (c != null) {
                // Case vide OU pièce ennemie
                if (c.isEmpty() || c.getPiece().isBlanc() != this.isBlanc()) {
                    res.add(c);
                }
            }
        }
        return res;
    }

    @Override
    public boolean isValidMove(int startRow, int startCol,
                               int endRow, int endCol) {

        int rowDiff = Math.abs(endRow - startRow);
        int colDiff = Math.abs(endCol - startCol);

        return (rowDiff == 2 && colDiff == 1) ||
                (rowDiff == 1 && colDiff == 2);
    }

    @Override
    public String getImageName() {
        return getColor().equals("white") ? "wN" : "bN";
    }
}
