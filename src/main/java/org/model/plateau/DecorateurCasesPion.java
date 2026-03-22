package org.model.plateau;

import org.model.piece.Piece;

import java.util.ArrayList;

public class DecorateurCasesPion extends DecorateurCasesAccessibles {

    public DecorateurCasesPion(Plateau plateau) {
        super(plateau);
    }

    @Override
    protected ArrayList<Case> getMesCasesAccessibles(Piece piece) {
        ArrayList<Case> res = new ArrayList<>();
        if (piece.getCase() == null) return res;

        int x = piece.getCase().getX();
        int y = piece.getCase().getY();

        // Direction d'avancement selon la couleur
        Direction moveDir    = piece.isBlanc() ? Direction.HAUT        : Direction.BAS;
        Direction diagGauche = piece.isBlanc() ? Direction.HAUT_GAUCHE : Direction.BAS_GAUCHE;
        Direction diagDroite = piece.isBlanc() ? Direction.HAUT_DROITE : Direction.BAS_DROITE;

        // Avancer d'une case
        Case devant = plateau.getCase(x + moveDir.dx, y + moveDir.dy);
        if (devant != null && devant.isEmpty()) {
            res.add(devant);

            // Double pas depuis la ligne de départ
            boolean estAuDepart = (piece.isBlanc() && x == 6) || (!piece.isBlanc() && x == 1);
            if (estAuDepart) {
                Case devant2 = plateau.getCase(x + 2 * moveDir.dx, y + 2 * moveDir.dy);
                if (devant2 != null && devant2.isEmpty()) {
                    res.add(devant2);
                }
            }
        }

        // Captures en diagonale
        for (Direction diag : new Direction[]{diagGauche, diagDroite}) {
            Case c = plateau.getCase(x + diag.dx, y + diag.dy);
            if (c != null && !c.isEmpty() && c.getPiece().isBlanc() != piece.isBlanc()) {
                res.add(c);
            }
        }

        // Prise en passant
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
}

