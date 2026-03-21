package org.model;

import java.awt.Point;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Enregistre une partie d'échecs au format PGN (Portable Game Notation).
 *
 * Structure d'un fichier PGN :
 *   - En-tête de 7 balises obligatoires (Seven Tag Roster)
 *   - Corps : numérotation + coups en SAN (Standard Algebraic Notation)
 *
 * Exemple :
 *   [Event "Partie d'Échecs"]
 *   [White "Blanc"]
 *   ...
 *
 *   1. e4 e5 2. Nf3 Nc6 3. Bb5 a6 *
 */
public class PgnRecorder {

    private final List<String> sanBlancs = new ArrayList<>();
    private final List<String> sanNoirs  = new ArrayList<>();

    private String result       = "*";   // en cours
    private final String date;
    private final String playerWhite;
    private final String playerBlack;

    public PgnRecorder(String playerWhite, String playerBlack) {
        this.playerWhite = playerWhite;
        this.playerBlack = playerBlack;
        LocalDate today = LocalDate.now();
        this.date = String.format("%04d.%02d.%02d",
                today.getYear(), today.getMonthValue(), today.getDayOfMonth());
    }

    // -----------------------------------------------------------------------
    // API publique
    // -----------------------------------------------------------------------

    /**
     * Enregistre un coup en SAN.
     *
     * @param coup        le coup appliqué (avec pieceName, capture, disambiguation, etc.)
     * @param estBlanc    true si c'est le camp blanc qui vient de jouer
     * @param estEchec    l'adversaire est en échec (mais pas mat)
     * @param estEchecEtMat l'adversaire est en échec et mat
     */
    public void enregistrerCoup(Coup coup, boolean estBlanc,
                                boolean estEchec, boolean estEchecEtMat) {
        String san = buildSan(coup, estEchec, estEchecEtMat);
        if (estBlanc) {
            sanBlancs.add(san);
        } else {
            sanNoirs.add(san);
        }
        System.out.println("PGN coup: " + san);
    }

    public void setResult(String result) {
        this.result = result;
    }

    /** Retourne le contenu PGN complet sous forme de String. */
    public String toPgn() {
        StringBuilder sb = new StringBuilder();

        // --- Seven Tag Roster ---
        sb.append("[Event \"Partie d'Échecs\"]\n");
        sb.append("[Site \"?\"]\n");
        sb.append("[Date \"").append(date).append("\"]\n");
        sb.append("[Round \"?\"]\n");
        sb.append("[White \"").append(playerWhite).append("\"]\n");
        sb.append("[Black \"").append(playerBlack).append("\"]\n");
        sb.append("[Result \"").append(result).append("\"]\n");
        sb.append("\n");

        // --- Corps (coups numérotés) ---
        int total = Math.max(sanBlancs.size(), sanNoirs.size());
        for (int i = 0; i < total; i++) {
            // Numéro de coup + point
            sb.append(i + 1).append(". ");
            if (i < sanBlancs.size()) sb.append(sanBlancs.get(i));
            sb.append(' ');
            if (i < sanNoirs.size())  sb.append(sanNoirs.get(i));
            sb.append(' ');
        }
        sb.append(result).append("\n");

        return sb.toString();
    }

    /**
     * Sauvegarde le fichier PGN à l'emplacement indiqué.
     *
     * @param filePath chemin complet du fichier (ex. "/home/user/partie.pgn")
     */
    public void sauvegarder(String filePath) {
        try (OutputStreamWriter fw = new OutputStreamWriter(
                new FileOutputStream(filePath), StandardCharsets.UTF_8)) {
            fw.write(toPgn());
            System.out.println("PGN sauvegardé : " + filePath);
        } catch (IOException e) {
            System.err.println("PGN — erreur d'écriture : " + e.getMessage());
        }
    }

    // -----------------------------------------------------------------------
    // Construction SAN
    // -----------------------------------------------------------------------

    private String buildSan(Coup coup, boolean check, boolean checkmate) {
        String type = coup.getType();

        // --- Roque ---
        if ("ROQUE".equals(type)) {
            String san = (coup.arr.y > coup.dep.y) ? "O-O" : "O-O-O";
            return appendSuffix(san, check, checkmate);
        }

        StringBuilder sb = new StringBuilder();
        String pieceName = coup.getPieceName();   // "Pawn", "Knight", …
        boolean isPromotion = "PROMOTION".equals(type);
        boolean isPawn  = "Pawn".equals(pieceName) || isPromotion;
        boolean capture = coup.isCapture();

        if (isPawn) {
            // Prise (y compris prise en passant) → "exd5"
            if (capture) {
                sb.append((char) ('a' + coup.dep.y));
                sb.append('x');
            }
            sb.append(toCoord(coup.arr));
            if (isPromotion) {
                sb.append('=').append(coup.getPromotionTo()); // "=Q"
            }
        } else {
            // Pièce normale
            sb.append(pieceSanLetter(pieceName));
            sb.append(coup.getDisambiguation());  // "" / "a" / "3" / "a3"
            if (capture) sb.append('x');
            sb.append(toCoord(coup.arr));
        }

        return appendSuffix(sb.toString(), check, checkmate);
    }

    private static String appendSuffix(String san, boolean check, boolean checkmate) {
        if (checkmate) return san + "#";
        if (check)     return san + "+";
        return san;
    }

    /** Lettre SAN standard pour chaque pièce (pion = chaîne vide). */
    private static String pieceSanLetter(String className) {
        switch (className) {
            case "Knight": return "N";
            case "Bishop": return "B";
            case "Rook":   return "R";
            case "Queen":  return "Q";
            case "King":   return "K";
            default:       return "";  // Pawn
        }
    }

    /**
     * Convertit (row, col) interne → coordonnées algébriques PGN.
     * row 0 = rang 8, col 0 = colonne a.
     */
    public static String toCoord(Point p) {
        return String.valueOf((char) ('a' + p.y)) + (8 - p.x);
    }
}



