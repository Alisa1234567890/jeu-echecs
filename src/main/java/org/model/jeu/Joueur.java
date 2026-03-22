package org.model.jeu;

import java.util.List;
import java.awt.Point;

import org.model.piece.Piece;
import org.model.plateau.Plateau;

public abstract class Joueur {

    protected final Jeu jeu;

    public Joueur(Jeu jeu) {
        this.jeu = jeu;
    }

    public abstract Coup getCoup();


    public boolean estEnEchec() {
        Piece roi = jeu.getEchiquier().getRoi(this);
        if (roi == null) {
            throw new IllegalStateException("Le roi du joueur est introuvable.");
        }


        List<Piece> piecesAdverses = jeu.getEchiquier().getPiecesAdverses(this);
        for (Piece piece : piecesAdverses) {
            if (piece.getCaseAccessible().contains(roi.getCase())) {
                return true;
            }
        }
        return false;
    }


    public boolean aDesCoupsLegaux() {
        List<Piece> pieces = jeu.getEchiquier().getPieces(this);

        for (Piece piece : pieces) {
            if (piece == null) continue;

            List<org.model.plateau.Case> casesAccessibles = piece.getCaseAccessible();
            for (org.model.plateau.Case caseAccessible : casesAccessibles) {
                Point from = new Point(piece.getCase().getX(), piece.getCase().getY());
                Point to = new Point(caseAccessible.getX(), caseAccessible.getY());

                if (estCoupLegal(from, to)) {
                    return true;
                }
            }
        }

        return false;
    }


    private boolean estCoupLegal(Point from, Point to) {
        Plateau plateau = jeu.getPlateau();


        Piece pieceAtFrom = plateau.getCase(from).getPiece();
        Piece pieceAtTo = plateau.getCase(to).getPiece();

        plateau.getCase(from).setPiece(null);
        plateau.getCase(to).setPiece(pieceAtFrom);
        jeu.getEchiquier().syncFromPlateau(plateau);

        boolean enEchec = estEnEchec();

        plateau.getCase(from).setPiece(pieceAtFrom);
        plateau.getCase(to).setPiece(pieceAtTo);
        jeu.getEchiquier().syncFromPlateau(plateau);

        return !enEchec;
    }

    public abstract boolean isBlanc();
}
