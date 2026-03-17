package org.model;

public class JHumain extends Joueur {

    private boolean blanc;

    public JHumain(Jeu jeu, boolean blanc) {
        super(jeu);
        this.blanc = blanc;
    }

    @Override
    public Coup getCoup() {
        return jeu.attendreCoup();
    }

    @Override
    public boolean isBlanc() {
        return blanc;
    }
}
