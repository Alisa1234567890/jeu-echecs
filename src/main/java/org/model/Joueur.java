package org.model;

public abstract class Joueur {

    protected final Jeu jeu;
    protected final boolean blanc;
    protected final String nom;

    public Joueur(Jeu jeu, boolean blanc, String nom) {
        this.jeu = jeu;
        this.blanc = blanc;
        this.nom = nom == null ? (blanc ? "White" : "Black") : nom;
    }

    public abstract Coup getCoup();

    public boolean estEnEchec() {
        return jeu.isKingInCheck(this);
    }

    public boolean aDesCoupsLegaux() {
        return !jeu.getLegalMoves(blanc).isEmpty();
    }

    public boolean isBlanc() {
        return blanc;
    }

    public String getNom() {
        return nom;
    }
}
