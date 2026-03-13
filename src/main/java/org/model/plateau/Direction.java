package org.model.plateau;

public enum Direction {
    HAUT( -1, 0),
    BAS( 1, 0),
    GAUCHE(0, -1),
    DROITE(0, 1),
    HAUT_DROITE(-1, 1),
    HAUT_GAUCHE(-1, -1),
    BAS_DROITE(1, 1),
    BAS_GAUCHE(1, -1);

    public final int dx;
    public final int dy;

    Direction(int _dx, int _dy) {
        this.dx = -_dx;
        this.dy = -_dy;
    }
}
