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

    public VC(Jeu jeu) {
        this.jeu = jeu;
        jeu.addObserver(this);

        setTitle("Jeu d'échecs");
        setSize(600, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        panel = new JPanel(new GridLayout(8, 8));
        add(panel);

        redraw();

        globalMouseListener = evt -> {
            if (!(evt instanceof java.awt.event.MouseEvent)) return;
            MouseEvent me = (MouseEvent) evt;
            if (me.getID() == MouseEvent.MOUSE_RELEASED) {
                handleGlobalMouseReleased(me);
            } else if (me.getID() == MouseEvent.MOUSE_DRAGGED) {
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
                casePanel.setBackground(couleurOriginale);

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

                casePanel.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mousePressed(MouseEvent e) {
                        synchronized (jeu) {
                            depart = new Point(ligne, colonne);
                            draggingPanel = casePanel;
                            casePanel.setBorder(BorderFactory.createLineBorder(Color.YELLOW, 3));

                            Component comp = (casePanel.getComponentCount() > 0) ? casePanel.getComponent(0) : null;
                            JLabel source = (comp instanceof JLabel) ? (JLabel) comp : null;
                            if (source == null || (source.getIcon() == null && (source.getText() == null || source.getText().isEmpty()))) {
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
                        synchronized (jeu) {
                            if (depart != null) {
                                jeu.setCoup(new Coup(depart, new Point(ligne, colonne)));
                                depart = null;

                                if (draggingPanel != null) {
                                    draggingPanel.setBorder(null);
                                    draggingPanel = null;
                                }

                                if (dragWindow != null) {
                                    dragWindow.setVisible(false);
                                    dragWindow.dispose();
                                    dragWindow = null;
                                    dragOffset = null;
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
                        casePanel.setBackground(couleurOriginale);
                    }

                    @Override
                    public void mouseDragged(MouseEvent e) {
                        if (dragWindow != null && dragOffset != null) {
                            int nx = e.getXOnScreen() - dragOffset.x;
                            int ny = e.getYOnScreen() - dragOffset.y;
                            dragWindow.setLocation(nx, ny);
                        }
                    }
                });

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
        synchronized (jeu) {
            if (depart == null) return;
            if (!panel.isShowing()) {
                cleanupDrag();
                depart = null;
                return;
            }

            try {
                Point panelOnScreen = panel.getLocationOnScreen();
                int rx = me.getXOnScreen() - panelOnScreen.x;
                int ry = me.getYOnScreen() - panelOnScreen.y;

                if (rx < 0 || ry < 0 || rx >= panel.getWidth() || ry >= panel.getHeight()) {
                    cleanupDrag();
                    depart = null;
                    return;
                }

                int cellW = Math.max(1, panel.getWidth() / 8);
                int cellH = Math.max(1, panel.getHeight() / 8);
                int col = Math.min(7, rx / cellW);
                int row = Math.min(7, ry / cellH);

                jeu.setCoup(new Coup(new Point(depart.x, depart.y), new Point(row, col)));
                depart = null;
                cleanupDrag();
            } catch (IllegalComponentStateException ex) {
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
        SwingUtilities.invokeLater(this::redraw);
    }

    @Override
    public void dispose() {
        try {
            Toolkit.getDefaultToolkit().removeAWTEventListener(globalMouseListener);
        } catch (Exception ignored) {}
        super.dispose();
    }
}
