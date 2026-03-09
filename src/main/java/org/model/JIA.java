package org.model;

public class JIA extends Joueur {

    public JIA(Jeu jeu) {
        super(jeu);
    }

    @Override
    public Coup getCoup() {
        // IA très basique: attend pas et renvoie null (aucun coup)
        return null;
    }
}
