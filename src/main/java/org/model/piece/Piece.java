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

    public ArrayList<Case> getCaseAccessible() {
        if (decor != null) {
            return decor.getAccessibleCases(this);
        }
        return new ArrayList<>();
    }

    public String getImageName() {
        String cls = this.getClass().getSimpleName();
        String code = "?";
        switch (cls) {
            case "Pion":     code = "P"; break;
            case "Tour":     code = "R"; break;
            case "Cavalier": code = "N"; break;
            case "Fou":      code = "B"; break;
            case "Dame":     code = "Q"; break;
            case "Roi":      code = "K"; break;
            default: code = "?"; break;
        }
        String prefix = isBlanc() ? "w" : "b";
        if ("?".equals(code)) return "";
        return prefix + code;
    }
}
