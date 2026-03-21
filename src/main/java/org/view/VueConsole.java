package org.view;

import org.model.Coup;
import org.model.Jeu;
import org.model.Joueur;
import org.model.piece.*;
import org.model.plateau.EchiquierModele;

import java.util.Observable;
import java.util.Observer;


public class VueConsole implements Observer {

    private static final String SEP  = "  ┼───┼───┼───┼───┼───┼───┼───┼───┼";
    private static final String TOP  = "  ┌───┬───┬───┬───┬───┬───┬───┬───┐";
    private static final String BOT  = "  └───┴───┴───┴───┴───┴───┴───┴───┘";
    private static final String COLS = "    a   b   c   d   e   f   g   h  ";

    private final Jeu jeu;
    private int halfMove = 0;

    public VueConsole(Jeu jeu) {
        this.jeu = jeu;
        // Affichage initial
        System.out.println();
        System.out.println("VUE CONSOLE — JEU D'ÉCHECS");
        afficherPlateau();
        afficherTour();
    }

    @Override
    public void update(Observable o, Object arg) {
        if (arg instanceof Coup) {
            Coup c = (Coup) arg;
            halfMove++;
            afficherCoup(c, halfMove);
            afficherPlateau();
            afficherTour();

        } else if (arg instanceof String) {
            // Fin de partie
            String msg = (String) arg;
            System.out.println();
            System.out.printf(" %-38s%n", "FIN : " + msg);
            System.out.println();
            System.out.println("=== Notation PGN ===");
            System.out.println("Fichier PNG : " + System.getProperty("user.dir") + "/partie_echecs.png");

        } else {
            halfMove = 0;
            System.out.println();
            System.out.println("NOUVELLE PARTIE");
            afficherPlateau();
            afficherTour();
        }
    }

    private void afficherCoup(Coup c, int half) {
        int num        = (half + 1) / 2;
        boolean blanc  = (half % 2 == 1);           // blanc joue les demi-coups impairs
        String couleur = blanc ? "BLANCS" : "NOIRS";

        System.out.println();
        System.out.printf("  Coup %d — %s : %s%n",
                num, couleur, describeCoup(c));
    }


    private String describeCoup(Coup c) {
        String type = c.getType() == null ? "" : c.getType();
        if ("ROQUE".equals(type)) {
            boolean kingSide = c.arr.y > c.dep.y;
            return kingSide ? "O-O  (roque côté roi)" : "O-O-O  (roque côté dame)";
        }

        String from  = toCoord(c.dep);
        String to    = toCoord(c.arr);
        String piece = pieceSanLabel(c.getPieceName());
        String sep   = c.isCapture() ? "x" : "→";
        String move  = piece + from + sep + to;

        switch (type) {
            case "PRISE EN PASSANT": return move + "  (prise en passant)";
            case "PROMOTION":        return move + "=" + c.getPromotionTo() + "  (promotion)";
            case "ECHEC":            return move + "+  (échec !)";
            default:                 return move;
        }
    }

    private static String pieceSanLabel(String name) {
        switch (name == null ? "" : name) {
            case "Cavalier": return "N";
            case "Fou":      return "B";
            case "Tour":     return "R";
            case "Dame":     return "Q";
            case "Roi":      return "K";
            default:         return "";   // Pion
        }
    }

    private void afficherPlateau() {
        EchiquierModele e = jeu.getEchiquier();
        System.out.println();
        System.out.println(TOP);
        for (int row = 0; row < 8; row++) {
            System.out.print((8 - row) + " │");
            for (int col = 0; col < 8; col++) {
                Piece p = e.getPiece(row, col);
                System.out.print(" " + symbol(p) + " │");
            }
            System.out.println(" " + (8 - row));
            if (row < 7) System.out.println(SEP);
        }
        System.out.println(BOT);
        System.out.println(COLS);
    }

    private static String symbol(Piece p) {
        if (p == null) return "·";
        boolean w = p.isBlanc();
        if (p instanceof Roi)   return w ? "♔" : "♚";
        if (p instanceof Dame)  return w ? "♕" : "♛";
        if (p instanceof Tour)   return w ? "♖" : "♜";
        if (p instanceof Fou) return w ? "♗" : "♝";
        if (p instanceof Cavalier) return w ? "♘" : "♞";
        if (p instanceof Pion)   return w ? "♙" : "♟";
        return "?";
    }

    private static String toCoord(java.awt.Point p) {
        return String.valueOf((char) ('a' + p.y)) + (8 - p.x);
    }

    private void afficherTour() {
        Joueur j = jeu.getJoueurCourant();
        String c = (j != null && j.isBlanc()) ? "BLANCS" : "NOIRS";
        System.out.println("  → Tour des " + c);
    }
}

