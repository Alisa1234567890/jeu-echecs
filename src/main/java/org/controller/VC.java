package org.controller;

import org.model.Coup;
import org.model.Jeu;
import org.model.Joueur;
import org.model.piece.Piece;
import org.tools.SvgToPngConverter;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JWindow;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.IllegalComponentStateException;
import java.awt.Image;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

public class VC extends JFrame implements Observer {

    private final Jeu jeu;
    private final ChessController controller;
    private final JPanel[][] casePanels = new JPanel[8][8];
    private final JLabel[][] caseLabels = new JLabel[8][8];
    private final List<Point> highlightedMoves = new ArrayList<>();
    private final Color beige = new Color(240, 217, 181);
    private final Color marron = new Color(181, 136, 99);
    private final Color highlight = new Color(100, 200, 100);

    private Point depart;
    private JPanel panel;
    private JPanel chessBoard;
    private JPanel draggingPanel;
    private JWindow dragWindow;
    private Point dragOffset;
    private AWTEventListener globalMouseListener;
    private JLabel statusLabel;

    public VC(Jeu jeu) {
        this(jeu, null);
    }

    public VC(Jeu jeu, ChessController controller) {
        this.jeu = jeu;
        this.controller = controller;
        jeu.addObserver(this);

        setTitle("Jeu d'echecs");
        setSize(760, 820);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        chessBoard = new JPanel(new GridLayout(8, 8));
        chessBoard.setPreferredSize(new Dimension(640, 640));

        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                JPanel casePanel = new JPanel(new BorderLayout());
                casePanel.setOpaque(true);
                casePanels[row][col] = casePanel;
                JLabel label = new JLabel("", SwingConstants.CENTER);
                caseLabels[row][col] = label;
                casePanel.add(label, BorderLayout.CENTER);

                final int ligne = row;
                final int colonne = col;

                casePanel.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mousePressed(MouseEvent e) {
                        synchronized (jeu) {
                            if (controller != null && !controller.canSelectPiece(ligne, colonne)) {
                                clearSelection();
                                redraw();
                                return;
                            }

                            Component comp = casePanel.getComponentCount() > 0 ? casePanel.getComponent(0) : null;
                            JLabel source = (comp instanceof JLabel) ? (JLabel) comp : null;
                            if (source == null || (source.getIcon() == null && (source.getText() == null || source.getText().isEmpty()))) {
                                clearSelection();
                                redraw();
                                return;
                            }

                            clearHighlights();
                            depart = new Point(ligne, colonne);
                            draggingPanel = casePanel;
                            casePanel.setBorder(BorderFactory.createLineBorder(Color.YELLOW, 3));

                            if (controller != null) {
                                highlightedMoves.addAll(controller.getLegalDestinations(ligne, colonne));
                            }

                            JLabel ghost = source.getIcon() != null
                                    ? new JLabel(source.getIcon())
                                    : new JLabel(source.getText(), SwingConstants.CENTER);

                            dragWindow = new JWindow();
                            dragWindow.getContentPane().add(ghost);
                            dragWindow.pack();
                            dragOffset = new Point(dragWindow.getWidth() / 2, dragWindow.getHeight() / 2);
                            dragWindow.setLocation(e.getXOnScreen() - dragOffset.x, e.getYOnScreen() - dragOffset.y);
                            dragWindow.setVisible(true);
                            redraw();
                        }
                    }

                    @Override
                    public void mouseReleased(MouseEvent e) {
                        synchronized (jeu) {
                            if (depart != null) {
                                submitMove(new Point(depart), new Point(ligne, colonne));
                            }
                        }
                    }

                    @Override
                    public void mouseEntered(MouseEvent e) {
                        if (!isHighlighted(ligne, colonne)) {
                            casePanel.setBackground(Color.RED);
                        }
                    }

                    @Override
                    public void mouseExited(MouseEvent e) {
                        casePanel.setBackground(backgroundFor(ligne, colonne));
                    }
                });

                casePanel.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
                    @Override
                    public void mouseDragged(MouseEvent e) {
                        if (dragWindow != null && dragOffset != null) {
                            dragWindow.setLocation(e.getXOnScreen() - dragOffset.x, e.getYOnScreen() - dragOffset.y);
                        }
                    }
                });

                chessBoard.add(casePanel);
            }
        }

        JPanel rowPanel = new JPanel(new GridLayout(8, 1));
        for (int i = 8; i >= 1; i--) {
            JLabel lbl = new JLabel(" " + i + " ", SwingConstants.CENTER);
            lbl.setFont(new Font("SansSerif", Font.BOLD, 12));
            rowPanel.add(lbl);
        }

        JPanel colPanel = new JPanel(new GridLayout(1, 8));
        for (char c = 'a'; c <= 'h'; c++) {
            JLabel lbl = new JLabel(String.valueOf(c), SwingConstants.CENTER);
            lbl.setFont(new Font("SansSerif", Font.BOLD, 12));
            colPanel.add(lbl);
        }

        JPanel southContainer = new JPanel(new BorderLayout());
        southContainer.add(new JLabel("   "), BorderLayout.WEST);
        southContainer.add(colPanel, BorderLayout.CENTER);

        statusLabel = new JLabel("Tour : BLANCS", SwingConstants.CENTER);
        statusLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        statusLabel.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));

        panel.add(statusLabel, BorderLayout.NORTH);
        panel.add(rowPanel, BorderLayout.WEST);
        panel.add(chessBoard, BorderLayout.CENTER);
        panel.add(southContainer, BorderLayout.SOUTH);

        add(panel);
        redraw();

        globalMouseListener = evt -> {
            if (!(evt instanceof MouseEvent me)) {
                return;
            }
            if (me.getID() == MouseEvent.MOUSE_RELEASED) {
                handleGlobalMouseReleased(me);
            } else if (me.getID() == MouseEvent.MOUSE_DRAGGED && dragWindow != null && dragOffset != null) {
                dragWindow.setLocation(me.getXOnScreen() - dragOffset.x, me.getYOnScreen() - dragOffset.y);
            }
        };
        Toolkit.getDefaultToolkit().addAWTEventListener(globalMouseListener,
                AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK);
    }

    public JPanel getPanel() {
        return panel;
    }

    private void redraw() {
        int boardW = Math.max(1, chessBoard.getWidth());
        int boardH = Math.max(1, chessBoard.getHeight());
        int cellW = Math.max(1, boardW / 8);
        int cellH = Math.max(1, boardH / 8);
        int iconSize = Math.max(32, Math.min(cellW, cellH));

        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                JPanel casePanel = casePanels[row][col];
                JLabel label = caseLabels[row][col];
                casePanel.setBackground(backgroundFor(row, col));
                if (depart == null || depart.x != row || depart.y != col) {
                    casePanel.setBorder(null);
                }

                Piece piece = jeu.getEchiquier().getPiece(row, col);
                if (piece == null) {
                    label.setIcon(null);
                    label.setText("");
                    continue;
                }

                String resourcePath = resolvePieceResource(piece);
                label.setIcon(createSafeIcon(piece, resourcePath, iconSize));
                label.setText("");
            }
        }

        if (depart != null) {
            casePanels[depart.x][depart.y].setBorder(BorderFactory.createLineBorder(Color.YELLOW, 3));
        }

        panel.revalidate();
        panel.repaint();
    }

    private String resolvePieceResource(Piece piece) {
        String imagePath = piece.getImageName();
        if (imagePath == null || imagePath.isEmpty()) {
            return null;
        }
        String[] exts = {".svg", ".png", ".jpeg", ".jpg"};
        for (String ext : exts) {
            String candidate = "/Pieces/" + imagePath + ext;
            if (getClass().getResource(candidate) != null) {
                return candidate;
            }
        }
        for (String ext : exts) {
            String candidate = "Pieces/" + imagePath + ext;
            if (getClass().getResource(candidate) != null) {
                return candidate;
            }
        }
        return null;
    }

    private Icon createSafeIcon(Piece piece, String resourcePath, int size) {
        if (resourcePath != null) {
            try {
                String lower = resourcePath.toLowerCase();
                if (lower.endsWith(".svg")) {
                    try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
                        if (is != null) {
                            BufferedImage bi = SvgToPngConverter.loadSvgAsImage(is, size, size);
                            if (bi != null) {
                                return new ImageIcon(bi);
                            }
                        }
                    }
                }
                java.net.URL url = getClass().getResource(resourcePath);
                if (url != null) {
                    ImageIcon ii = new ImageIcon(url);
                    if (ii.getIconWidth() > 0 && ii.getIconHeight() > 0) {
                        Image img = ii.getImage().getScaledInstance(size, size, Image.SCALE_SMOOTH);
                        return new ImageIcon(img);
                    }
                }
            } catch (Exception ignored) {
            }
        }

        String initial = piece.getClass().getSimpleName().substring(0, 1).toUpperCase();
        BufferedImage bi = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = bi.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new Color(0, 0, 0, 0));
            g.fillRect(0, 0, size, size);
            g.setColor(piece.isBlanc() ? new Color(255, 255, 255, 230) : new Color(60, 60, 60, 230));
            g.fillOval(2, 2, size - 4, size - 4);
            g.setColor(piece.isBlanc() ? marron : beige);
            Font font = new Font("SansSerif", Font.BOLD, Math.max(12, size / 2));
            g.setFont(font);
            FontMetrics fm = g.getFontMetrics();
            g.drawString(initial, (size - fm.stringWidth(initial)) / 2, (size - fm.getHeight()) / 2 + fm.getAscent());
        } finally {
            g.dispose();
        }
        return new ImageIcon(bi);
    }

    private void handleGlobalMouseReleased(MouseEvent me) {
        synchronized (jeu) {
            if (depart == null) {
                return;
            }
            if (!panel.isShowing()) {
                clearSelection();
                redraw();
                return;
            }
            try {
                Point boardOnScreen = chessBoard.getLocationOnScreen();
                int rx = me.getXOnScreen() - boardOnScreen.x;
                int ry = me.getYOnScreen() - boardOnScreen.y;
                if (rx < 0 || ry < 0 || rx >= chessBoard.getWidth() || ry >= chessBoard.getHeight()) {
                    clearSelection();
                    redraw();
                    return;
                }
                int cellW = Math.max(1, chessBoard.getWidth() / 8);
                int cellH = Math.max(1, chessBoard.getHeight() / 8);
                int col = Math.min(7, rx / cellW);
                int row = Math.min(7, ry / cellH);
                submitMove(new Point(depart), new Point(row, col));
            } catch (IllegalComponentStateException ex) {
                clearSelection();
                redraw();
            }
        }
    }

    private void submitMove(Point source, Point target) {
        if (controller != null) {
            controller.submitMove(source, target);
        } else {
            jeu.setCoup(new Coup(source, target));
        }
        clearSelection();
        redraw();
    }

    private boolean isHighlighted(int row, int col) {
        for (Point point : highlightedMoves) {
            if (point.x == row && point.y == col) {
                return true;
            }
        }
        return false;
    }

    private Color backgroundFor(int row, int col) {
        if (isHighlighted(row, col)) {
            return highlight;
        }
        return ((row + col) % 2 == 0) ? beige : marron;
    }

    private void clearHighlights() {
        highlightedMoves.clear();
    }

    private void clearSelection() {
        clearHighlights();
        depart = null;
        cleanupDrag();
    }

    private void cleanupDrag() {
        if (draggingPanel != null) {
            try {
                draggingPanel.setBorder(null);
            } catch (Exception ignored) {
            }
            draggingPanel = null;
        }
        if (dragWindow != null) {
            try {
                dragWindow.setVisible(false);
                dragWindow.dispose();
            } catch (Exception ignored) {
            }
            dragWindow = null;
            dragOffset = null;
        }
    }

    @Override
    public void update(Observable o, Object arg) {
        SwingUtilities.invokeLater(() -> {
            redraw();
            if (arg instanceof String msg) {
                statusLabel.setForeground(Color.RED);
                statusLabel.setText(msg);
                if (msg.contains("Checkmate") || msg.contains("Stalemate") || msg.contains("lost on time")) {
                    JOptionPane.showMessageDialog(this, msg, "Fin de partie", JOptionPane.INFORMATION_MESSAGE);
                }
                return;
            }

            if (arg instanceof Coup coup) {
                String type = coup.getType();
                Joueur joueur = jeu.getJoueurCourant();
                String nextPlayer = (joueur != null && joueur.isBlanc()) ? "NOIRS" : "BLANCS";
                switch (type == null ? "" : type) {
                    case "ECHEC" -> {
                        statusLabel.setForeground(new Color(180, 0, 0));
                        statusLabel.setText("ECHEC ! - Tour : " + nextPlayer);
                    }
                    case "ROQUE" -> {
                        statusLabel.setForeground(new Color(0, 100, 180));
                        statusLabel.setText("ROQUE - Tour : " + nextPlayer);
                    }
                    case "PRISE EN PASSANT" -> {
                        statusLabel.setForeground(new Color(0, 120, 0));
                        statusLabel.setText("PRISE EN PASSANT - Tour : " + nextPlayer);
                    }
                    case "PROMOTION" -> {
                        statusLabel.setForeground(new Color(120, 0, 120));
                        statusLabel.setText("PROMOTION - Tour : " + nextPlayer);
                    }
                    default -> {
                        statusLabel.setForeground(Color.BLACK);
                        statusLabel.setText("Tour : " + nextPlayer);
                    }
                }
                return;
            }

            statusLabel.setForeground(Color.BLACK);
            Joueur joueur = jeu.getJoueurCourant();
            String current = (joueur != null && joueur.isBlanc()) ? "BLANCS" : "NOIRS";
            statusLabel.setText("Tour : " + current);
        });
    }

    @Override
    public void dispose() {
        try {
            Toolkit.getDefaultToolkit().removeAWTEventListener(globalMouseListener);
        } catch (Exception ignored) {
        }
        super.dispose();
    }
}
