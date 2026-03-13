package org.model;

public class JIA extends Joueur {

    public JIA(Jeu jeu) {
        super(jeu);
    }

    @Override
    public Coup getCoup() {
        return null;
    }

    @Override
    public boolean isBlanc() {
        // Implémentation spécifique pour JIA
        return false; // Exemple : retourne false pour les pièces noires
    }
}
