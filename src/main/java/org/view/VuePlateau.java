package org.view;

import org.model.jeu.Coup;
import org.model.jeu.Jeu;
import org.model.jeu.Joueur;
import org.model.piece.Piece;
import org.tools.SvgToPngConverter;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.Observable;
import java.util.Observer;

public class VuePlateau extends JFrame implements Observer {

    private final Jeu jeu;

    private JPanel panel;
    private JLabel label;
    private final JPanel[][] casePanels = new JPanel[8][8];
    private final JLabel[][] caseLabels = new JLabel[8][8];

    final Color BEIGE     = new Color(240, 217, 181);
    final Color MARRON    = new Color(181, 136, 99);
    final Color HIGHLIGHT = new Color(100, 200, 100);

    public VuePlateau(Jeu jeu) {
        this.jeu = jeu;

        setTitle("Jeu d'échecs");
        setSize(650, 680);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        panel = new JPanel(new BorderLayout());
        JPanel echiquier = new JPanel(new GridLayout(8, 8));

        for (int l = 0; l < 8; l++) {
            for (int c = 0; c < 8; c++) {
                JPanel casePanel = new JPanel(new BorderLayout());
                casePanels[l][c] = casePanel;
                JLabel label = new JLabel("", SwingConstants.CENTER);
                caseLabels[l][c] = label;
                casePanel.add(label, BorderLayout.CENTER);
                echiquier.add(casePanel);
            }
        }

        JPanel lignesPanel = new JPanel(new GridLayout(8, 1));
        for (int i = 8; i >= 1; i--) {
            JLabel lbl = new JLabel(" " + i + " ", SwingConstants.CENTER);
            lbl.setFont(new Font("SansSerif", Font.BOLD, 12));
            lignesPanel.add(lbl);
        }
        JPanel colPanel = new JPanel(new GridLayout(1, 8));
        for (char c = 'a'; c <= 'h'; c++) {
            JLabel lbl = new JLabel(String.valueOf(c), SwingConstants.CENTER);
            lbl.setFont(new Font("SansSerif", Font.BOLD, 12));
            colPanel.add(lbl);
        }
        JPanel conteneur = new JPanel(new BorderLayout());
        conteneur.add(new JLabel("   "), BorderLayout.WEST);
        conteneur.add(colPanel, BorderLayout.CENTER);

        label = new JLabel("Tour : BLANCS", SwingConstants.CENTER);
        label.setFont(new Font("SansSerif", Font.BOLD, 14));
        label.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));

        panel.add(label, BorderLayout.NORTH);
        panel.add(lignesPanel, BorderLayout.WEST);
        panel.add(echiquier, BorderLayout.CENTER);
        panel.add(conteneur, BorderLayout.SOUTH);
        add(panel);

        redraw();
    }

    public JPanel getCasePanel(int row, int col) { return casePanels[row][col]; }

    public JLabel getCaseLabel(int row, int col)  { return caseLabels[row][col]; }

    public JPanel getPanel() { return panel; }

    public void setHighlight(int row, int col) {
        casePanels[row][col].setBackground(HIGHLIGHT);
        casePanels[row][col].revalidate();
        casePanels[row][col].repaint();
    }

    public void clearHighlight(int row, int col) {
        casePanels[row][col].setBackground(((row + col) % 2 == 0) ? BEIGE : MARRON);
        casePanels[row][col].revalidate();
        casePanels[row][col].repaint();
    }

    public void setFond(int ligne, int col) {
        casePanels[ligne][col].setBackground(Color.RED);
    }

    public void clearFond(int ligne, int col) {
        casePanels[ligne][col].setBackground(((ligne + col) % 2 == 0) ? BEIGE : MARRON);
    }

    public void setCaseBorder(int ligne, int col, javax.swing.border.Border border) {
        casePanels[ligne][col].setBorder(border);
    }

    @Override
    public void update(Observable o, Object arg) {
        final Joueur joueur = jeu.getJoueurCourant(); // capturé avant invokeLater (joueurCourant pas encore basculé)
        SwingUtilities.invokeLater(() -> {
            redraw();

            if (arg instanceof Coup) {
                Coup c = (Coup) arg;
                String type = c.getType();
                String next = (joueur != null && joueur.isBlanc()) ? "NOIRS" : "BLANCS";
                switch (type == null ? "" : type) {
                    case "ECHEC":
                        label.setForeground(new Color(180, 0, 0));
                        label.setText("ÉCHEC ! — Tour : " + next); break;
                    case "ROQUE":
                        label.setForeground(new Color(0, 100, 180));
                        label.setText("ROQUE — Tour : " + next); break;
                    case "PRISE EN PASSANT":
                        label.setForeground(new Color(0, 120, 0));
                        label.setText("PRISE EN PASSANT — Tour : " + next); break;
                    case "PROMOTION":
                        label.setForeground(new Color(120, 0, 120));
                        label.setText("PROMOTION — Tour : " + next); break;
                    default:
                        label.setForeground(Color.BLACK);
                        label.setText("Tour : " + next);
                }
            } else if (arg instanceof String) {
                label.setForeground(Color.RED);
                label.setText((String) arg);
            } else {
                label.setForeground(Color.BLACK);
                label.setText("Tour : " + (joueur != null && joueur.isBlanc() ? "BLANCS" : "NOIRS"));
            }
        });
    }

    private void redraw() {
        int panelW = Math.max(1, panel.getWidth());
        int panelH = Math.max(1, panel.getHeight());
        int cellW  = Math.max(1, panelW / 8);
        int cellH  = Math.max(1, panelH / 8);
        int iconSize = Math.max(32, Math.min(cellW, cellH));

        for (int l = 0; l < 8; l++) {
            for (int c = 0; c < 8; c++) {
                casePanels[l][c].setBackground(((l + c) % 2 == 0) ? BEIGE : MARRON);
                Piece piece = jeu.getEchiquier().getPiece(l, c); // ← consultation Modèle
                if (piece != null) {
                    String imagePath = piece.getImageName();
                    String resourcePath = null;
                    if (imagePath != null && !imagePath.isEmpty()) {
                        for (String ext : new String[]{".svg", ".png", ".jpeg", ".jpg"}) {
                            String candidate = "/Pieces/" + imagePath + ext;
                            if (getClass().getResource(candidate) != null) { resourcePath = candidate; break; }
                        }
                    }
                    caseLabels[l][c].setIcon(createIcon(piece, resourcePath, iconSize));
                    caseLabels[l][c].setText("");
                } else {
                    caseLabels[l][c].setIcon(null);
                    caseLabels[l][c].setText("");
                }
            }
        }
        panel.revalidate();
        panel.repaint();
    }

    Icon createIcon(Piece piece, String resourcePath, int size) {
        if (resourcePath != null) {
            try {
                if (resourcePath.toLowerCase().endsWith(".svg")) {
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
                    if (ii.getIconWidth() > 0)
                        return new ImageIcon(ii.getImage().getScaledInstance(size, size, Image.SCALE_SMOOTH));
                }
            } catch (Exception ignored) {}
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
            g.setColor(piece.isBlanc() ? MARRON : BEIGE);
            Font font = new Font("SansSerif", Font.BOLD, Math.max(12, size / 2));
            g.setFont(font);
            FontMetrics fm = g.getFontMetrics();
            g.drawString(initial, (size - fm.stringWidth(initial)) / 2, (size - fm.getHeight()) / 2 + fm.getAscent());
        } finally { g.dispose(); }
        return new ImageIcon(bi);
    }
}
