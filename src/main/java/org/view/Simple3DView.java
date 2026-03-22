package org.view;

import org.model.Coup;
import org.model.Jeu;
import org.model.JeuObserver;
import org.model.piece.Bishop;
import org.model.piece.King;
import org.model.piece.Knight;
import org.model.piece.Pawn;
import org.model.piece.Piece;
import org.model.piece.Queen;
import org.model.piece.Rook;
import org.model.plateau.Case;
import org.model.plateau.PlateauSingleton;
import org.util.ImageGenerator;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Polygon;
import java.awt.Point;
import java.awt.RenderingHints;

/**
 * Vue pseudo-3D branchée sur JeuObserver.
 * Les curseurs X/Y/Zoom modifient maintenant l'affichage en temps reel.
 */
public class Simple3DView extends JFrame implements JeuObserver {

    private final Jeu jeu;
    private final JLabel statusLabel;
    private final JLabel turnLabel;
    private final JLabel lastMoveLabel;
    private final PlaceholderPanel boardPanel;
    private final JSlider angleXSlider;
    private final JSlider angleYSlider;
    private final JSlider zoomSlider;

    private Coup lastMove;

    public Simple3DView(Jeu jeu) {
        super("Vue 3D (stub)");
        this.jeu = jeu;

        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(900, 650);
        setLayout(new BorderLayout(8, 8));

        JPanel left = new JPanel(new GridLayout(0, 1, 4, 4));
        left.setPreferredSize(new Dimension(220, 600));
        left.setBorder(BorderFactory.createTitledBorder("Options 3D"));

        left.add(new JLabel("Angle X :"));
        angleXSlider = new JSlider(JSlider.HORIZONTAL, -90, 90, 30);
        angleXSlider.setMajorTickSpacing(30);
        angleXSlider.setPaintTicks(true);
        left.add(angleXSlider);

        left.add(new JLabel("Angle Y :"));
        angleYSlider = new JSlider(JSlider.HORIZONTAL, -180, 180, 0);
        angleYSlider.setMajorTickSpacing(60);
        angleYSlider.setPaintTicks(true);
        left.add(angleYSlider);

        left.add(new JLabel("Zoom :"));
        zoomSlider = new JSlider(JSlider.HORIZONTAL, 50, 200, 100);
        zoomSlider.setMajorTickSpacing(25);
        zoomSlider.setPaintTicks(true);
        left.add(zoomSlider);

        statusLabel = new JLabel("Statut: ...");
        turnLabel = new JLabel("Tour: ...");
        lastMoveLabel = new JLabel("Dernier coup: -");

        statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        turnLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        lastMoveLabel.setFont(new Font("Monospaced", Font.PLAIN, 12));

        JPanel info = new JPanel();
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.setBorder(BorderFactory.createTitledBorder("Etat de la partie"));
        info.add(statusLabel);
        info.add(Box.createVerticalStrut(6));
        info.add(turnLabel);
        info.add(Box.createVerticalStrut(6));
        info.add(lastMoveLabel);
        left.add(info);

        boardPanel = new PlaceholderPanel();
        wireControls();

        add(left, BorderLayout.WEST);
        add(boardPanel, BorderLayout.CENTER);
        setLocationRelativeTo(null);

        refreshFromGame();
    }

    private void wireControls() {
        angleXSlider.addChangeListener(_ -> boardPanel.repaint());
        angleYSlider.addChangeListener(_ -> boardPanel.repaint());
        zoomSlider.addChangeListener(_ -> boardPanel.repaint());
    }

    @Override
    public void update(Object arg) {
        if (arg instanceof Coup coup) {
            lastMove = coup;
        }
        SwingUtilities.invokeLater(this::refreshFromGame);
    }

    private void refreshFromGame() {
        statusLabel.setText("Statut: " + jeu.getStatusMessage());
        if (jeu.partieTerminee()) {
            String end = jeu.isDraw() ? "Nulle" : ("Victoire " + jeu.getWinnerLabel());
            turnLabel.setText("Tour: Partie terminee (" + end + ")");
        } else {
            turnLabel.setText("Tour: " + (jeu.isWhiteToMove() ? "Blanc" : "Noir"));
        }
        lastMoveLabel.setText("Dernier coup: " + formatMove(lastMove));
        boardPanel.repaint();
    }

    private String formatMove(Coup coup) {
        if (coup == null || coup.dep == null || coup.arr == null) {
            return "-";
        }
        return toSquare(coup.dep) + " -> " + toSquare(coup.arr) + " (" + coup.getType() + ")";
    }

    private String toSquare(Point p) {
        char file = (char) ('a' + p.y);
        int rank = 8 - p.x;
        return "" + file + rank;
    }

    private double getPitchRadians() {
        double pitchDegrees = 35.0 + angleXSlider.getValue() * 0.30;
        pitchDegrees = Math.max(8.0, Math.min(78.0, pitchDegrees));
        return Math.toRadians(pitchDegrees);
    }

    private double getYawRadians() {
        return Math.toRadians(angleYSlider.getValue());
    }

    private double getZoomFactor() {
        return zoomSlider.getValue() / 100.0;
    }

    private Point project(double x, double y, double z, int centerX, int centerY, double scale) {
        double yaw = getYawRadians();
        double pitch = getPitchRadians();

        double x1 = x * Math.cos(yaw) + z * Math.sin(yaw);
        double z1 = -x * Math.sin(yaw) + z * Math.cos(yaw);
        double y2 = y * Math.cos(pitch) - z1 * Math.sin(pitch);
        double z2 = y * Math.sin(pitch) + z1 * Math.cos(pitch);

        double cameraDistance = 14.0;
        double perspective = cameraDistance / (cameraDistance - z2);
        int sx = centerX + (int) Math.round(x1 * scale * perspective);
        int sy = centerY - (int) Math.round(y2 * scale * perspective);
        return new Point(sx, sy);
    }

    private Color darken(Color base, double factor) {
        factor = Math.max(0.0, Math.min(1.0, factor));
        return new Color(
                (int) Math.round(base.getRed() * factor),
                (int) Math.round(base.getGreen() * factor),
                (int) Math.round(base.getBlue() * factor)
        );
    }

    private class PlaceholderPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

                g2.setColor(Color.WHITE);
                g2.fillRect(0, 0, getWidth(), getHeight());

                int centerX = getWidth() / 2;
                int centerY = getHeight() / 2 + 45;
                double scale = 42.0 * getZoomFactor();
                double thickness = 0.55;

                Color light = ImageGenerator.getLightSquareColor();
                Color dark = ImageGenerator.getDarkSquareColor();
                Color border = ImageGenerator.getBoardBorderColor();

                Point tl = project(-4, 0, -4, centerX, centerY, scale);
                Point tr = project(4, 0, -4, centerX, centerY, scale);
                Point br = project(4, 0, 4, centerX, centerY, scale);
                Point bl = project(-4, 0, 4, centerX, centerY, scale);
                Point blDown = project(-4, -thickness, 4, centerX, centerY, scale);
                Point brDown = project(4, -thickness, 4, centerX, centerY, scale);
                Point trDown = project(4, -thickness, -4, centerX, centerY, scale);
                Point tlDown = project(-4, -thickness, -4, centerX, centerY, scale);

                g2.setColor(darken(border, 0.72));
                g2.fillPolygon(polygonOf(bl, br, brDown, blDown));
                g2.setColor(darken(border, 0.62));
                g2.fillPolygon(polygonOf(tr, br, brDown, trDown));
                g2.setColor(darken(border, 0.56));
                g2.fillPolygon(polygonOf(tl, bl, blDown, tlDown));

                for (int r = 0; r < 8; r++) {
                    for (int c = 0; c < 8; c++) {
                        Point p00 = project(c - 4.0, 0, r - 4.0, centerX, centerY, scale);
                        Point p10 = project(c - 3.0, 0, r - 4.0, centerX, centerY, scale);
                        Point p11 = project(c - 3.0, 0, r - 3.0, centerX, centerY, scale);
                        Point p01 = project(c - 4.0, 0, r - 3.0, centerX, centerY, scale);
                        Polygon squarePoly = polygonOf(p00, p10, p11, p01);

                        g2.setColor((r + c) % 2 == 0 ? light : dark);
                        g2.fillPolygon(squarePoly);
                        g2.setColor(new Color(0, 0, 0, 55));
                        g2.drawPolygon(squarePoly);

                        Case square = PlateauSingleton.INSTANCE.getCase(r, c);
                        if (square != null && square.getPiece() != null) {
                            Point pc = project(c - 3.5, 0.28, r - 3.5, centerX, centerY, scale);
                            Point px = project(c - 3.0, 0.28, r - 3.5, centerX, centerY, scale);
                            Point pz = project(c - 3.5, 0.28, r - 3.0, centerX, centerY, scale);
                            int pieceSize = Math.max(12, Math.min(distance(pc, px), distance(pc, pz)));
                            drawPieceToken(g2, square.getPiece(), pc.x, pc.y, pieceSize, dark, light);
                        }
                    }
                }

                g2.setColor(new Color(32, 48, 74));
                g2.setFont(new Font("SansSerif", Font.BOLD, 14));
                String msg = String.format("Vue pseudo-3D - X:%d  Y:%d  Zoom:%d%%",
                        angleXSlider.getValue(), angleYSlider.getValue(), zoomSlider.getValue());
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(msg, (getWidth() - fm.stringWidth(msg)) / 2, getHeight() - 18);
            } finally {
                g2.dispose();
            }
        }

        private Polygon polygonOf(Point... points) {
            Polygon polygon = new Polygon();
            for (Point point : points) {
                polygon.addPoint(point.x, point.y);
            }
            return polygon;
        }

        private int distance(Point a, Point b) {
            return (int) Math.round(Math.hypot(a.x - b.x, a.y - b.y));
        }

        private void drawPieceToken(Graphics2D g2, Piece piece, int cx, int cy, int size, Color darkSquare, Color lightSquare) {
            int shadowW = size + 10;
            int shadowH = Math.max(6, size / 2);
            g2.setColor(new Color(0, 0, 0, 55));
            g2.fillOval(cx - shadowW / 2, cy + size / 2, shadowW, shadowH);

            int bodyW = size;
            int bodyH = size + 10;
            g2.setColor(piece.isBlanc() ? new Color(242, 242, 242) : new Color(45, 45, 45));
            g2.fillOval(cx - bodyW / 2, cy - bodyH / 2, bodyW, bodyW);
            g2.fillRoundRect(cx - bodyW / 3, cy - bodyH / 6, bodyW * 2 / 3, bodyH * 2 / 3, 8, 8);
            g2.fillOval(cx - bodyW / 4, cy - bodyH / 2 - bodyW / 6, bodyW / 2, bodyW / 2);

            g2.setColor(piece.isBlanc() ? darkSquare : lightSquare);
            g2.setFont(new Font("SansSerif", Font.BOLD, Math.max(11, size - 2)));
            String s = pieceLetter(piece);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(s, cx - fm.stringWidth(s) / 2, cy + fm.getAscent() / 3 + 1);
        }

        private String pieceLetter(Piece piece) {
            if (piece instanceof Pawn) return "P";
            if (piece instanceof Knight) return "N";
            if (piece instanceof Bishop) return "B";
            if (piece instanceof Rook) return "R";
            if (piece instanceof Queen) return "Q";
            if (piece instanceof King) return "K";
            return "?";
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("Vue 3D (demo)");
            f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            f.setSize(500, 300);
            f.add(new JLabel("Lancer via org.Main pour la vue connectee au jeu.", JLabel.CENTER));
            f.setLocationRelativeTo(null);
            f.setVisible(true);
        });
    }
}
