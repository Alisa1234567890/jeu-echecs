package org.model;

public abstract class Piece {
    protected String color; // "white" or "black"

    public Piece(String color) {
        this.color = color;
    }

    public String getColor() {
        return color;
    }

    // Each piece defines its own movement
    public abstract boolean isValidMove(int startRow, int startCol,
                                        int endRow, int endCol);

    // Each piece provides its image filename
    public abstract String getImageName();
}
