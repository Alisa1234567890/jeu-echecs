package org.model;

public abstract class Joueur {

    protected final Jeu jeu;

    public Joueur(Jeu jeu) {
        this.jeu = jeu;
    }

    // doit être implémentée par JHumain et JIA
    public abstract Coup getCoup();
}
