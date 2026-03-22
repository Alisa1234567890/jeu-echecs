package org.model.plateau;

import org.model.piece.*;
import org.model.jeu.Joueur;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;

public class EchiquierModele extends Observable {

    private Piece[][] board;
    private final Plateau plateau;

    private final Color couleurClair = new Color(240, 217, 181);
    private final Color couleurFonce = new Color(181, 136, 99);
    private final Color couleurSurvol = Color.YELLOW;

    public EchiquierModele(Plateau plateau) {
        this.plateau = plateau;
        board = new Piece[8][8];
        initialiserPieces();
    }

    private void initialiserPieces() {
        // Pièces noires
        board[0][0] = new Tour("black", plateau);
        board[0][1] = new Cavalier("black", plateau);
        board[0][2] = new Fou("black", plateau);
        board[0][3] = new Dame("black", plateau);
        board[0][4] = new Roi("black", plateau);
        board[0][5] = new Fou("black", plateau);
        board[0][6] = new Cavalier("black", plateau);
        board[0][7] = new Tour("black", plateau);
        for (int col = 0; col < 8; col++) {
            board[1][col] = new Pion("black", plateau);
        }

        // Pièces blanches
        board[7][0] = new Tour("white", plateau);
        board[7][1] = new Cavalier("white", plateau);
        board[7][2] = new Fou("white", plateau);
        board[7][3] = new Dame("white", plateau);
        board[7][4] = new Roi("white", plateau);
        board[7][5] = new Fou("white", plateau);
        board[7][6] = new Cavalier("white", plateau);
        board[7][7] = new Tour("white", plateau);
        for (int col = 0; col < 8; col++) {
            board[6][col] = new Pion("white", plateau);
        }

        // Synchroniser board → plateau
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                plateau.getCase(r, c).setPiece(board[r][c]);
            }
        }

        setChanged();
        notifyObservers();
    }

    public Piece getPiece(int row, int col) {
        return board[row][col];
    }

    public void setPiece(int row, int col, Piece p) {
        board[row][col] = p;
        if (p != null) p.setCase(new Case(row, col));
        setChanged();
        notifyObservers();
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
        setChanged();
        notifyObservers();
    }

    public Piece getRoi(Joueur joueur) {
        for (int row = 0; row < board.length; row++) {
            for (int col = 0; col < board[row].length; col++) {
                Piece piece = board[row][col];
                if (piece instanceof Roi && piece.isBlanc() == joueur.isBlanc()) {
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
