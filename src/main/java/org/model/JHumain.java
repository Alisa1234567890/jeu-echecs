package org.model;

public class JHumain extends Joueur {

    public JHumain(Jeu jeu, boolean blanc, String nom) {
        super(jeu, blanc, nom);
    }

    @Override
    public Coup getCoup() {
        synchronized (jeu) {
            while (!jeu.partieTerminee()) {
                Coup coup = jeu.consumePendingHumanMove(blanc);
                if (coup != null) {
                    return coup;
                }
                try {
                    jeu.wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return null;
                }
            }
            return null;
        }
    }
}
