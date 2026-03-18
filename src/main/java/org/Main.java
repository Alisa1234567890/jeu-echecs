package org;

import org.controller.MF;
import org.controller.VC;
import org.model.Jeu;

import javax.swing.JOptionPane;

public class Main {
    public static void main(String[] args) {
        Jeu.GameMode mode = chooseMode();
        Jeu.Difficulty difficulty = mode == Jeu.GameMode.HUMAN_VS_AI
                ? chooseDifficulty()
                : Jeu.Difficulty.EASY;

        Jeu j = new Jeu(mode, difficulty);
        MF mf = new MF();
        mf.setJeu(j);
        j.addObserver(mf);
        VC vue = new VC(j);
        mf.setVC(vue);
        j.addObserver(vue);
        mf.setVisible(true);
    }

    private static Jeu.GameMode chooseMode() {
        Object[] options = {"Jouer contre un ami", "Jouer contre l'IA"};
        int choice = JOptionPane.showOptionDialog(
                null,
                "Choisissez le mode de jeu",
                "Mode de jeu",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]
        );
        return choice == 1 ? Jeu.GameMode.HUMAN_VS_AI : Jeu.GameMode.HUMAN_VS_HUMAN;
    }

    private static Jeu.Difficulty chooseDifficulty() {
        Object[] options = {"Easy", "Medium", "Hard"};
        int choice = JOptionPane.showOptionDialog(
                null,
                "Choisissez le niveau de l'IA",
                "Niveau IA",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[1]
        );
        if (choice == 2) {
            return Jeu.Difficulty.HARD;
        }
        if (choice == 1) {
            return Jeu.Difficulty.MEDIUM;
        }
        return Jeu.Difficulty.EASY;
    }
}
