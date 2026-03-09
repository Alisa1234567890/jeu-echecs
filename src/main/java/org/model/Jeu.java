package org.model;

import org.model.plateau.Plateau;
import org.model.plateau.PlateauSingleton;

import java.util.Observable;

public class Jeu extends Observable implements Runnable {

    public Coup nextC;

    private Joueur joueur1;
    private Joueur joueur2;
    private boolean termine = false;

    private EchiquierModele echiquier;

    public Jeu() {
        echiquier = new EchiquierModele();
        joueur1 = new JHumain(this);
        joueur2 = new JHumain(this);
        Plateau p = PlateauSingleton.INSTANCE;
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece piece = echiquier.getPiece(r, c);
                p.getCase(r, c).setPiece(piece);
            }
        }
        new Thread(this, "Jeu-Thread").start();
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
            if (c != null) {
                appliquerCoup(c);
            }
        }
    }

    public boolean partieTerminee() {
        return termine;
    }

    public Joueur getJoueurSuivant() {
        return joueur1;
    }

    public void appliquerCoup(Coup c) {
        if (c == null) return;
        synchronized (this) {
            nextC = c;
            System.out.println("Attempting move: " + c.dep + " -> " + c.arr);
            boolean ok = PlateauSingleton.INSTANCE.deplacer(c.dep, c.arr);
            System.out.println("Move result: " + ok);
            if (ok) {
                echiquier.syncFromPlateau(PlateauSingleton.INSTANCE);
            }
            setChanged();
            this.notifyAll();
            notifyObservers(c);
        }
    }

    public void setCoup(Coup c) {
        if (c == null) return;
        synchronized (this) {
            this.nextC = c;
            this.notifyAll();
            setChanged();
            notifyObservers(c);
        }
    }
}
