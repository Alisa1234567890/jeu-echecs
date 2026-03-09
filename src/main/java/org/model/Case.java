package org.model;

public class Case {

    private int x;
    private int y;
    private Piece piece;

    public Case(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public Piece getPiece() {
        return piece;
    }

    public void setPiece(Piece p) {
        this.piece = p;
        if (p != null) {
            p.setCase(this);
        }
    }

    public boolean isEmpty() {
        return piece == null;
    }

    @Override
    public String toString() {
        if (piece == null) return "[ ]";
        return "[" + piece.getClass().getSimpleName().charAt(0) + "]";
    }
}
