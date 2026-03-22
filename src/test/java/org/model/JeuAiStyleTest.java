package org.model;

import org.junit.jupiter.api.Test;

import java.awt.Point;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JeuAiStyleTest {

    @Test
    void modeLabelExposeLeStyleIA() {
        Jeu jeu = new Jeu(Jeu.GameMode.HUMAN_VS_AI, Jeu.Difficulty.HARD, Jeu.AIStyle.AGRESSIF);
        try {
            assertEquals(Jeu.AIStyle.AGRESSIF, jeu.getAiStyle());
            assertTrue(jeu.getModeLabel().contains("Agressif"));
            assertTrue(jeu.getAiStyleDescription().contains("initiatives"));
        } finally {
            jeu.stopGame();
        }
    }

    @Test
    void scoreDeCoupVarieSelonLeStyleChoisi() {
        Coup e2e4 = new Coup(new Point(6, 4), new Point(4, 4));
        Jeu agressif = new Jeu(Jeu.GameMode.HUMAN_VS_AI, Jeu.Difficulty.MEDIUM, Jeu.AIStyle.AGRESSIF);
        Jeu prudent = new Jeu(Jeu.GameMode.HUMAN_VS_AI, Jeu.Difficulty.MEDIUM, Jeu.AIStyle.PRUDENT);
        try {
            int scoreAgressif = agressif.scoreMove(e2e4, true);
            int scorePrudent = prudent.scoreMove(e2e4, true);
            assertNotEquals(scoreAgressif, scorePrudent, "Le style IA doit influencer l'évaluation d'un même coup");
        } finally {
            agressif.stopGame();
            prudent.stopGame();
        }
    }

    @Test
    void iaRenvoieToujoursUnCoupLegalQuelQueSoitLeStyle() {
        for (Jeu.AIStyle style : Jeu.AIStyle.values()) {
            Jeu jeu = new Jeu(Jeu.GameMode.HUMAN_VS_AI, Jeu.Difficulty.MEDIUM, style);
            try {
                Coup coup = jeu.chooseAiMove(false, Jeu.Difficulty.MEDIUM);
                assertNotNull(coup, "L'IA doit proposer un coup au début de partie pour le style " + style);
                assertTrue(jeu.isLegalMove(coup, false), "Le coup choisi doit être légal pour le style " + style);
            } finally {
                jeu.stopGame();
            }
        }
    }
}

