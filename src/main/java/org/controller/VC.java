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
                    ImageIcon icon = new ImageIcon(
                            getClass().getResource(
                                    "/pieces/" + piece.getImageName()
                            )
                    );
                    JLabel label = new JLabel(icon);
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
