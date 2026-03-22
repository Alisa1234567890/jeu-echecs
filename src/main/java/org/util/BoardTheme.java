package org.util;

import java.awt.Color;

/**
 * Single source of truth for chessboard colors.
 */
public final class BoardTheme {

    private static final Color LIGHT_SQUARE = new Color(220, 234, 248);
    private static final Color DARK_SQUARE = new Color(88, 123, 168);
    private static final Color HOVER_SQUARE = new Color(133, 194, 255);
    private static final Color BOARD_BORDER = new Color(61, 98, 151);

    private BoardTheme() {
    }

    public static Color lightSquare() {
        return LIGHT_SQUARE;
    }

    public static Color darkSquare() {
        return DARK_SQUARE;
    }

    public static Color hoverSquare() {
        return HOVER_SQUARE;
    }

    public static Color boardBorder() {
        return BOARD_BORDER;
    }

    public static Color squareAt(int row, int col) {
        return ((row + col) % 2 == 0) ? LIGHT_SQUARE : DARK_SQUARE;
    }
}
