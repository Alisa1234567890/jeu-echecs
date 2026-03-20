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
        this(jeu, blanc, blanc ? "White AI" : "Black AI", difficulty);
    }

    @Override
    public Coup getCoup() {
        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }

        List<Coup> coups = jeu.getLegalMoves(blanc);
        if (coups.isEmpty()) {
            return null;
        }

        return switch (difficulty) {
            case EASY -> coups.get(random.nextInt(coups.size()));
            case MEDIUM -> chooseBestImmediateMove(coups);
            case HARD -> chooseBestStrategicMove(coups);
        };
    }

    private Coup chooseBestImmediateMove(List<Coup> coups) {
        Coup best = null;
        int bestScore = Integer.MIN_VALUE;
        for (Coup coup : coups) {
            int score = jeu.scoreMove(coup, blanc);
            if (score > bestScore) {
                bestScore = score;
                best = coup;
            }
        }
        return best != null ? best : coups.get(0);
    }

    private Coup chooseBestStrategicMove(List<Coup> coups) {
        Coup best = null;
        int bestScore = Integer.MIN_VALUE;
        for (Coup coup : coups) {
            int score = jeu.scoreMove(coup, blanc);
            score += evaluateDestinationPressure(coup);
            if (score > bestScore) {
                bestScore = score;
                best = coup;
            }
        }
        return best != null ? best : chooseBestImmediateMove(coups);
    }

    private int evaluateDestinationPressure(Coup coup) {
        int pressure = 0;
        for (Coup opponentMove : jeu.getLegalMoves(!blanc)) {
            if (opponentMove.arr.x == coup.arr.x && opponentMove.arr.y == coup.arr.y) {
                pressure -= jeu.getPieceValueAt(coup.dep.x, coup.dep.y);
            }
        }
        return pressure == 0 ? 2 : pressure;
    }
}
