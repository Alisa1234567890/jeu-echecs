package org.controller;

import org.model.Coup;
import org.model.Jeu;
import org.model.piece.Piece;
import org.model.plateau.Case;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

/**
 * CONTRÔLEUR — responsabilités strictes MVC :
 *   (1) Récupère les événements Swing (MouseListeners enregistrés sur la Vue)
 *   (2) Répercussions locales directes sur la Vue sans exploitation du Modèle
 *       (surlignage, bordure de sélection, fenêtre fantôme de drag)
 *   (3) Déclenche les traitements du Modèle (jeu.setCoup, jeu.nouvellePartie)
 *
 * Le Contrôleur est aussi Observer pour :
 *   – réinitialiser son état interne après chaque coup (supprime les highlights
 *     restants non encore nettoyés)
 *   – afficher le dialogue de fin de partie et déclencher une nouvelle partie
 */
public class MF extends JFrame implements Observer {

    // ── Références ────────────────────────────────────────────────────────────
    private Jeu  jeu;
    private VC   vue;

    // ── État interne du Contrôleur (interaction en cours) ────────────────────
    private Point  depart;
    private JPanel draggingPanel;
    private JWindow dragWindow;
    private Point  dragOffset;
    private final List<Point> casesHighlightees = new ArrayList<>();

    // ── Listener global pour le drag en dehors de l'échiquier ────────────────
    private AWTEventListener globalMouseListener;

    // ── Composant d'affichage interne du Contrôleur ──────────────────────────
    private JLabel    statusLabel;
    private JPanel    boardHolder;

    // ─────────────────────────────────────────────────────────────────────────

    public MF() {
        super("Jeu d'Échecs");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(700, 750);
        setLayout(new BorderLayout());

        statusLabel = new JLabel("Contrôleur initialisé", SwingConstants.CENTER);
        statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        add(statusLabel, BorderLayout.NORTH);

        boardHolder = new JPanel(new BorderLayout());
        add(boardHolder, BorderLayout.CENTER);
    }

    // ── Injection des dépendances (appelé depuis Main) ────────────────────────

    public void setJeu(Jeu j) {
        this.jeu = j;
    }

    /**
     * Intègre la Vue dans la fenêtre et enregistre les MouseListeners
     * sur tous les panneaux de cases — étape (1) du MVC.
     */
    public void setVC(VC vc) {
        this.vue = vc;

        // Intégration graphique
        boardHolder.removeAll();
        boardHolder.add(vc.getPanel(), BorderLayout.CENTER);
        revalidate();
        repaint();

        // ── Étape (1) : enregistrement des événements Swing ──────────────────
        for (int l = 0; l < 8; l++) {
            for (int c = 0; c < 8; c++) {
                final int ligne   = l;
                final int colonne = c;
                JPanel casePanel  = vc.getCasePanel(l, c);

                casePanel.addMouseListener(new MouseAdapter() {
                    @Override public void mousePressed (MouseEvent e) { onPressed (ligne, colonne, e); }
                    @Override public void mouseReleased(MouseEvent e) { onReleased(ligne, colonne, e); }
                    @Override public void mouseEntered (MouseEvent e) { onEntered (ligne, colonne);    }
                    @Override public void mouseExited  (MouseEvent e) { onExited  (ligne, colonne);    }
                });

                casePanel.addMouseMotionListener(new MouseMotionAdapter() {
                    @Override public void mouseDragged(MouseEvent e) { onDragged(e); }
                });
            }
        }

        // ── Listener global pour détecter le relâchement hors plateau ────────
        globalMouseListener = evt -> {
            if (!(evt instanceof MouseEvent)) return;
            MouseEvent me = (MouseEvent) evt;
            if      (me.getID() == MouseEvent.MOUSE_RELEASED) onGlobalReleased(me);
            else if (me.getID() == MouseEvent.MOUSE_DRAGGED && dragWindow != null && dragOffset != null) {
                dragWindow.setLocation(me.getXOnScreen() - dragOffset.x,
                                       me.getYOnScreen() - dragOffset.y);
            }
        };
        Toolkit.getDefaultToolkit().addAWTEventListener(globalMouseListener,
                AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK);

        // ── Menu « Partie → Nouvelle partie » (étape 1 + déclenchement étape 3)
        JMenuBar menuBar = new JMenuBar();
        JMenu menuPartie = new JMenu("Partie");
        JMenuItem newGame = new JMenuItem("Nouvelle partie");
        newGame.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.META_DOWN_MASK));
        newGame.addActionListener(e ->                          // étape (1)
                new Thread(jeu::nouvellePartie, "Reset-Thread").start() // étape (3)
        );
        menuPartie.add(newGame);
        menuBar.add(menuPartie);
        setJMenuBar(menuBar);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Étape (1) → (2) → (3)  Handlers des événements Swing
    // ══════════════════════════════════════════════════════════════════════════

    /** (1) Pression de souris — sélection de la pièce. */
    private void onPressed(int ligne, int colonne, MouseEvent e) {
        synchronized (jeu) {
            // (2) Effacer les anciens surlignages (effet local, pas de Modèle)
            clearAllHighlights();

            JLabel source = vue.getCaseLabel(ligne, colonne);
            if (source.getIcon() == null && (source.getText() == null || source.getText().isEmpty())) {
                return; // case vide
            }

            depart        = new Point(ligne, colonne);
            draggingPanel = vue.getCasePanel(ligne, colonne);

            // (2) Bordure jaune sur la case sélectionnée
            vue.setCaseBorder(ligne, colonne, BorderFactory.createLineBorder(Color.YELLOW, 3));

            // (3) Interroger le Modèle pour les cases accessibles, puis (2) les surligner
            Piece piece = jeu.getEchiquier().getPiece(ligne, colonne);
            if (piece != null) {
                System.out.println("MF: compute targets for " + piece.getClass().getSimpleName()
                        + " at (" + ligne + "," + colonne + ") color=" + (piece.isBlanc() ? "W" : "B"));
                for (Case tgt : piece.getCaseAccessible()) {
                    if (tgt == null) continue;
                    int x = tgt.getX(), y = tgt.getY();
                    if (x >= 0 && x < 8 && y >= 0 && y < 8) {
                        vue.setHighlight(x, y);            // (2) effet local direct
                        casesHighlightees.add(new Point(x, y));
                    }
                }
            }

            // (2) Fenêtre fantôme de drag (effet visuel local, pas de Modèle)
            Icon icon = source.getIcon();
            JLabel ghost = (icon != null)
                    ? new JLabel(icon)
                    : new JLabel(source.getText(), SwingConstants.CENTER);
            dragWindow = new JWindow();
            dragWindow.getContentPane().add(ghost);
            dragWindow.pack();
            dragOffset = new Point(dragWindow.getWidth() / 2, dragWindow.getHeight() / 2);
            dragWindow.setLocation(e.getXOnScreen() - dragOffset.x, e.getYOnScreen() - dragOffset.y);
            dragWindow.setVisible(true);
        }
    }

    /** (1) Relâchement de souris — soumission du coup au Modèle. */
    private void onReleased(int ligne, int colonne, MouseEvent e) {
        synchronized (jeu) {
            clearAllHighlights(); // (2) nettoyage immédiat
            if (depart != null) {
                jeu.setCoup(new Coup(depart, new Point(ligne, colonne))); // (3) → Modèle
                depart = null;
            }
            cleanupDrag(); // (2) suppression fenêtre fantôme
        }
    }

    /** (1) Survol — effet de hover local. */
    private void onEntered(int ligne, int colonne) {
        if (!casesHighlightees.contains(new Point(ligne, colonne)))
            vue.setHover(ligne, colonne); // (2) couleur rouge, pas de Modèle
    }

    /** (1) Sortie de case — restauration couleur. */
    private void onExited(int ligne, int colonne) {
        if (!casesHighlightees.contains(new Point(ligne, colonne)))
            vue.clearHover(ligne, colonne); // (2) effet local direct
    }

    /** (1) Drag — déplacement de la fenêtre fantôme. */
    private void onDragged(MouseEvent e) {
        if (dragWindow != null && dragOffset != null)
            dragWindow.setLocation(e.getXOnScreen() - dragOffset.x,
                                   e.getYOnScreen() - dragOffset.y); // (2)
    }

    /** (1) Relâchement global (hors plateau) — calcule la case cible. */
    private void onGlobalReleased(MouseEvent me) {
        synchronized (jeu) {
            if (depart == null) return;
            JPanel boardPanel = vue.getPanel();
            if (!boardPanel.isShowing()) { cleanupDrag(); depart = null; return; }
            try {
                Point origin = boardPanel.getLocationOnScreen();
                int rx = me.getXOnScreen() - origin.x;
                int ry = me.getYOnScreen() - origin.y;
                if (rx < 0 || ry < 0 || rx >= boardPanel.getWidth() || ry >= boardPanel.getHeight()) {
                    cleanupDrag(); depart = null; return;
                }
                int col = Math.min(7, rx / Math.max(1, boardPanel.getWidth()  / 8));
                int row = Math.min(7, ry / Math.max(1, boardPanel.getHeight() / 8));
                clearAllHighlights(); // (2)
                jeu.setCoup(new Coup(new Point(depart.x, depart.y), new Point(row, col))); // (3)
                depart = null;
                cleanupDrag(); // (2)
            } catch (IllegalComponentStateException ex) {
                cleanupDrag(); depart = null;
            }
        }
    }

    // ── Helpers locaux (étape 2 uniquement) ──────────────────────────────────

    private void clearAllHighlights() {
        for (Point p : casesHighlightees) {
            vue.clearHighlight(p.x, p.y);           // (2)
        }
        casesHighlightees.clear();
        if (draggingPanel != null) {
            // Chercher la case de départ pour enlever la bordure
            if (depart != null) vue.setCaseBorder(depart.x, depart.y, null);
            draggingPanel = null;
        }
    }

    private void cleanupDrag() {
        if (depart != null) { vue.setCaseBorder(depart.x, depart.y, null); } // (2)
        draggingPanel = null;
        if (dragWindow != null) {
            try { dragWindow.setVisible(false); dragWindow.dispose(); } catch (Exception ignored) {}
            dragWindow = null;
            dragOffset = null;
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Observer — réception des notifications du Modèle (étape 4)
    // ══════════════════════════════════════════════════════════════════════════

    @Override
    public void update(Observable o, Object arg) {
        if (arg instanceof String) {
            // Fin de partie : le Contrôleur affiche le dialogue et déclenche
            // éventuellement une nouvelle partie (étape 3)
            String msg = (String) arg;
            SwingUtilities.invokeLater(() -> {
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
                    new Thread(jeu::nouvellePartie, "Reset-Thread").start(); // (3)
                } else {
                    System.exit(0);
                }
            });
        } else if (arg == null) {
            // Nouvelle partie : réinitialiser l'état interne du Contrôleur
            SwingUtilities.invokeLater(() -> {
                depart = null;
                casesHighlightees.clear();
                statusLabel.setText("Nouvelle partie — Tour des BLANCS");
            });
        } else {
            SwingUtilities.invokeLater(() -> statusLabel.setText("Mise à jour reçue"));
        }
    }

    @Override
    public void dispose() {
        if (globalMouseListener != null)
            try { Toolkit.getDefaultToolkit().removeAWTEventListener(globalMouseListener); }
            catch (Exception ignored) {}
        super.dispose();
    }
}
