package org.model;


public enum Direction {
    NORTH(-1, 0), SOUTH(1, 0), WEST(0, -1), EAST(0, 1),
    NORTH_EAST(-1, 1), NORTH_WEST(-1, -1), SOUTH_EAST(1, 1), SOUTH_WEST(1, -1);

    public final int dx;
    public final int dy;

    Direction(int dx, int dy) {
        this.dx = dx;
        this.dy = dy;
    }
}
