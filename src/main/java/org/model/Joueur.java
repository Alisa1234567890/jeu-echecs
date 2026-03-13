package org.model;

import java.util.List;
import org.model.piece.Piece;

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
     * Détermine si le joueur joue avec les pièces blanches.
     * @return true si le joueur est blanc, false sinon.
     */
    public abstract boolean isBlanc();
}
