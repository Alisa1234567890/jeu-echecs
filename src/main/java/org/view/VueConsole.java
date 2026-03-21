package org.view;

import org.model.Coup;
import org.model.Jeu;
import org.model.Joueur;
import org.tools.PgnRecorder;
import org.model.piece.*;
import org.model.plateau.EchiquierModele;

import java.util.Observable;
import java.util.Observer;

/**
 * Vue console — observatrice de {@link Jeu}.
 *
 * Affiche dans la console :
 *   • Le plateau en ASCII/Unicode après chaque coup
 *   • Le coup joué en notation algébrique
 *   • L'état de la partie (échec, mat, pat, répétition)
 *   • Le PGN complet en fin de partie
 *
 * Cette classe n'est qu'une vue : elle ne modifie aucune donnée du modèle.
 */
public class VueConsole implements Observer {

    private static final String SEP  = "  ┼───┼───┼───┼───┼───┼───┼───┼───┼";
    private static final String TOP  = "  ┌───┬───┬───┬───┬───┬───┬───┬───┐";
    private static final String BOT  = "  └───┴───┴───┴───┴───┴───┴───┴───┘";
    private static final String COLS = "    a   b   c   d   e   f   g   h  ";

    private final Jeu jeu;
    /** Compteur de demi-coups (1 = blanc, 2 = noir, 3 = blanc, …) */
    private int halfMove = 0;

    public VueConsole(Jeu jeu) {
        this.jeu = jeu;
        // Affichage initial
        System.out.println();
        System.out.println("╔══════════════════════════════════════╗");
        System.out.println("║       VUE CONSOLE — JEU D'ÉCHECS     ║");
        System.out.println("╚══════════════════════════════════════╝");
        afficherPlateau();
        afficherTour();
    }

    // ── Observer ─────────────────────────────────────────────────────────────

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
            System.out.println("╔══════════════════════════════════════╗");
            System.out.printf( "║  %-38s║%n", "FIN : " + msg);
            System.out.println("╚══════════════════════════════════════╝");
            System.out.println();
            System.out.println("── Notation PGN ────────────────────────");
            System.out.println(jeu.getPgnRecorder().toPgn());
            System.out.println("────────────────────────────────────────");

        } else {
            // null → nouvelle partie ou redraw simple
            halfMove = 0;
            System.out.println();
            System.out.println("════════  NOUVELLE PARTIE  ════════");
            afficherPlateau();
            afficherTour();
        }
    }

    // ── Affichage du coup ────────────────────────────────────────────────────

    private void afficherCoup(Coup c, int half) {
        int num        = (half + 1) / 2;
        boolean blanc  = (half % 2 == 1);           // blanc joue les demi-coups impairs
        String couleur = blanc ? "BLANCS" : "NOIRS";

        System.out.println();
        System.out.printf("  Coup %d — %s : %s%n",
                num, couleur, describeCoup(c));
    }

    /**
     * Décrit un coup de façon lisible (notation algébrique longue + libellé).
     * Utilise les métadonnées stockées dans {@link Coup} lors de {@code appliquerCoup}.
     */
    private String describeCoup(Coup c) {
        String type = c.getType() == null ? "" : c.getType();

        // ── Roque ──────────────────────────────────────────────────────────
        if ("ROQUE".equals(type)) {
            boolean kingSide = c.arr.y > c.dep.y;
            return kingSide ? "O-O  (roque côté roi)" : "O-O-O  (roque côté dame)";
        }

        String from  = PgnRecorder.toCoord(c.dep);
        String to    = PgnRecorder.toCoord(c.arr);
        String piece = pieceSanLabel(c.getPieceName());
        String sep   = c.isCapture() ? "x" : "→";
        String move  = piece + from + sep + to;

        // ── Suffixes ────────────────────────────────────────────────────────
        switch (type) {
            case "PRISE EN PASSANT": return move + "  (prise en passant)";
            case "PROMOTION":        return move + "=" + c.getPromotionTo() + "  (promotion)";
            case "ECHEC":            return move + "+  (échec !)";
            default:                 return move;
        }
    }

    /** Lettre + espace pour les pièces (pion = vide). */
    private static String pieceSanLabel(String name) {
        switch (name == null ? "" : name) {
            case "Knight": return "N";
            case "Bishop": return "B";
            case "Rook":   return "R";
            case "Queen":  return "Q";
            case "King":   return "K";
            default:       return "";   // Pawn
        }
    }

    // ── Affichage du plateau ─────────────────────────────────────────────────

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

    /** Symbole Unicode de la pièce (· si case vide). */
    private static String symbol(Piece p) {
        if (p == null) return "·";
        boolean w = p.isBlanc();
        if (p instanceof King)   return w ? "♔" : "♚";
        if (p instanceof Queen)  return w ? "♕" : "♛";
        if (p instanceof Rook)   return w ? "♖" : "♜";
        if (p instanceof Bishop) return w ? "♗" : "♝";
        if (p instanceof Knight) return w ? "♘" : "♞";
        if (p instanceof Pawn)   return w ? "♙" : "♟";
        return "?";
    }

    // ── Tour courant ─────────────────────────────────────────────────────────

    private void afficherTour() {
        Joueur j = jeu.getJoueurCourant();
        String c = (j != null && j.isBlanc()) ? "BLANCS" : "NOIRS";
        System.out.println("  → Tour des " + c);
    }
}

