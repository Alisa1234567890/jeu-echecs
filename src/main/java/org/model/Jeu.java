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
        // initialize plateau singleton and some pieces for demonstration
        Plateau p = PlateauSingleton.INSTANCE;
        // place two pieces as example
        // Roi r = new Roi(true);
        // Dame d = new Dame(false);
        // p.getCase(4, 0).setPiece(r);
        // p.getCase(3, 7).setPiece(d);
        // use renamed English classes King/Queen
        King r = new King("white");
        Queen d = new Queen("black");
        p.getCase(4, 0).setPiece(r);
        p.getCase(3, 7).setPiece(d);
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
            // try to move on plateau
            boolean ok = PlateauSingleton.INSTANCE.deplacer(c.dep, c.arr);
            if (!ok) {
                // invalid move: could set flags or throw; for now just ignore
            }
            setChanged();
            notifyAll();
            notifyObservers(c);
        }
    }
}
