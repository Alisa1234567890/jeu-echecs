package org.model;

public class JHumain extends Joueur {

    public JHumain(Jeu jeu) {
        super(jeu);
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
}
