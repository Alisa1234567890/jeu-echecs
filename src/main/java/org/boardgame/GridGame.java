package org.boardgame;

import java.util.List;

public interface GridGame {
    String getTitle();
    int getBoardSize();
    GridPiece getPieceAt(int row, int col);
    String getCurrentPlayer();
    String getStatusMessage();
    boolean isFinished();
    List<GridMove> getLegalMovesFrom(GridCoordinate from);
    boolean playMove(GridMove move);
    void reset();
}
