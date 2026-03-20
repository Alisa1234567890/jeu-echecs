package org;

import org.controller.MF;
import org.controller.VC;
import org.model.Jeu;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.Color;
import java.awt.Font;

public class Main {

    public static void main(String[] args) {
        applyBlueTheme();
        SwingUtilities.invokeLater(Main::launchWithPrompts);
    }

    public static void restartSameGame(Jeu.GameMode mode, Jeu.Difficulty difficulty, Jeu jeuCourant, MF frameCourante) {
        if (jeuCourant != null) {
            jeuCourant.stopGame();
        }
        if (frameCourante != null) {
            frameCourante.dispose();
        }
        launchGame(mode, difficulty);
    }

    public static void relaunchWithPrompts(Jeu jeuCourant, MF frameCourante) {
        if (jeuCourant != null) {
            jeuCourant.stopGame();
        }
        if (frameCourante != null) {
            frameCourante.dispose();
        }
        launchWithPrompts();
    }

    private static void launchWithPrompts() {
        Jeu.GameMode mode = chooseMode();
        Jeu.Difficulty difficulty = mode == Jeu.GameMode.HUMAN_VS_AI
                ? chooseDifficulty()
                : Jeu.Difficulty.EASY;
        launchGame(mode, difficulty);
    }

    private static void launchGame(Jeu.GameMode mode, Jeu.Difficulty difficulty) {
        Jeu jeu = new Jeu(mode, difficulty);
        MF frame = new MF();
        frame.setJeu(jeu);
        frame.setSessionActions(
                () -> restartSameGame(mode, difficulty, jeu, frame),
                () -> relaunchWithPrompts(jeu, frame)
        );
        jeu.addObserver(frame);

        VC vue = new VC(jeu);
        frame.setVC(vue);
        jeu.addObserver(vue);
        frame.setVisible(true);
    }

    private static void applyBlueTheme() {
        UIManager.put("Panel.background", new Color(228, 239, 252));
        UIManager.put("OptionPane.background", new Color(228, 239, 252));
        UIManager.put("OptionPane.messageForeground", new Color(27, 59, 107));
        UIManager.put("Button.background", new Color(101, 149, 214));
        UIManager.put("Button.foreground", Color.WHITE);
        UIManager.put("Button.select", new Color(72, 122, 191));
        UIManager.put("Button.focus", new Color(72, 122, 191));
        UIManager.put("Label.foreground", new Color(27, 59, 107));
        UIManager.put("Label.font", new Font("SansSerif", Font.BOLD, 13));
        UIManager.put("Button.font", new Font("SansSerif", Font.BOLD, 13));
        UIManager.put("OptionPane.buttonAreaBorder", null);
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
