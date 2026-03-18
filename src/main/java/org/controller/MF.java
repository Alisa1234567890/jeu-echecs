package org.controller;

import org.model.Jeu;
import org.model.JeuObserver;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;

public class MF extends JFrame implements JeuObserver {

    public Jeu jeu;
    private final JLabel statusLabel;
    private final JPanel boardHolder;

    public MF() {
        super("Echiquier");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600, 600);
        setLayout(new BorderLayout());
        statusLabel = new JLabel("Initialisation...", SwingConstants.CENTER);
        add(statusLabel, BorderLayout.NORTH);
        boardHolder = new JPanel(new BorderLayout());
        add(boardHolder, BorderLayout.CENTER);
    }

    public void setJeu(Jeu j) {
        this.jeu = j;
        statusLabel.setText(j.getModeLabel() + " | " + j.getStatusMessage());
    }

    public void setVC(VC vc) {
        boardHolder.removeAll();
        boardHolder.add(vc.getPanel(), BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    @Override
    public void update(Object arg) {
        SwingUtilities.invokeLater(() -> {
            String msg = jeu != null ? jeu.getModeLabel() + " | " + jeu.getStatusMessage() : "Mise a jour";
            statusLabel.setText(msg);
            repaint();
        });
    }
}
