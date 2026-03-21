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


public class MF extends JFrame implements Observer {

    private Jeu  jeu;
    private VC   vue;

    private Point pointDepart;
    private JPanel jPanel;
    private JWindow jWindow;
    private Point point;
    private final List<Point> casesSurlignes = new ArrayList<>();

    private AWTEventListener awtEventListener;

    private JLabel jLabel;
    private JPanel jPanelBordures;

    public MF() {
        super("Jeu d'Échecs");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(700, 750);
        setLayout(new BorderLayout());

        jLabel = new JLabel("Contrôleur initialisé", SwingConstants.CENTER);
        jLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        add(jLabel, BorderLayout.NORTH);

        jPanelBordures = new JPanel(new BorderLayout());
        add(jPanelBordures, BorderLayout.CENTER);
    }

    public void initJeu(Jeu j) {
        this.jeu = j;
    }

    public void initVC(VC vc) {
        this.vue = vc;

        jPanelBordures.removeAll();
        jPanelBordures.add(vc.getPanel(), BorderLayout.CENTER);
        revalidate();
        repaint();

        for (int l = 0; l < 8; l++) {
            for (int c = 0; c < 8; c++) {
                final int ligne   = l;
                final int colonne = c;
                JPanel casePanel  = vc.getCasePanel(l, c);

                casePanel.addMouseListener(new MouseAdapter() {
                    @Override public void mousePressed (MouseEvent e) { onPressed (ligne, colonne, e); }
                    @Override public void mouseReleased(MouseEvent e) { onReleased(ligne, colonne); }
                    @Override public void mouseEntered (MouseEvent e) { onEntered (ligne, colonne);    }
                    @Override public void mouseExited  (MouseEvent e) { onExited  (ligne, colonne);    }
                });

                casePanel.addMouseMotionListener(new MouseMotionAdapter() {
                    @Override public void mouseDragged(MouseEvent e) { onDragged(e); }
                });
            }
        }

        awtEventListener = evt -> {
            if (!(evt instanceof MouseEvent)) return;
            MouseEvent me = (MouseEvent) evt;
            if      (me.getID() == MouseEvent.MOUSE_RELEASED) onGlobalReleased(me);
            else if (me.getID() == MouseEvent.MOUSE_DRAGGED && jWindow != null && point != null) {
                jWindow.setLocation(me.getXOnScreen() - point.x,
                                       me.getYOnScreen() - point.y);
            }
        };
        Toolkit.getDefaultToolkit().addAWTEventListener(awtEventListener,
                AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK);

        JMenuBar menuBar = new JMenuBar();
        JMenu menuPartie = new JMenu("Partie");
        JMenuItem newGame = new JMenuItem("Nouvelle partie");
        newGame.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.META_DOWN_MASK));
        newGame.addActionListener(e ->
                new Thread(jeu::nouvellePartie, "Reset-Thread").start()
        );
        menuPartie.add(newGame);
        menuBar.add(menuPartie);
        setJMenuBar(menuBar);
    }

    private void onPressed(int ligne, int colonne, MouseEvent e) {
        synchronized (jeu) {
            supprimerCasesSurlignes();

            JLabel source = vue.getCaseLabel(ligne, colonne);
            if (source.getIcon() == null && (source.getText() == null || source.getText().isEmpty())) {
                return;
            }

            pointDepart = new Point(ligne, colonne);
            jPanel = vue.getCasePanel(ligne, colonne);

            vue.setCaseBorder(ligne, colonne, BorderFactory.createLineBorder(Color.YELLOW, 3));

            Piece piece = jeu.getEchiquier().getPiece(ligne, colonne);
            if (piece != null) {
                System.out.println("MF: calculer des cibles pour " + piece.getClass().getSimpleName()
                        + " au (" + ligne + "," + colonne + ") couleur=" + (piece.isBlanc() ? "W" : "B"));
                for (Case tgt : piece.getCaseAccessible()) {
                    if (tgt == null) continue;
                    int x = tgt.getX(), y = tgt.getY();
                    if (x >= 0 && x < 8 && y >= 0 && y < 8) {
                        vue.setHighlight(x, y);
                        casesSurlignes.add(new Point(x, y));
                    }
                }
            }

            Icon icon = source.getIcon();
            JLabel ghost = (icon != null)
                    ? new JLabel(icon)
                    : new JLabel(source.getText(), SwingConstants.CENTER);
            jWindow = new JWindow();
            jWindow.getContentPane().add(ghost);
            jWindow.pack();
            point = new Point(jWindow.getWidth() / 2, jWindow.getHeight() / 2);
            jWindow.setLocation(e.getXOnScreen() - point.x, e.getYOnScreen() - point.y);
            jWindow.setVisible(true);
        }
    }

    private void onReleased(int ligne, int colonne) {
        synchronized (jeu) {
            supprimerCasesSurlignes();
            if (pointDepart != null) {
                jeu.setCoup(new Coup(pointDepart, new Point(ligne, colonne)));
                pointDepart = null;
            }
            cleanupDrag();
        }
    }

    private void onEntered(int ligne, int colonne) {
        if (!casesSurlignes.contains(new Point(ligne, colonne)))
            vue.setFond(ligne, colonne);
    }

    private void onExited(int ligne, int colonne) {
        if (!casesSurlignes.contains(new Point(ligne, colonne)))
            vue.clearFond(ligne, colonne);
    }

    private void onDragged(MouseEvent e) {
        if (jWindow != null && point != null)
            jWindow.setLocation(e.getXOnScreen() - point.x,
                                   e.getYOnScreen() - point.y);
    }

    private void onGlobalReleased(MouseEvent me) {
        synchronized (jeu) {
            if (pointDepart == null) return;
            JPanel boardPanel = vue.getPanel();
            if (!boardPanel.isShowing()) { cleanupDrag(); pointDepart = null; return; }
            try {
                Point origin = boardPanel.getLocationOnScreen();
                int rx = me.getXOnScreen() - origin.x;
                int ry = me.getYOnScreen() - origin.y;
                if (rx < 0 || ry < 0 || rx >= boardPanel.getWidth() || ry >= boardPanel.getHeight()) {
                    cleanupDrag(); pointDepart = null; return;
                }
                int col = Math.min(7, rx / Math.max(1, boardPanel.getWidth()  / 8));
                int row = Math.min(7, ry / Math.max(1, boardPanel.getHeight() / 8));
                supprimerCasesSurlignes();
                jeu.setCoup(new Coup(new Point(pointDepart.x, pointDepart.y), new Point(row, col))); // (3)
                pointDepart = null;
                cleanupDrag();
            } catch (IllegalComponentStateException ex) {
                cleanupDrag(); pointDepart = null;
            }
        }
    }

    private void supprimerCasesSurlignes() {
        for (Point p : casesSurlignes) {
            vue.clearHighlight(p.x, p.y);
        }
        casesSurlignes.clear();
        if (jPanel != null) {
            if (pointDepart != null) vue.setCaseBorder(pointDepart.x, pointDepart.y, null);
            jPanel = null;
        }
    }

    private void cleanupDrag() {
        if (pointDepart != null) { vue.setCaseBorder(pointDepart.x, pointDepart.y, null); } // (2)
        jPanel = null;
        if (jWindow != null) {
            try { jWindow.setVisible(false); jWindow.dispose(); } catch (Exception ignored) {}
            jWindow = null;
            point = null;
        }
    }

    @Override
    public void update(Observable o, Object arg) {
        if (arg instanceof String) {
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
            SwingUtilities.invokeLater(() -> {
                pointDepart = null;
                casesSurlignes.clear();
                jLabel.setText("Nouvelle partie — Tour des BLANCS");
            });
        } else {
            SwingUtilities.invokeLater(() -> jLabel.setText("Mise à jour reçue"));
        }
    }

    @Override
    public void dispose() {
        if (awtEventListener != null)
            try { Toolkit.getDefaultToolkit().removeAWTEventListener(awtEventListener); }
            catch (Exception ignored) {}
        super.dispose();
    }
}
