package org.model.plateau;

public enum Direction {

    // Directions classiques (1 case)
    HAUT        (-1,  0),
    BAS         ( 1,  0),
    GAUCHE      ( 0, -1),
    DROITE      ( 0,  1),
    HAUT_DROITE (-1,  1),
    HAUT_GAUCHE (-1, -1),
    BAS_DROITE  ( 1,  1),
    BAS_GAUCHE  ( 1, -1),

    // Sauts du Cavalier (forme en L)
    SAUT_2H_1D  (-2,  1),   // 2 haut, 1 droite
    SAUT_2H_1G  (-2, -1),   // 2 haut, 1 gauche
    SAUT_2B_1D  ( 2,  1),   // 2 bas,  1 droite
    SAUT_2B_1G  ( 2, -1),   // 2 bas,  1 gauche
    SAUT_1H_2D  (-1,  2),   // 1 haut, 2 droite
    SAUT_1H_2G  (-1, -2),   // 1 haut, 2 gauche
    SAUT_1B_2D  ( 1,  2),   // 1 bas,  2 droite
    SAUT_1B_2G  ( 1, -2);   // 1 bas,  2 gauche

    public final int dx;
    public final int dy;

    Direction(int dx, int dy) {
        this.dx = dx;
        this.dy = dy;
    }

    public static Direction[] glissement() {
        return new Direction[]{
            HAUT, BAS, GAUCHE, DROITE,
            HAUT_DROITE, HAUT_GAUCHE, BAS_DROITE, BAS_GAUCHE
        };
    }

    public static Direction[] sauts() {
        return new Direction[]{
            SAUT_2H_1D, SAUT_2H_1G, SAUT_2B_1D, SAUT_2B_1G,
            SAUT_1H_2D, SAUT_1H_2G, SAUT_1B_2D, SAUT_1B_2G
        };
    }
}


