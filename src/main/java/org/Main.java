package org;

import org.controller.VC;
import org.model.Jeu;

public class Main {
    public static void main(String[] args) {
        Jeu j = new Jeu();
        VC vue = new VC(j);
        vue.setVisible(true);
        new Thread(j).start();
    }
}
