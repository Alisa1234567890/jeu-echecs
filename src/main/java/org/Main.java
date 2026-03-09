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
        // also create the VC view (board UI) so the board and clicks work
        VC vue = new VC(j);
        // embed the VC panel into MF and only show MF as a single window
        mf.setVC(vue);
        j.addObserver(vue); // ensure VC receives updates
        mf.setVisible(true);
        new Thread(j).start();
    }
}
