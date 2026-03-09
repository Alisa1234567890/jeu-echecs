package org.model;

public abstract class Joueur {

    protected final Jeu jeu;

    public Joueur(Jeu jeu) {
        this.jeu = jeu;
    }

    public abstract Coup getCoup();
}
