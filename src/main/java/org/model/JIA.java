package org.model;

public class JIA extends Joueur {

    private boolean blanc;

    public JIA(Jeu jeu, boolean blanc) {
        super(jeu);
        this.blanc = blanc;
    }

    @Override
    public Coup getCoup() {
        return null;
    }

    @Override
    public boolean isBlanc() {
        return blanc;
    }
}
