package org.model;

import java.util.Observable;

public class Jeu extends Observable implements Runnable {

    public Coup nextC;

    private Joueur joueur1;
    private Joueur joueur2;
    private boolean termine = false;

    public Jeu() {
        joueur1 = new Joueur(this);
        joueur2 = new Joueur(this);
        Plateau p = PlateauSingleton.INSTANCE;
        Roi r = new Roi(true);
        Dame d = new Dame(false);
        p.getCase(4, 0).setPiece(r);
        p.getCase(3, 7).setPiece(d);
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
            boolean ok = PlateauSingleton.INSTANCE.deplacer(c.dep, c.arr);
            if (!ok) {
            }
            setChanged();
            notifyAll();
            notifyObservers(c);
        }
    }
}
