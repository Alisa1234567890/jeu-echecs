package org.controller;

import org.model.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.AWTEventListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.Observable;
import java.util.Observer;

public class VC extends JFrame implements Observer {

    private final Jeu jeu;
    private Point depart;
    private JPanel panel;
    private JPanel draggingPanel;
    private JWindow dragWindow;
    private Point dragOffset;
    private AWTEventListener globalMouseListener;
    private boolean[][] allowed;

    public VC(Jeu jeu) {
        this.jeu = jeu;
        jeu.addObserver(this);
        allowed = new boolean[8][8];

        setTitle("Jeu d'échecs");
        setSize(600, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        panel = new JPanel(new GridLayout(8, 8));
        add(panel);

        redraw();

        // Register a global AWT listener so we catch mouse releases even when the cursor
        // is outside the original case panel (the drag ghost may be in a JWindow).
        globalMouseListener = evt -> {
            if (!(evt instanceof java.awt.event.MouseEvent)) return;
            MouseEvent me = (MouseEvent) evt;
            if (me.getID() == MouseEvent.MOUSE_RELEASED) {
                handleGlobalMouseReleased(me);
            } else if (me.getID() == MouseEvent.MOUSE_DRAGGED) {
                // update ghost location while dragging anywhere
                if (dragWindow != null && dragOffset != null) {
                    int nx = me.getXOnScreen() - dragOffset.x;
                    int ny = me.getYOnScreen() - dragOffset.y;
                    dragWindow.setLocation(nx, ny);
                }
            }
        };
        Toolkit.getDefaultToolkit().addAWTEventListener(globalMouseListener,
                AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK);
    }


    public JPanel getPanel() {
        return panel;
    }

    private void redraw() {
        panel.removeAll();

        for (int l = 0; l < 8; l++) {
            for (int c = 0; c < 8; c++) {

                JPanel casePanel = new JPanel(new BorderLayout());
                Color couleurOriginale =
                        (l + c) % 2 == 0 ? Color.WHITE : Color.BLACK;
                Color bg = allowed[l][c] ? Color.GREEN : couleurOriginale;
                casePanel.setBackground(bg);

                int ligne = l;
                int colonne = c;


                Piece piece = jeu.getEchiquier().getPiece(ligne, colonne);
                if (piece != null) {
                    String imagePath = piece.getImageName();
                    java.net.URL url = null;
                    if (imagePath != null && !imagePath.isEmpty()) {
                        String[] candidates = new String[] {
                                "/" + imagePath,
                                "/pieces/" + imagePath,
                                "/Pieces/" + imagePath,
                                imagePath
                        };
                        for (String pth : candidates) {
                            url = getClass().getResource(pth);
                            if (url != null) break;
                        }
                    }

                    JLabel label;
                    if (url != null) {
                        label = new JLabel(new ImageIcon(url));
                    } else {

                        String initial = piece.getClass().getSimpleName();
                        initial = initial.isEmpty() ? "?" : initial.substring(0, 1);
                        label = new JLabel(initial, SwingConstants.CENTER);
                    }
                    casePanel.add(label);
                }

                // Mouse handling for drag-and-drop: press to start, drag to move ghost, release to finish
                casePanel.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mousePressed(MouseEvent e) {
                        synchronized (jeu) {
                            depart = new Point(ligne, colonne);
                            draggingPanel = casePanel;
                            casePanel.setBorder(BorderFactory.createLineBorder(Color.YELLOW, 3));

                            // compute allowed target squares for visual feedback
                            for (int rr = 0; rr < 8; rr++) for (int cc = 0; cc < 8; cc++) allowed[rr][cc] = false;
                            Piece p = jeu.getEchiquier().getPiece(ligne, colonne);
                            if (p != null) {
                                java.util.ArrayList<Case> acces = p.getCaseAccessible();
                                if (acces != null && !acces.isEmpty()) {
                                    for (Case ac : acces) {
                                        allowed[ac.getX()][ac.getY()] = true;
                                    }
                                } else {
                                    // fallback: check all squares with isValidMove
                                    for (int rr = 0; rr < 8; rr++) {
                                        for (int cc = 0; cc < 8; cc++) {
                                            try {
                                                if (p.isValidMove(ligne, colonne, rr, cc)) allowed[rr][cc] = true;
                                            } catch (Exception ignored) {}
                                        }
                                    }
                                }
                                // force redraw so highlights appear
                                panel.revalidate();
                                panel.repaint();
                            }

                            // create ghost image in a JWindow
                            Component comp = (casePanel.getComponentCount() > 0) ? casePanel.getComponent(0) : null;
                            JLabel source = (comp instanceof JLabel) ? (JLabel) comp : null;
                            // only start a drag if there is a visible piece (icon or text)
                            if (source == null || (source.getIcon() == null && (source.getText() == null || source.getText().isEmpty()))) {
                                // nothing to drag
                                depart = null;
                                draggingPanel = null;
                                return;
                            }

                            JLabel ghost;
                            if (source.getIcon() != null) {
                                Icon icon = source.getIcon();
                                ghost = new JLabel(icon);
                            } else {
                                ghost = new JLabel(source.getText(), SwingConstants.CENTER);
                            }

                            dragWindow = new JWindow();
                            dragWindow.getContentPane().add(ghost);
                            dragWindow.pack();
                            int gw = dragWindow.getWidth();
                            int gh = dragWindow.getHeight();
                            dragOffset = new Point(gw / 2, gh / 2);
                            int sx = e.getXOnScreen() - dragOffset.x;
                            int sy = e.getYOnScreen() - dragOffset.y;
                            dragWindow.setLocation(sx, sy);
                            dragWindow.setVisible(true);
                        }
                    }

                    @Override
                    public void mouseReleased(MouseEvent e) {
                        // keep existing behavior for releases that happen on a case panel
                        synchronized (jeu) {
                            if (depart != null) {
                                Coup coup = new Coup(depart, new Point(ligne, colonne));
                                depart = null;

                                if (draggingPanel != null) {
                                    draggingPanel.setBorder(null);
                                    draggingPanel = null;
                                }

                                // clear allowed highlights
                                for (int rr = 0; rr < 8; rr++) for (int cc = 0; cc < 8; cc++) allowed[rr][cc] = false;
                                updateHighlights();

                                if (dragWindow != null) {
                                    dragWindow.setVisible(false);
                                    dragWindow.dispose();
                                    dragWindow = null;
                                    dragOffset = null;
                                }

                                // apply move through game logic immediately
                                try {
                                    jeu.appliquerCoup(coup);
                                } catch (Exception ex) {
                                    // in case appliquerCoup has threading expectations, fallback to setting nextC
                                    jeu.nextC = coup;
                                    jeu.notify();
                                }
                            }
                        }
                    }

                    @Override
                    public void mouseEntered(MouseEvent e) {
                        casePanel.setBackground(Color.RED);
                    }

                    @Override
                    public void mouseExited(MouseEvent e) {
                        // restore original or allowed highlight
                        casePanel.setBackground(allowed[ligne][colonne] ? Color.GREEN : couleurOriginale);
                    }

                    @Override
                    public void mouseDragged(MouseEvent e) {
                        // Note: MouseAdapter does implement MouseMotionListener so this will be called
                        if (dragWindow != null && dragOffset != null) {
                            int nx = e.getXOnScreen() - dragOffset.x;
                            int ny = e.getYOnScreen() - dragOffset.y;
                            dragWindow.setLocation(nx, ny);
                        }
                    }
                });

                // also add mouse motion listener so dragging over other components updates ghost
                casePanel.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
                    @Override
                    public void mouseDragged(MouseEvent e) {
                        if (dragWindow != null && dragOffset != null) {
                            int nx = e.getXOnScreen() - dragOffset.x;
                            int ny = e.getYOnScreen() - dragOffset.y;
                            dragWindow.setLocation(nx, ny);
                        }
                    }
                });

                panel.add(casePanel);
            }
        }

        panel.revalidate();
        panel.repaint();
    }

    private void handleGlobalMouseReleased(MouseEvent me) {
        // capture global release and translate to a board cell, if appropriate
        synchronized (jeu) {
            if (depart == null) return;
            if (!panel.isShowing()) {
                // panel not visible -> cancel drag
                cleanupDrag();
                depart = null;
                return;
            }

            try {
                Point panelOnScreen = panel.getLocationOnScreen();
                int rx = me.getXOnScreen() - panelOnScreen.x;
                int ry = me.getYOnScreen() - panelOnScreen.y;

                if (rx < 0 || ry < 0 || rx >= panel.getWidth() || ry >= panel.getHeight()) {
                    // released outside board -> cancel drag
                    cleanupDrag();
                    depart = null;
                    return;
                }

                int cellW = Math.max(1, panel.getWidth() / 8);
                int cellH = Math.max(1, panel.getHeight() / 8);
                int col = Math.min(7, rx / cellW);
                int row = Math.min(7, ry / cellH);

                Coup coup = new Coup(new Point(depart.x, depart.y), new Point(row, col));
                depart = null;
                cleanupDrag();
                for (int rr = 0; rr < 8; rr++) for (int cc = 0; cc < 8; cc++) allowed[rr][cc] = false;
                updateHighlights();
                try {
                    jeu.appliquerCoup(coup);
                } catch (Exception ex) {
                    jeu.nextC = coup;
                    jeu.notify();
                }
            } catch (IllegalComponentStateException ex) {
                // if component not showing or location cannot be determined, cancel gracefully
                cleanupDrag();
                depart = null;
            }
        }
    }

    private void cleanupDrag() {
        if (draggingPanel != null) {
            try { draggingPanel.setBorder(null); } catch (Exception ignored) {}
            draggingPanel = null;
        }
        if (dragWindow != null) {
            try { dragWindow.setVisible(false); dragWindow.dispose(); } catch (Exception ignored) {}
            dragWindow = null;
            dragOffset = null;
        }
    }

    @Override
    public void update(Observable o, Object arg) {
        redraw();
    }

    @Override
    public void dispose() {
        // remove global listener to avoid leaking
        try {
            Toolkit.getDefaultToolkit().removeAWTEventListener(globalMouseListener);
        } catch (Exception ignored) {}
        super.dispose();
    }

    private void updateHighlights() {
        if (panel == null) return;
        Component[] comps = panel.getComponents();
        if (comps == null) return;
        int idx = 0;
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                if (idx >= comps.length) continue;
                Component comp = comps[idx++];
                if (!(comp instanceof JPanel)) continue;
                JPanel cp = (JPanel) comp;
                Color couleurOriginale = (r + c) % 2 == 0 ? Color.WHITE : Color.BLACK;
                cp.setBackground(allowed[r][c] ? Color.GREEN : couleurOriginale);
            }
        }
        panel.revalidate();
        panel.repaint();
    }
}
