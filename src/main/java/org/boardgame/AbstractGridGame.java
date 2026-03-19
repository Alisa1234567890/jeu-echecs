package org.boardgame;

public abstract class AbstractGridGame implements GridGame {

    protected final int boardSize;
    protected String currentPlayer;
    protected String statusMessage;
    protected boolean finished;

    protected AbstractGridGame(int boardSize, String firstPlayer, String statusMessage) {
        this.boardSize = boardSize;
        this.currentPlayer = firstPlayer;
        this.statusMessage = statusMessage;
    }

    @Override
    public int getBoardSize() {
        return boardSize;
    }

    @Override
    public String getCurrentPlayer() {
        return currentPlayer;
    }

    @Override
    public String getStatusMessage() {
        return statusMessage;
    }

    @Override
    public boolean isFinished() {
        return finished;
    }

    protected void switchPlayer() {
        currentPlayer = "White".equals(currentPlayer) ? "Black" : "White";
    }
}
