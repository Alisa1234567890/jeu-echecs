package org;
import org.controller.ControllerJeu;
import org.view.VuePlateau;
import org.model.jeu.Jeu;
import org.view.VueConsole;
public class Main {
    public static void main(String[] args) {
        Jeu j = new Jeu();
        ControllerJeu mf = new ControllerJeu();
        mf.initJeu(j);
        j.addObserver(mf);
        VuePlateau vue = new VuePlateau(j);
        mf.initVC(vue);
        j.addObserver(vue);
        mf.setVisible(true);
        VueConsole console = new VueConsole(j);
        j.addObserver(console);
    }
}
