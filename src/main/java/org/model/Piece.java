package org.model;

import java.util.ArrayList;

public class Piece {

    protected Case position;
    protected boolean blanc; // color flag
    protected String color; // "white" or "black"

    public Piece(boolean blanc) {
        this.blanc = blanc;
        this.color = blanc ? "white" : "black";
    }

    // compatibility constructor used throughout the project
    public Piece(String color) {
        this.color = color;
        this.blanc = "white".equalsIgnoreCase(color);
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

    // retourne les cases accessibles depuis la position actuelle
    // default: empty list (subclasses that use board-rays should override)
    public ArrayList<Case> getCaseAccessible() {
        return new ArrayList<>();
    }

    // compatibility methods used by other Piece subclasses in the project
    public boolean isValidMove(int startRow, int startCol,
                               int endRow, int endCol) {
        return false;
    }

    public String getImageName() {
        return "";
    }
}
