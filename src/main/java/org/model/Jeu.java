package org.model;

import java.util.Observable;

public class Jeu extends Observable implements Runnable {

    public Coup nextC;

    private Joueur joueur1;
    private Joueur joueur2;
    private boolean termine = false;

    private EchiquierModele echiquier;

    public Jeu() {
        echiquier = new EchiquierModele();
        joueur1 = new Joueur(this);
        joueur2 = new Joueur(this);
    }

    public EchiquierModele getEchiquier() {
        return echiquier;
    }

    @Override
    public void run() {
        jouerPartie();
    }

    public void jouerPartie() {
        while (!partieTerminee()) {
            Joueur js = getJoueurSuivant();
            Coup c = js.getCoup();
            appliquerCoup(c);
        }
    }

    public boolean partieTerminee() {
        return termine;
    }

    public Joueur getJoueurSuivant() {
        return joueur1;
    }

    public void appliquerCoup(Coup c) {
        synchronized (this) {
            nextC = c;
            setChanged();
            notifyObservers(c);
        }
    }
}
