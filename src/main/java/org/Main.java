package org;

import org.controller.MF;
import org.controller.VC;
import org.model.Jeu;

public class Main {
    public static void main(String[] args) {
        Jeu j = new Jeu();
        MF mf = new MF();
        mf.setJeu(j);
        j.addObserver(mf);
        VC vue = new VC(j);
        mf.setVC(vue);
        j.addObserver(vue);
        mf.setVisible(true);
        // thread du jeu démarré automatiquement dans le constructeur de Jeu
    }
}
