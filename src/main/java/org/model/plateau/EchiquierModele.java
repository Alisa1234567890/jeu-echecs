package org.model.plateau;

import org.model.piece.*;
import org.model.Joueur;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class EchiquierModele {

    private Piece[][] board;

    private final Color couleurClair = new Color(240, 217, 181);
    private final Color couleurFonce = new Color(181, 136, 99);
    private final Color couleurSurvol = Color.YELLOW;

    public EchiquierModele() {
        board = new Piece[8][8];
        initialiserPieces();
    }

    private void initialiserPieces() {

        board[0][0] = new Rook("black");
        board[0][1] = new Knight("black");
        board[0][2] = new Bishop("black");
        board[0][3] = new Queen("black");
        board[0][4] = new King("black");
        board[0][5] = new Bishop("black");
        board[0][6] = new Knight("black");
        board[0][7] = new Rook("black");

        for (int col = 0; col < 8; col++) {
            board[1][col] = new Pawn("black");
        }

        board[7][0] = new Rook("white");
        board[7][1] = new Knight("white");
        board[7][2] = new Bishop("white");
        board[7][3] = new Queen("white");
        board[7][4] = new King("white");
        board[7][5] = new Bishop("white");
        board[7][6] = new Knight("white");
        board[7][7] = new Rook("white");

        for (int col = 0; col < 8; col++) {
            board[6][col] = new Pawn("white");
        }

    }

    public Piece getPiece(int row, int col) {
        return board[row][col];
    }

    public void setPiece(int row, int col, Piece p) {
        board[row][col] = p;
        if (p != null) p.setCase(new Case(row, col));
    }

    public void syncFromPlateau(Plateau plateau) {
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Case plateauCase = plateau.getCase(r, c);
                Piece p = (plateauCase == null) ? null : plateauCase.getPiece();
                board[r][c] = p;
                if (p != null && plateauCase != null) {
                    p.setCase(plateauCase);
                }
            }
        }
    }

    public Color getCouleurCase(int ligne, int colonne) {
        return (ligne + colonne) % 2 == 0 ? couleurClair : couleurFonce;
    }

    public Color getCouleurSurvol() {
        return couleurSurvol;
    }


    public Piece getRoi(Joueur joueur) {
        for (int row = 0; row < board.length; row++) {
            for (int col = 0; col < board[row].length; col++) {
                Piece piece = board[row][col];
                if (piece instanceof King && piece.isBlanc() == joueur.isBlanc()) {
                    return piece;
                }
            }
        }
        return null;
    }


    public List<Piece> getPiecesAdverses(Joueur joueur) {
        List<Piece> piecesAdverses = new ArrayList<>();
        for (int row = 0; row < board.length; row++) {
            for (int col = 0; col < board[row].length; col++) {
                Piece piece = board[row][col];
                if (piece != null && piece.isBlanc() != joueur.isBlanc()) {
                    piecesAdverses.add(piece);
                }
            }
        }
        return piecesAdverses;
    }


    public List<Piece> getPieces(Joueur joueur) {
        List<Piece> pieces = new ArrayList<>();
        for (int row = 0; row < board.length; row++) {
            for (int col = 0; col < board[row].length; col++) {
                Piece piece = board[row][col];
                if (piece != null && piece.isBlanc() == joueur.isBlanc()) {
                    pieces.add(piece);
                }
            }
        }
        return pieces;
    }
}
