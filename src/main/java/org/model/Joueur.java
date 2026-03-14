package org.model;

import java.util.List;
import java.awt.Point;
import org.model.piece.Piece;
import org.model.plateau.Plateau;
import org.model.plateau.PlateauSingleton;

public abstract class Joueur {

    protected final Jeu jeu;

    public Joueur(Jeu jeu) {
        this.jeu = jeu;
    }

    public abstract Coup getCoup();

    /**
     * Vérifie si le joueur est en état d'échec.
     * @return true si le roi du joueur est en échec, false sinon.
     */
    public boolean estEnEchec() {
        // Récupérer la position du roi du joueur
        Piece roi = jeu.getEchiquier().getRoi(this);
        if (roi == null) {
            throw new IllegalStateException("Le roi du joueur est introuvable.");
        }

        // Vérifier si une pièce adverse peut atteindre la position du roi
        List<Piece> piecesAdverses = jeu.getEchiquier().getPiecesAdverses(this);
        for (Piece piece : piecesAdverses) {
            if (piece.getCaseAccessible().contains(roi.getCase())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Vérifie si le joueur a au moins un coup légal disponible.
     * @return true si le joueur a au moins un coup légal, false sinon.
     */
    public boolean aDesCoupsLegaux() {
        List<Piece> pieces = jeu.getEchiquier().getPieces(this);

        for (Piece piece : pieces) {
            if (piece == null) continue;

            List<org.model.plateau.Case> casesAccessibles = piece.getCaseAccessible();
            for (org.model.plateau.Case caseAccessible : casesAccessibles) {
                Point from = new Point(piece.getCase().getX(), piece.getCase().getY());
                Point to = new Point(caseAccessible.getX(), caseAccessible.getY());

                // Simuler le mouvement
                if (estCoupLegal(from, to)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Teste si un coup est légal (n'expose pas le roi à un échec).
     * @param from Position initiale
     * @param to Position finale
     * @return true si le coup est légal
     */
    private boolean estCoupLegal(Point from, Point to) {
        Plateau plateau = PlateauSingleton.INSTANCE;

        // Sauvegarder l'état du plateau
        Piece pieceAtFrom = plateau.getCase(from).getPiece();
        Piece pieceAtTo = plateau.getCase(to).getPiece();

        // Effectuer le mouvement temporaire
        plateau.getCase(from).setPiece(null);
        plateau.getCase(to).setPiece(pieceAtFrom);
        jeu.getEchiquier().syncFromPlateau(plateau);

        // Vérifier si le roi est en échec après le mouvement
        boolean enEchec = estEnEchec();

        // Restaurer l'état
        plateau.getCase(from).setPiece(pieceAtFrom);
        plateau.getCase(to).setPiece(pieceAtTo);
        jeu.getEchiquier().syncFromPlateau(plateau);

        return !enEchec;
    }

    /**
     * Détermine si le joueur joue avec les pièces blanches.
     * @return true si le joueur est blanc, false sinon.
     */
    public abstract boolean isBlanc();
}
