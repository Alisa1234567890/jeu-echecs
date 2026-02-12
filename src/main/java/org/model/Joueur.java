package org.model;

public class Joueur {

    private final Jeu jeu;

    public Joueur(Jeu jeu) {
        this.jeu = jeu;
    }

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
