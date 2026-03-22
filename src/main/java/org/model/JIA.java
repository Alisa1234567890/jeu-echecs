package org.model;

import java.util.List;
import java.util.Random;

public class JIA extends Joueur {

    private final Jeu.Difficulty difficulty;
    private final Random random = new Random();

    public JIA(Jeu jeu, boolean blanc, String nom, Jeu.Difficulty difficulty) {
        super(jeu, blanc, nom);
        this.difficulty = difficulty;
    }

    public JIA(Jeu jeu, boolean blanc, Jeu.Difficulty difficulty) {
        this(jeu, blanc, blanc ? "IA Blanche" : "IA Noire", difficulty);
    }

    @Override
    public Coup getCoup() {
        try {
            if (difficulty != Jeu.Difficulty.HARD) {
                Thread.sleep(300);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }

        return switch (difficulty) {
            case EASY -> {
                List<Coup> coups = jeu.getLegalMoves(blanc);
                yield coups.isEmpty() ? null : coups.get(random.nextInt(coups.size()));
            }
            case MEDIUM, HARD -> jeu.chooseAiMove(blanc, difficulty);
        };
    }
}
