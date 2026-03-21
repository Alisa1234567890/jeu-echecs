package org.controller;

import org.model.*;
import org.model.piece.Piece;
import org.tools.SvgToPngConverter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.AWTEventListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.InputStream;
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
    private Color BEIGE = new Color(240, 217, 181);
    private Color MARRON = new Color(181, 136, 99);
    private Color HIGHLIGHT = new Color(100, 200, 100);
    private java.util.List<Point> casesAccessiblesHighlightees = new java.util.ArrayList<>();
    private JLabel statusLabel;

    private final JPanel[][] casePanels = new JPanel[8][8];
    private final JLabel[][] caseLabels = new JLabel[8][8];

    public VC(Jeu jeu) {
        this.jeu = jeu;
        jeu.addObserver(this);

        setTitle("Jeu d'échecs");
        setSize(650, 680);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // ── Barre de menu ──────────────────────────────────────────────────
        JMenuBar menuBar = new JMenuBar();
        JMenu menuPartie = new JMenu("Partie");
        JMenuItem newGameItem = new JMenuItem("Nouvelle partie");
        newGameItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(
                java.awt.event.KeyEvent.VK_N,
                java.awt.event.InputEvent.META_DOWN_MASK)); // Cmd+N sur Mac
        newGameItem.addActionListener(e ->
                // Exécuté hors de l'EDT pour ne pas bloquer l'interface
                new Thread(jeu::nouvellePartie, "Reset-Thread").start()
        );
        menuPartie.add(newGameItem);
        menuBar.add(menuPartie);
        setJMenuBar(menuBar);
        // ───────────────────────────────────────────────────────────────────

        panel = new JPanel(new BorderLayout());

        JPanel chessBoard = new JPanel(new GridLayout(8, 8));

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
                            // clear previous highlights
                            for (Point p : casesAccessiblesHighlightees) {
                                Color couleur = ((p.x + p.y) % 2 == 0) ? BEIGE : MARRON;
                                casePanels[p.x][p.y].setBackground(couleur);
                            }
                            casesAccessiblesHighlightees.clear();

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

                            // Highlight all accessible squares (en passant, castling, promotion
                            // are now computed directly by each piece's getCaseAccessible())
                            Piece piece = jeu.getEchiquier().getPiece(ligne, colonne);
                            if (piece != null) {
                                java.util.List<org.model.plateau.Case> casesAccessibles = piece.getCaseAccessible();
                                System.out.println("VC: compute targets for " + piece.getClass().getSimpleName()
                                        + " at (" + ligne + "," + colonne + ") color=" + (piece.isBlanc() ? "W" : "B"));
                                System.out.print("VC: final targets:");
                                for (org.model.plateau.Case tgt : casesAccessibles) {
                                    if (tgt != null) System.out.print(" (" + tgt.getX() + "," + tgt.getY() + ")");
                                }
                                System.out.println();

                                for (org.model.plateau.Case tgt : casesAccessibles) {
                                    if (tgt == null) continue;
                                    int x = tgt.getX();
                                    int y = tgt.getY();
                                    if (x >= 0 && x < 8 && y >= 0 && y < 8) {
                                        casePanels[x][y].setBackground(HIGHLIGHT);
                                        casesAccessiblesHighlightees.add(new Point(x, y));
                                        try { casePanels[x][y].revalidate(); casePanels[x][y].repaint(); } catch (Exception ignored) {}
                                    }
                                }
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
                            // clear highlights
                            for (Point p : casesAccessiblesHighlightees) {
                                Color couleur = ((p.x + p.y) % 2 == 0) ? BEIGE : MARRON;
                                casePanels[p.x][p.y].setBackground(couleur);
                                try { casePanels[p.x][p.y].revalidate(); casePanels[p.x][p.y].repaint(); } catch (Exception ignored) {}
                            }
                            casesAccessiblesHighlightees.clear();

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
                        if (!casesAccessiblesHighlightees.contains(new Point(ligne, colonne))) {
                            casePanel.setBackground(Color.RED);
                        }
                    }

                    @Override
                    public void mouseExited(MouseEvent e) {
                        if (!casesAccessiblesHighlightees.contains(new Point(ligne, colonne))) {
                            Color couleurOriginale = ((ligne + colonne) % 2 == 0) ? BEIGE : MARRON;
                            casePanel.setBackground(couleurOriginale);
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
        JLabel spacer = new JLabel("   ");
        southContainer.add(spacer, BorderLayout.WEST);
        southContainer.add(colPanel, BorderLayout.CENTER);



        // Status bar
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
        int panelW = Math.max(1, panel.getWidth());
        int panelH = Math.max(1, panel.getHeight());
        int cellW = Math.max(1, panelW / 8);
        int cellH = Math.max(1, panelH / 8);
        int iconSize = Math.max(32, Math.min(cellW, cellH));

        for (int l = 0; l < 8; l++) {
            for (int c = 0; c < 8; c++) {
                JPanel casePanel = casePanels[l][c];
                JLabel label = caseLabels[l][c];

                Color couleurOriginale = ((l + c) % 2 == 0) ? BEIGE : MARRON;
                casePanel.setBackground(couleurOriginale);

                Piece piece = jeu.getEchiquier().getPiece(l, c);
                if (piece != null) {
                    String imagePath = piece.getImageName();
                    String resourcePath = null;

                    if (imagePath != null && !imagePath.isEmpty()) {
                        String[] exts = new String[]{".svg", ".png", ".jpeg", ".jpg"};
                        for (String ext : exts) {
                            String candidate = "/Pieces/" + imagePath + ext;
                            java.net.URL url = getClass().getResource(candidate);
                            if (url != null) { resourcePath = candidate; break; }
                        }
                        if (resourcePath == null) {
                            for (String ext : exts) {
                                String candidate = "Pieces/" + imagePath + ext;
                                java.net.URL url = getClass().getResource(candidate);
                                if (url != null) { resourcePath = candidate; break; }
                            }
                        }
                    }

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
                            if (bi != null) return new ImageIcon(bi);
                        }
                    } catch (Exception ignored) {}
                }
                java.net.URL url = getClass().getResource(resourcePath);
                if (url != null) {
                    ImageIcon ii = new ImageIcon(url);
                    if (ii.getIconWidth() > 0) {
                        Image img = ii.getImage().getScaledInstance(size, size, Image.SCALE_SMOOTH);
                        return new ImageIcon(img);
                    }
                }
            } catch (Exception ignored) {}
        }

        // Fallback: letter in a circle
        String initial = piece.getClass().getSimpleName().substring(0, 1).toUpperCase();
        BufferedImage bi = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = bi.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new Color(0,0,0,0));
            g.fillRect(0,0,size,size);
            g.setColor(piece.isBlanc() ? new Color(255,255,255,230) : new Color(60,60,60,230));
            g.fillOval(2,2,size-4,size-4);
            g.setColor(piece.isBlanc() ? MARRON : BEIGE);
            Font font = new Font("SansSerif", Font.BOLD, Math.max(12, size/2));
            g.setFont(font);
            FontMetrics fm = g.getFontMetrics();
            g.drawString(initial, (size - fm.stringWidth(initial)) / 2, (size - fm.getHeight()) / 2 + fm.getAscent());
        } finally { g.dispose(); }
        return new ImageIcon(bi);
    }

    private void handleGlobalMouseReleased(MouseEvent me) {
        synchronized (jeu) {
            if (depart == null) return;
            if (!panel.isShowing()) { cleanupDrag(); depart = null; return; }
            try {
                Point panelOnScreen = panel.getLocationOnScreen();
                int rx = me.getXOnScreen() - panelOnScreen.x;
                int ry = me.getYOnScreen() - panelOnScreen.y;
                if (rx < 0 || ry < 0 || rx >= panel.getWidth() || ry >= panel.getHeight()) {
                    cleanupDrag(); depart = null; return;
                }
                int cellW = Math.max(1, panel.getWidth() / 8);
                int cellH = Math.max(1, panel.getHeight() / 8);
                int col = Math.min(7, rx / cellW);
                int row = Math.min(7, ry / cellH);
                jeu.setCoup(new Coup(new Point(depart.x, depart.y), new Point(row, col)));
                depart = null;
                cleanupDrag();
            } catch (IllegalComponentStateException ex) {
                cleanupDrag(); depart = null;
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
        SwingUtilities.invokeLater(() -> {
            redraw();
            if (arg instanceof String) {
                // Message de fin de partie
                String msg = (String) arg;
                statusLabel.setForeground(Color.RED);
                statusLabel.setText(msg);

                int choice = JOptionPane.showOptionDialog(
                        this,
                        msg + "\n\nVoulez-vous jouer une nouvelle partie ?",
                        "Fin de partie",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.INFORMATION_MESSAGE,
                        null,
                        new String[]{"Nouvelle partie", "Quitter"},
                        "Nouvelle partie"
                );
                if (choice == 0) {
                    new Thread(jeu::nouvellePartie, "Reset-Thread").start();
                } else if (choice == 1 || choice == JOptionPane.CLOSED_OPTION) {
                    System.exit(0);
                }
            } else if (arg instanceof Coup) {
                Coup c = (Coup) arg;
                String type = c.getType();
                // joueurCourant is still the player who just moved
                Joueur joueur = jeu.getJoueurCourant();
                String nextPlayer = (joueur != null && joueur.isBlanc()) ? "NOIRS" : "BLANCS";
                switch (type == null ? "" : type) {
                    case "ECHEC":
                        statusLabel.setForeground(new Color(180, 0, 0));
                        statusLabel.setText("ÉCHEC ! — Tour : " + nextPlayer);
                        break;
                    case "ROQUE":
                        statusLabel.setForeground(new Color(0, 100, 180));
                        statusLabel.setText("ROQUE — Tour : " + nextPlayer);
                        break;
                    case "PRISE EN PASSANT":
                        statusLabel.setForeground(new Color(0, 120, 0));
                        statusLabel.setText("PRISE EN PASSANT — Tour : " + nextPlayer);
                        break;
                    case "PROMOTION":
                        statusLabel.setForeground(new Color(120, 0, 120));
                        statusLabel.setText("PROMOTION — Tour : " + nextPlayer);
                        break;
                    default:
                        statusLabel.setForeground(Color.BLACK);
                        statusLabel.setText("Tour : " + nextPlayer);
                }
            } else {
                statusLabel.setForeground(Color.BLACK);
                Joueur joueur = jeu.getJoueurCourant();
                String current = (joueur != null && joueur.isBlanc()) ? "BLANCS" : "NOIRS";
                statusLabel.setText("Tour : " + current);
            }
        });
    }

    @Override
    public void dispose() {
        try { Toolkit.getDefaultToolkit().removeAWTEventListener(globalMouseListener); } catch (Exception ignored) {}
        super.dispose();
    }
}
