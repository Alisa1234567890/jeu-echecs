package org;

import org.controller.MF;
import org.controller.VC;
import org.model.Jeu;
import org.view.VueConsole;

public class Main {
    public static void main(String[] args) {
        Jeu j = new Jeu();

        // ── Vue graphique ────────────────────────────────────────────────
        MF mf = new MF();
        mf.setJeu(j);
        j.addObserver(mf);
        VC vue = new VC(j);
        mf.setVC(vue);
        j.addObserver(vue);
        mf.setVisible(true);

        // ── Vue console (même modèle, aucune modification du modèle) ─────
        VueConsole console = new VueConsole(j);
        j.addObserver(console);
    }
}


