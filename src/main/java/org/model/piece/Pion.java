package org.model.piece;

import org.model.plateau.Case;
import org.model.plateau.Direction;
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


        // Direction d'avancement selon la couleur
        Direction moveDir    = isBlanc() ? Direction.HAUT    : Direction.BAS;
        Direction diagGauche = isBlanc() ? Direction.HAUT_GAUCHE : Direction.BAS_GAUCHE;
        Direction diagDroite = isBlanc() ? Direction.HAUT_DROITE : Direction.BAS_DROITE;

        // Avancer d'une case
        Case devant = plateau.getCase(x + moveDir.dx, y + moveDir.dy);
        if (devant != null && devant.isEmpty()) {
            res.add(devant);

            boolean estAuDepart = (isBlanc() && x == 6) || (!isBlanc() && x == 1);
            if (estAuDepart) {
                Case devant2 = plateau.getCase(x + (2 * moveDir.dx), y + (2 * moveDir.dy));
                if (devant2 != null && devant2.isEmpty()) {
                    res.add(devant2);
                }
            }
        }

        // Captures en diagonale
        for (Direction diag : new Direction[]{diagGauche, diagDroite}) {
            Case c = plateau.getCase(x + diag.dx, y + diag.dy);
            if (c != null && !c.isEmpty() && c.getPiece().isBlanc() != this.isBlanc()) {
                res.add(c);
            }
        }

        // En passant
        Case epTarget = plateau.getEnPassantTarget();
        if (epTarget != null) {
            int epX = epTarget.getX();
            int epY = epTarget.getY();
            if (epX == x + moveDir.dx && (epY == y - 1 || epY == y + 1)) {
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
