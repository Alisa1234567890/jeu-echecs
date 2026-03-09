package org.controller;

import org.model.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Observable;
import java.util.Observer;

public class VC extends JFrame implements Observer {

    private final Jeu jeu;
    private Point depart;
    private JPanel panel;

    public VC(Jeu jeu) {
        this.jeu = jeu;
        jeu.addObserver(this);

        setTitle("Jeu d'échecs");
        setSize(600, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        panel = new JPanel(new GridLayout(8, 8));
        add(panel);

        redraw();
    }

    // expose the internal board panel so it can be embedded elsewhere
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

                // Display piece
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
                        // fallback: use piece initial if image not found
                        String initial = piece.getClass().getSimpleName();
                        initial = initial.isEmpty() ? "?" : initial.substring(0, 1);
                        label = new JLabel(initial, SwingConstants.CENTER);
                    }
                    casePanel.add(label);
                }

                casePanel.addMouseListener(new MouseAdapter() {

                    @Override
                    public void mouseEntered(MouseEvent e) {
                        casePanel.setBackground(Color.RED);
                    }

                    @Override
                    public void mouseExited(MouseEvent e) {
                        casePanel.setBackground(couleurOriginale);
                    }

                    @Override
                    public void mouseClicked(MouseEvent e) {
                        synchronized (jeu) {
                            if (depart == null) {
                                depart = new Point(ligne, colonne);
                            } else {
                                jeu.nextC = new Coup(
                                        depart,
                                        new Point(ligne, colonne)
                                );
                                depart = null;
                                jeu.notify();
                            }
                        }
                    }
                });

                panel.add(casePanel);
            }
        }

        panel.revalidate();
        panel.repaint();
    }

    @Override
    public void update(Observable o, Object arg) {
        redraw();
    }
}
