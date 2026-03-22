package org.util;

import org.model.piece.Piece;
import org.model.plateau.Plateau;
import org.tools.SvgToPngConverter;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;

/**
 * Genere une image PNG standard de l'echiquier, sans metadonnees.
 */
public class ImageGenerator {

    private static final int CELL = 80;
    private static final int MARGIN = 30;
    private static final int SIZE = CELL * 8 + MARGIN * 2;

    private static final Color BEIGE = BoardTheme.lightSquare();
    private static final Color MARRON = BoardTheme.darkSquare();
    private static final Color BORDER = BoardTheme.boardBorder();

    public static Color getLightSquareColor() {
        return BEIGE;
    }

    public static Color getDarkSquareColor() {
        return MARRON;
    }

    public static Color getBoardBorderColor() {
        return BORDER;
    }

    public static BufferedImage renderBoard(Plateau plateau) {
        BufferedImage image = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        g.setColor(BORDER);
        g.fillRect(0, 0, SIZE, SIZE);

        Font coordFont = new Font("SansSerif", Font.BOLD, 14);
        g.setFont(coordFont);
        FontMetrics fm = g.getFontMetrics();
        g.setColor(Color.WHITE);
        for (int i = 0; i < 8; i++) {
            String col = String.valueOf((char) ('a' + i));
            int cx = MARGIN + i * CELL + (CELL - fm.stringWidth(col)) / 2;
            g.drawString(col, cx, MARGIN - 6);
            g.drawString(col, cx, SIZE - 6);

            String row = String.valueOf(8 - i);
            int cy = MARGIN + i * CELL + (CELL + fm.getAscent()) / 2 - 2;
            g.drawString(row, (MARGIN - fm.stringWidth(row)) / 2, cy);
            g.drawString(row, SIZE - (MARGIN + fm.stringWidth(row)) / 2 - 4, cy);
        }

        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                int x = MARGIN + col * CELL;
                int y = MARGIN + row * CELL;

                g.setColor((row + col) % 2 == 0 ? BEIGE : MARRON);
                g.fillRect(x, y, CELL, CELL);

                if (plateau == null) {
                    continue;
                }

                var square = plateau.getCase(row, col);
                if (square != null && !square.isEmpty()) {
                    Piece piece = square.getPiece();
                    String name = piece.getImageName();
                    boolean drawn = false;
                    if (name != null && !name.isEmpty()) {
                        String res = "/Pieces/" + name + ".svg";
                        try (InputStream is = ImageGenerator.class.getResourceAsStream(res)) {
                            if (is != null) {
                                BufferedImage pi = SvgToPngConverter.loadSvgAsImage(is, CELL, CELL);
                                if (pi != null) {
                                    g.drawImage(pi, x, y, CELL, CELL, null);
                                    drawn = true;
                                }
                            }
                        } catch (Exception ignored) {
                        }
                    }
                    if (!drawn) {
                        drawFallback(g, piece, x, y);
                    }
                }
            }
        }

        g.dispose();
        return image;
    }

    private static void drawFallback(Graphics2D g, Piece piece, int x, int y) {
        g.setColor(piece.isBlanc() ? new Color(255, 255, 255, 220) : new Color(50, 50, 50, 220));
        g.fillOval(x + 8, y + 8, CELL - 16, CELL - 16);
        g.setColor(piece.isBlanc() ? MARRON : BEIGE);
        String letter = piece.getClass().getSimpleName().substring(0, 1);
        Font f = new Font("SansSerif", Font.BOLD, CELL / 2);
        g.setFont(f);
        FontMetrics fm = g.getFontMetrics();
        g.drawString(letter, x + (CELL - fm.stringWidth(letter)) / 2,
                y + (CELL - fm.getHeight()) / 2 + fm.getAscent());
    }

    public static void saveAsPng(BufferedImage image, String path) {
        try {
            File file = new File(path);
            ImageIO.write(image, "png", file);
            System.out.println("Image sauvegardee: " + file.getAbsolutePath());
        } catch (Exception e) {
            System.err.println("Erreur lors de la sauvegarde de l'image: " + e.getMessage());
        }
    }
}
