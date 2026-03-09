package org.model;

import java.util.ArrayList;

public abstract class Piece {

    protected Case position;
    protected boolean blanc; // color

    public Piece(boolean blanc) {
        this.blanc = blanc;
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

    // retourne les cases accessibles depuis la position actuelle
    public abstract ArrayList<Case> getCaseAccessible();
}
