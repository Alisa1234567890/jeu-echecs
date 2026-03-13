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

    @Override
    public boolean isBlanc() {
        // Implémentation spécifique pour JHumain
        return true; // Exemple : retourne true pour les pièces blanches
    }
}
