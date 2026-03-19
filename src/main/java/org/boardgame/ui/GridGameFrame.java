package org.boardgame.ui;

import org.boardgame.GridCoordinate;
import org.boardgame.GridGame;
import org.boardgame.GridMove;
import org.boardgame.GridPiece;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;

public class GridGameFrame extends JFrame {

    private final GridGame game;
    private final JButton[][] cells;
    private final JLabel statusLabel = new JLabel("", SwingConstants.CENTER);
    private final List<GridCoordinate> highlights = new ArrayList<>();
    private GridCoordinate selected;

    public GridGameFrame(GridGame game) {
        super(game.getTitle());
        this.game = game;
        this.cells = new JButton[game.getBoardSize()][game.getBoardSize()];

        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(720, 760);
        setLayout(new BorderLayout(8, 8));

        statusLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        add(statusLabel, BorderLayout.NORTH);

        JPanel board = new JPanel(new GridLayout(game.getBoardSize(), game.getBoardSize()));
        for (int row = 0; row < game.getBoardSize(); row++) {
            for (int col = 0; col < game.getBoardSize(); col++) {
                JButton button = new JButton();
                button.setFocusPainted(false);
                button.setFont(new Font("Monospaced", Font.BOLD, 18));
                final int r = row;
                final int c = col;
                button.addActionListener(e -> onCellClicked(r, c));
                cells[row][col] = button;
                board.add(button);
            }
        }

        JButton resetButton = new JButton("Reset");
        resetButton.addActionListener(e -> {
            game.reset();
            selected = null;
            highlights.clear();
            refresh();
        });

        add(board, BorderLayout.CENTER);
        add(resetButton, BorderLayout.SOUTH);
        refresh();
    }

    private void onCellClicked(int row, int col) {
        GridCoordinate coordinate = new GridCoordinate(row, col);
        if (selected != null) {
            GridMove move = new GridMove(selected, coordinate);
            if (game.playMove(move)) {
                selected = null;
                highlights.clear();
                refresh();
                return;
            }
        }

        selected = coordinate;
        highlights.clear();
        highlights.addAll(game.getLegalMovesFrom(coordinate).stream().map(GridMove::to).toList());
        refresh();
    }

    private void refresh() {
        statusLabel.setText(game.getStatusMessage());
        for (int row = 0; row < game.getBoardSize(); row++) {
            for (int col = 0; col < game.getBoardSize(); col++) {
                JButton button = cells[row][col];
                GridPiece piece = game.getPieceAt(row, col);
                button.setText(piece == null ? "" : piece.symbol());
                button.setBackground(isHighlighted(row, col)
                        ? new Color(123, 200, 132)
                        : ((row + col) % 2 == 0 ? new Color(240, 217, 181) : new Color(181, 136, 99)));
                button.setBorder(BorderFactory.createLineBorder(
                        selected != null && selected.row() == row && selected.col() == col ? Color.YELLOW : Color.DARK_GRAY,
                        selected != null && selected.row() == row && selected.col() == col ? 3 : 1
                ));
            }
        }
        repaint();
    }

    private boolean isHighlighted(int row, int col) {
        return highlights.stream().anyMatch(c -> c.row() == row && c.col() == col);
    }
}
