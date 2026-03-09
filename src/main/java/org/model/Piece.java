package org.model;

import java.util.ArrayList;

public class Piece {

    protected Case position;
    protected boolean blanc;
    protected String color;

    public Piece(boolean blanc) {
        this.blanc = blanc;
        this.color = blanc ? "white" : "black";
    }

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

    public ArrayList<Case> getCaseAccessible() {
        return new ArrayList<>();
    }

    public boolean isValidMove(int startRow, int startCol,
                               int endRow, int endCol) {
        return false;
    }

    public String getImageName() {
        return "";
    }
}
