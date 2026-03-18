package org.model;

public abstract class Joueur {

    protected final Jeu jeu;
    protected final boolean blanc;
    protected final String nom;

    public Joueur(Jeu jeu, boolean blanc, String nom) {
        this.jeu = jeu;
        this.blanc = blanc;
        this.nom = nom;
    }

    public abstract Coup getCoup();

    public boolean isBlanc() {
        return blanc;
    }

    public String getNom() {
        return nom;
    }
}
