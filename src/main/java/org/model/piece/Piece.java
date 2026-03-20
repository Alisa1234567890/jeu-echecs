package org.model.piece;

import org.model.plateau.Case;
import org.model.plateau.DecorateurCasesAccessibles;

import java.util.ArrayList;

public class Piece {

    protected Case position;
    protected boolean blanc;
    protected String color;
    protected DecorateurCasesAccessibles decor;

    public Piece(boolean blanc) {
        this(blanc, null);
    }

    public Piece(boolean blanc, DecorateurCasesAccessibles decor) {
        this.blanc = blanc;
        this.color = blanc ? "white" : "black";
        this.decor = decor;
    }

    public Piece(String color) {
        this(color, null);
    }

    public Piece(String color, DecorateurCasesAccessibles decor) {
        this.color = color;
        this.blanc = "white".equalsIgnoreCase(color);
        this.decor = decor;
    }

    public void setCase(Case c) {
        this.position = c;
    }

    public Case getCase() {
        return position;
    }

    public boolean isBlanc() {
        return blanc;
    }

    public String getColor() {
        return color;
    }

    public ArrayList<Case> getCasesAccessibles() {
        if (decor != null) {
            return decor.getCasesAccessibles(this);
        }
        return new ArrayList<>();
    }

    public ArrayList<Case> getCaseAccessible() {
        return getCasesAccessibles();
    }

    public boolean isValidMove(int startRow, int startCol,
                               int endRow, int endCol) {
        return false;
    }


    public String getImageName() {
        String cls = this.getClass().getSimpleName();
        String code = "?";
        switch (cls) {
            case "Pawn": code = "P"; break;
            case "Rook": code = "R"; break;
            case "Knight": code = "N"; break;
            case "Bishop": code = "B"; break;
            case "Queen": code = "Q"; break;
            case "King": code = "K"; break;
            default: code = "?"; break;
        }
        String prefix = isBlanc() ? "w" : "b";
        if ("?".equals(code)) return "";
        return prefix + code;
    }
}
