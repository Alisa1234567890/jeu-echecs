package org.model;

import org.junit.jupiter.api.Test;

import java.awt.Point;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JeuRulesTest {

    private static Coup move(int fromRow, int fromCol, int toRow, int toCol) {
        return new Coup(new Point(fromRow, fromCol), new Point(toRow, toCol));
    }

    @Test
    void unJoueurEnEchecNePeutPasJouerUnCoupSansParerLechec() {
        Jeu jeu = new Jeu(Jeu.GameMode.HUMAN_VS_HUMAN, Jeu.Difficulty.EASY);
        try {
            assertTrue(jeu.appliquerCoup(move(6, 4, 4, 4))); // e2-e4
            assertTrue(jeu.appliquerCoup(move(1, 3, 3, 3))); // d7-d5
            assertTrue(jeu.appliquerCoup(move(4, 4, 3, 3))); // e4xd5
            assertTrue(jeu.appliquerCoup(move(0, 3, 3, 3))); // Qd8xd5
            assertTrue(jeu.appliquerCoup(move(7, 1, 5, 2))); // Nb1-c3
            assertTrue(jeu.appliquerCoup(move(3, 3, 2, 4))); // Qd5-e6+

            Coup coupInterdit = move(6, 0, 5, 0); // a2-a3, ne pare pas l'échec
            assertTrue(jeu.isKingInCheck(true));
            assertFalse(jeu.isLegalMove(coupInterdit, true));
            assertFalse(jeu.appliquerCoup(coupInterdit));
        } finally {
            jeu.stopGame();
        }
    }

    @Test
    void uneParadeLegaleResteAutoriseePendantLechec() {
        Jeu jeu = new Jeu(Jeu.GameMode.HUMAN_VS_HUMAN, Jeu.Difficulty.EASY);
        try {
            assertTrue(jeu.appliquerCoup(move(6, 4, 4, 4))); // e2-e4
            assertTrue(jeu.appliquerCoup(move(1, 3, 3, 3))); // d7-d5
            assertTrue(jeu.appliquerCoup(move(4, 4, 3, 3))); // e4xd5
            assertTrue(jeu.appliquerCoup(move(0, 3, 3, 3))); // Qd8xd5
            assertTrue(jeu.appliquerCoup(move(7, 1, 5, 2))); // Nb1-c3
            assertTrue(jeu.appliquerCoup(move(3, 3, 2, 4))); // Qd5-e6+

            Coup parade = move(7, 5, 6, 4); // Fou f1-e2 interpose
            assertTrue(jeu.isLegalMove(parade, true));
            assertTrue(jeu.appliquerCoup(parade));
            assertFalse(jeu.isKingInCheck(true));
        } finally {
            jeu.stopGame();
        }
    }
}

