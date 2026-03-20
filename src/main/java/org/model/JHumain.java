package org.model;

public class JHumain extends Joueur {

    public JHumain(Jeu jeu, boolean blanc) {
        this(jeu, blanc, blanc ? "White" : "Black");
    }

    public JHumain(Jeu jeu, boolean blanc, String nom) {
        super(jeu, blanc, nom);
    }

    @Override
    public Coup getCoup() {
        return jeu.attendreCoup(blanc);
    }
}
