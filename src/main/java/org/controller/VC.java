package org.controller;

import org.model.Coup;
import org.model.Jeu;
import org.model.Joueur;
import org.model.piece.Piece;
import org.tools.SvgToPngConverter;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.Observable;
import java.util.Observer;

/**
 * VUE — responsabilités strictes MVC :
 *   • (4) Reçoit les notifications du Modèle via update()
 *   • (5) Consulte le Modèle (jeu.getEchiquier()) pour mettre à jour l'affichage
 *
 * La Vue n'écoute aucun événement Swing, ne modifie pas le Modèle et
 * n'appelle aucune méthode de traitement (setCoup, nouvellePartie…).
 * Elle expose des méthodes que le Contrôleur peut appeler pour des
 * effets locaux directs (étape 2 : surlignage, bordures, hover).
 */
public class VC extends JFrame implements Observer {

    // ── Référence au Modèle (lecture seule dans redraw) ──────────────────────
    private final Jeu jeu;

    // ── Composants Swing ─────────────────────────────────────────────────────
    private JPanel panel;
    private JLabel statusLabel;
    private final JPanel[][] casePanels = new JPanel[8][8];
    private final JLabel[][] caseLabels = new JLabel[8][8];

    // ── Couleurs (accessibles par le Contrôleur pour les effets locaux) ───────
    final Color BEIGE     = new Color(240, 217, 181);
    final Color MARRON    = new Color(181, 136, 99);
    final Color HIGHLIGHT = new Color(100, 200, 100);

    // ─────────────────────────────────────────────────────────────────────────

    public VC(Jeu jeu) {
        this.jeu = jeu;

        setTitle("Jeu d'échecs");
        setSize(650, 680);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        panel = new JPanel(new BorderLayout());
        JPanel chessBoard = new JPanel(new GridLayout(8, 8));

        // Construire la grille — AUCUN MouseListener ici (rôle du Contrôleur)
        for (int l = 0; l < 8; l++) {
            for (int c = 0; c < 8; c++) {
                JPanel casePanel = new JPanel(new BorderLayout());
                casePanels[l][c] = casePanel;
                JLabel label = new JLabel("", SwingConstants.CENTER);
                caseLabels[l][c] = label;
                casePanel.add(label, BorderLayout.CENTER);
                chessBoard.add(casePanel);
            }
        }

        // Étiquettes de colonnes et de rangées
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

        // Barre de statut
        statusLabel = new JLabel("Tour : BLANCS", SwingConstants.CENTER);
        statusLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        statusLabel.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));

        panel.add(statusLabel, BorderLayout.NORTH);
        panel.add(rowPanel, BorderLayout.WEST);
        panel.add(chessBoard, BorderLayout.CENTER);
        panel.add(southContainer, BorderLayout.SOUTH);
        add(panel);

        redraw();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Accesseurs exposés au Contrôleur pour les effets locaux directs (étape 2)
    // ══════════════════════════════════════════════════════════════════════════

    /** Le Contrôleur y attache ses MouseListeners (étape 1). */
    public JPanel getCasePanel(int row, int col) { return casePanels[row][col]; }

    /** Le Contrôleur lit l'icône pour créer la fenêtre fantôme de drag. */
    public JLabel getCaseLabel(int row, int col)  { return caseLabels[row][col]; }

    public JPanel getPanel() { return panel; }

    /** Effet local direct — surlignage vert d'une case (étape 2). */
    public void setHighlight(int row, int col) {
        casePanels[row][col].setBackground(HIGHLIGHT);
        casePanels[row][col].revalidate();
        casePanels[row][col].repaint();
    }

    /** Effet local direct — suppression du surlignage (étape 2). */
    public void clearHighlight(int row, int col) {
        casePanels[row][col].setBackground(((row + col) % 2 == 0) ? BEIGE : MARRON);
        casePanels[row][col].revalidate();
        casePanels[row][col].repaint();
    }

    /** Effet local direct — couleur de survol (étape 2). */
    public void setHover(int row, int col) {
        casePanels[row][col].setBackground(Color.RED);
    }

    /** Effet local direct — restauration couleur normale (étape 2). */
    public void clearHover(int row, int col) {
        casePanels[row][col].setBackground(((row + col) % 2 == 0) ? BEIGE : MARRON);
    }

    /** Effet local direct — bordure de sélection (étape 2). */
    public void setCaseBorder(int row, int col, javax.swing.border.Border border) {
        casePanels[row][col].setBorder(border);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Observer — étapes 4 (notification) et 5 (consultation du Modèle)
    // ══════════════════════════════════════════════════════════════════════════

    @Override
    public void update(Observable o, Object arg) {
        SwingUtilities.invokeLater(() -> {
            redraw(); // étape 5 : consultation de jeu.getEchiquier()

            if (arg instanceof Coup) {
                Coup c = (Coup) arg;
                String type = c.getType();
                Joueur joueur = jeu.getJoueurCourant();
                String next = (joueur != null && joueur.isBlanc()) ? "NOIRS" : "BLANCS";
                switch (type == null ? "" : type) {
                    case "ECHEC":
                        statusLabel.setForeground(new Color(180, 0, 0));
                        statusLabel.setText("ÉCHEC ! — Tour : " + next); break;
                    case "ROQUE":
                        statusLabel.setForeground(new Color(0, 100, 180));
                        statusLabel.setText("ROQUE — Tour : " + next); break;
                    case "PRISE EN PASSANT":
                        statusLabel.setForeground(new Color(0, 120, 0));
                        statusLabel.setText("PRISE EN PASSANT — Tour : " + next); break;
                    case "PROMOTION":
                        statusLabel.setForeground(new Color(120, 0, 120));
                        statusLabel.setText("PROMOTION — Tour : " + next); break;
                    default:
                        statusLabel.setForeground(Color.BLACK);
                        statusLabel.setText("Tour : " + next);
                }
            } else if (arg instanceof String) {
                // Fin de partie : mise à jour de la barre de statut uniquement.
                // Le dialogue est géré par le Contrôleur (MF).
                statusLabel.setForeground(Color.RED);
                statusLabel.setText((String) arg);
            } else {
                // null → nouvelle partie / redraw simple
                statusLabel.setForeground(Color.BLACK);
                Joueur joueur = jeu.getJoueurCourant();
                statusLabel.setText("Tour : " + (joueur != null && joueur.isBlanc() ? "BLANCS" : "NOIRS"));
            }
        });
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Rendu interne — étape 5 (la Vue consulte le Modèle)
    // ══════════════════════════════════════════════════════════════════════════

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
                    caseLabels[l][c].setIcon(createSafeIcon(piece, resourcePath, iconSize));
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

    Icon createSafeIcon(Piece piece, String resourcePath, int size) {
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
        // Repli : lettre dans un cercle
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
