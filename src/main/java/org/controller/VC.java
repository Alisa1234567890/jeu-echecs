package org.controller;

import org.model.Coup;
import org.model.Jeu;
import org.model.JeuObserver;
import org.model.piece.Piece;
import org.model.plateau.Case;
import org.tools.SvgToPngConverter;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JWindow;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.IllegalComponentStateException;
import java.awt.Image;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class VC extends JFrame implements JeuObserver {

    private final Jeu jeu;
    private final JPanel[][] casePanels = new JPanel[8][8];
    private final JLabel[][] caseLabels = new JLabel[8][8];
    private final List<Point> highlightedMoves = new ArrayList<>();

    private JPanel panel;
    private Point depart;
    private JPanel draggingPanel;
    private JWindow dragWindow;
    private Point dragOffset;
    private AWTEventListener globalMouseListener;

    private final Color beige = new Color(220, 234, 248);
    private final Color marron = new Color(88, 123, 168);

    public VC(Jeu jeu) {
        this.jeu = jeu;
        jeu.addObserver(this);

        setTitle("Jeu d'echecs");
        setSize(600, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        panel = new JPanel(new GridLayout(8, 8));
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                JPanel casePanel = new JPanel(new BorderLayout());
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
                            if (!jeu.canSelectPiece(ligne, colonne)) {
                                clearSelection();
                                redraw();
                                return;
                            }

                            depart = new Point(ligne, colonne);
                            draggingPanel = casePanel;
                            casePanel.setBorder(BorderFactory.createLineBorder(Color.YELLOW, 3));
                            updateHighlights(ligne, colonne);

                            Component comp = casePanel.getComponentCount() > 0 ? casePanel.getComponent(0) : null;
                            JLabel source = (comp instanceof JLabel) ? (JLabel) comp : null;
                            if (source == null || (source.getIcon() == null && (source.getText() == null || source.getText().isEmpty()))) {
                                draggingPanel = null;
                                return;
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
                            casePanel.setBackground(new Color(162, 207, 255));
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

                panel.add(casePanel);
            }
        }

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
        int panelW = Math.max(1, panel.getWidth());
        int panelH = Math.max(1, panel.getHeight());
        int cellW = Math.max(1, panelW / 8);
        int cellH = Math.max(1, panelH / 8);
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
                Icon icon = createSafeIcon(resourcePath, iconSize);
                label.setIcon(icon);
                label.setText(icon == null ? getFallbackText(piece) : "");
            }
        }

        if (depart != null) {
            casePanels[depart.x][depart.y].setBorder(BorderFactory.createLineBorder(Color.YELLOW, 3));
        }

        panel.revalidate();
        panel.repaint();
    }

    private void updateHighlights(int row, int col) {
        highlightedMoves.clear();
        highlightedMoves.addAll(jeu.getLegalDestinations(row, col));
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
            return jeu.getEchiquier().getCouleurSurvol();
        }
        return ((row + col) % 2 == 0) ? beige : marron;
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

    private Icon createSafeIcon(String resourcePath, int size) {
        if (resourcePath != null) {
            try {
                if (resourcePath.toLowerCase().endsWith(".svg")) {
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
        return null;
    }

    private String getFallbackText(Piece piece) {
        String simpleName = piece.getClass().getSimpleName();
        return (simpleName == null || simpleName.isEmpty()) ? "?" : simpleName.substring(0, 1).toUpperCase();
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
                Point panelOnScreen = panel.getLocationOnScreen();
                int rx = me.getXOnScreen() - panelOnScreen.x;
                int ry = me.getYOnScreen() - panelOnScreen.y;

                if (rx < 0 || ry < 0 || rx >= panel.getWidth() || ry >= panel.getHeight()) {
                    clearSelection();
                    redraw();
                    return;
                }

                int cellW = Math.max(1, panel.getWidth() / 8);
                int cellH = Math.max(1, panel.getHeight() / 8);
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
        jeu.setCoup(new Coup(source, target));
        clearSelection();
        redraw();
    }

    private void clearSelection() {
        highlightedMoves.clear();
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
    public void update(Object arg) {
        SwingUtilities.invokeLater(this::redraw);
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
