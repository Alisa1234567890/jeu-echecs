package org.model.piece;

import org.model.plateau.Case;
import org.model.plateau.Plateau;
import org.model.plateau.PlateauSingleton;

import java.util.ArrayList;

public class King extends Piece {

    public King(String color) {
        super(color);
    }

    public ArrayList<Case> getCaseAccessible() {
        ArrayList<Case> res = new ArrayList<>();
        if (position == null) return res;

        int x = position.getX();
        int y = position.getY();
        Plateau plateau = findPlateau();
        if (plateau == null) return res;

        // 1. Déplacements standards (1 case autour)
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx == 0 && dy == 0) continue;
                Case c = plateau.getCase(x + dx, y + dy);
                if (c != null && (c.isEmpty() || c.getPiece().isBlanc() != this.blanc)) {
                    res.add(c);
                }
            }
        }

        // 2. AJOUT POUR LE ROQUE : Autoriser le saut de 2 cases
        // On ajoute les destinations potentielles (colonnes 2 et 6)
        // La validation réelle (échec, pièces entre deux) se fera dans Jeu.java

        // Petit roque (vers la droite, colonne 6)
        Case petitRoque = plateau.getCase(x, 6);
        if (petitRoque != null && petitRoque.isEmpty()) {
            res.add(petitRoque);
        }

        // Grand roque (vers la gauche, colonne 2)
        Case grandRoque = plateau.getCase(x, 2);
        if (grandRoque != null && grandRoque.isEmpty()) {
            res.add(grandRoque);
        }

        return res;
    }

    private Plateau findPlateau() {
        return PlateauSingleton.INSTANCE;
    }
}
