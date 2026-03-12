package org.controller;

import org.model.*;
import org.tools.SvgToPngConverter;

import javax.swing.*;
import java.awt.*;
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
    private Point depart;
    private JPanel panel;
    private JPanel draggingPanel;
    private JWindow dragWindow;
    private Point dragOffset;
    private AWTEventListener globalMouseListener;

    // reuse components to avoid structural changes during repaint
    private final JPanel[][] casePanels = new JPanel[8][8];
    private final JLabel[][] caseLabels = new JLabel[8][8];

    public VC(Jeu jeu) {
        this.jeu = jeu;
        jeu.addObserver(this);

        setTitle("Jeu d'échecs");
        setSize(600, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        panel = new JPanel(new GridLayout(8, 8));
        for (int l = 0; l < 8; l++) {
            for (int c = 0; c < 8; c++) {
                JPanel casePanel = new JPanel(new BorderLayout());
                casePanels[l][c] = casePanel;
                JLabel label = new JLabel("", SwingConstants.CENTER);
                caseLabels[l][c] = label;
                casePanel.add(label, BorderLayout.CENTER);

                final int ligne = l;
                final int colonne = c;

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
                        Color couleurOriginale = ((ligne + colonne) % 2 == 0) ? Color.WHITE : Color.BLACK;
                        casePanel.setBackground(couleurOriginale);
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
        // Update existing components in-place to avoid structural changes
        int panelW = Math.max(1, panel.getWidth());
        int panelH = Math.max(1, panel.getHeight());
        int cellW = Math.max(1, panelW / 8);
        int cellH = Math.max(1, panelH / 8);
        int iconSize = Math.max(32, Math.min(cellW, cellH));

        for (int l = 0; l < 8; l++) {
            for (int c = 0; c < 8; c++) {
                JPanel casePanel = casePanels[l][c];
                JLabel label = caseLabels[l][c];

                Color couleurOriginale = ((l + c) % 2 == 0) ? Color.WHITE : Color.BLACK;
                casePanel.setBackground(couleurOriginale);

                Piece piece = jeu.getEchiquier().getPiece(l, c);
                if (piece != null) {
                    String imagePath = piece.getImageName();
                    String resourcePath = null;
                    java.net.URL url = null;

                    if (imagePath != null && !imagePath.isEmpty()) {
                        // build candidates from base name and several extensions/prefixes
                        String base = imagePath;
                        int dot = base.lastIndexOf('.');
                        String withoutExt = (dot > 0) ? base.substring(0, dot) : base;
                        // prefer raster formats first, then svg
                        String[] exts = new String[]{".png", ".jpeg", ".jpg", ".svg", ""};
                        String[] prefixes = new String[]{"/", "/Pieces/", "/pieces/", ""};

                        outer:
                        for (String pref : prefixes) {
                            for (String ext : exts) {
                                String cand = pref + withoutExt + ext;
                                url = getClass().getResource(cand);
                                if (url != null) {
                                    resourcePath = cand;
                                    break outer;
                                }
                                // also try cand using the original full imagePath (in case imagePath already had directory)
                                cand = pref + base;
                                url = getClass().getResource(cand);
                                if (url != null) {
                                    resourcePath = cand;
                                    break outer;
                                }
                            }
                        }
                    }

                    // Debug log: which resource path was chosen for this piece
                    System.out.println("VC: piece at (" + l + "," + c + ") type=" + piece.getClass().getSimpleName() + " -> resourcePath=" + resourcePath);

                    Icon icon = createSafeIcon(piece, resourcePath, iconSize);
                    label.setIcon(icon);
                    label.setText("");
                } else {
                    label.setIcon(null);
                    label.setText("");
                }
            }
        }

        panel.revalidate();
        panel.repaint();
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
                                System.out.println("VC: loaded SVG resource=" + resourcePath + " for piece=" + piece.getClass().getSimpleName());
                                return new ImageIcon(bi);
                            }
                        }
                    } catch (Exception ex) {
                        // fallback below
                    }
                }

                java.net.URL url = getClass().getResource(resourcePath);
                if (url != null) {
                    ImageIcon ii = new ImageIcon(url);
                    if (ii.getIconWidth() > 0 && ii.getIconHeight() > 0) {
                        System.out.println("VC: loaded raster resource=" + resourcePath + " for piece=" + piece.getClass().getSimpleName());
                        Image img = ii.getImage().getScaledInstance(size, size, Image.SCALE_SMOOTH);
                        return new ImageIcon(img);
                    }
                }
            } catch (Exception ignored) {
            }
        }

        // Fallback: generate an icon with piece initial
        System.out.println("VC: fallback icon for piece=" + piece.getClass().getSimpleName() + " resource=" + resourcePath);
        String initial = piece.getClass().getSimpleName();
        initial = (initial == null || initial.isEmpty()) ? "?" : initial.substring(0, 1).toUpperCase();
        BufferedImage bi = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = bi.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            // background
            g.setColor(new Color(0,0,0,0));
            g.fillRect(0,0,size,size);
            // circle
            if (piece.isBlanc()) g.setColor(new Color(255,255,255,230)); else g.setColor(new Color(60,60,60,230));
            g.fillOval(2,2,size-4,size-4);
            // letter
            g.setColor(piece.isBlanc() ? Color.BLACK : Color.WHITE);
            Font font = new Font("SansSerif", Font.BOLD, Math.max(12, size/2));
            g.setFont(font);
            FontMetrics fm = g.getFontMetrics();
            int tx = (size - fm.stringWidth(initial)) / 2;
            int ty = (size - fm.getHeight()) / 2 + fm.getAscent();
            g.drawString(initial, tx, ty);
        } finally {
            g.dispose();
        }
        return new ImageIcon(bi);
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
