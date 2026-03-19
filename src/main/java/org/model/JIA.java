package org.model;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class JIA extends Joueur {

    private final boolean blanc;
    private final Jeu.Difficulty difficulty;
    private final Random random = new Random();

    public JIA(Jeu jeu, boolean blanc) {
        this(jeu, blanc, Jeu.Difficulty.EASY);
    }

    public JIA(Jeu jeu, boolean blanc, Jeu.Difficulty difficulty) {
        super(jeu);
        this.blanc = blanc;
        this.difficulty = difficulty;
    }

    @Override
    public Coup getCoup() {
        try {
            Thread.sleep(difficulty == Jeu.Difficulty.HARD ? 250L : 400L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }

        List<Coup> legalMoves = new ArrayList<>();
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                if (jeu.getEchiquier().getPiece(row, col) == null
                        || jeu.getEchiquier().getPiece(row, col).isBlanc() != blanc) {
                    continue;
                }
                for (Point target : jeu.getLegalDestinations(row, col)) {
                    legalMoves.add(new Coup(new Point(row, col), target));
                }
            }
        }
        if (legalMoves.isEmpty()) {
            return null;
        }
        return legalMoves.get(random.nextInt(legalMoves.size()));
    }

    @Override
    public boolean isBlanc() {
        return blanc;
    }
}
