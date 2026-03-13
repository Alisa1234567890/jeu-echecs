package org.model;

public class JHumain extends Joueur {

    private boolean blanc;

    public JHumain(Jeu jeu, boolean blanc) {
        super(jeu);
        this.blanc = blanc;
    }

    @Override
    public Coup getCoup() {
        synchronized (jeu) {
            try {
                jeu.wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return jeu.nextC;
        }
    }

    @Override
    public boolean isBlanc() {
        return blanc;
    }
}
