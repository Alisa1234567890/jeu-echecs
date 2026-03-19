package org.boardgame.checkers;

import org.boardgame.GridPiece;

public class CheckersPiece implements GridPiece {

    private final String owner;
    private boolean king;

    public CheckersPiece(String owner) {
        this.owner = owner;
    }

    @Override
    public String owner() {
        return owner;
    }

    @Override
    public String symbol() {
        if ("White".equals(owner)) {
            return king ? "WK" : "WM";
        }
        return king ? "BK" : "BM";
    }

    public boolean isKing() {
        return king;
    }

    public void crown() {
        king = true;
    }
}
