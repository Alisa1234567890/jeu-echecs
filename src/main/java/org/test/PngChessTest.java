package org.test;

import org.model.Jeu;
import org.model.Jeu.Difficulty;
import org.model.Jeu.GameMode;

import java.io.File;

/**
 * Test simple du PNG standard (sans metadonnees).
 */
public class PngChessTest {

    public static void main(String[] args) {
        System.out.println("Test du PNG standard...");

        Jeu jeu = new Jeu(GameMode.HUMAN_VS_HUMAN, Difficulty.EASY);

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        jeu.sauvegardePng();

        String pngPath = System.getProperty("user.dir") + File.separator + "partie_echecs.png";
        File pngFile = new File(pngPath);

        if (pngFile.exists() && pngFile.length() > 0) {
            System.out.println("PNG cree avec succes: " + pngPath);
        } else {
            System.out.println("Echec de creation du PNG: " + pngPath);
        }

        System.out.println("Ouverture de l'editeur PNG simplifie...");
        org.editor.PngChessEditor.main(new String[]{});
    }
}
