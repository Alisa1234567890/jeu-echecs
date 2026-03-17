package org.controller;

import org.model.Jeu;

import javax.swing.*;
import java.awt.*;
import java.util.Observable;
import java.util.Observer;

public class MF extends JFrame implements Observer {

    public Jeu jeu;
    private JLabel statusLabel;
    private JPanel boardHolder;

    public MF() {
        super("MF Window");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600, 600);
        setLayout(new BorderLayout());
        statusLabel = new JLabel("MF initialized", SwingConstants.CENTER);
        add(statusLabel, BorderLayout.NORTH);
        boardHolder = new JPanel(new BorderLayout());
        add(boardHolder, BorderLayout.CENTER);
    }

    public void setJeu(Jeu j) {
        this.jeu = j;
    }

    public void setVC(VC vc) {
        boardHolder.removeAll();
        boardHolder.add(vc.getPanel(), BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    @Override
    public void update(Observable o, Object arg) {
        SwingUtilities.invokeLater(() -> {
            String msg = "Update received";
            if (arg != null) {
                String argStr = arg.toString();
                if (argStr.contains("ÉCHEC ET MAT")) {
                    msg = argStr ;
                } else if (argStr.contains("PAT")) {
                    msg = argStr ;
                } else if (argStr.contains("ÉCHEC")) {
                    msg = "ÉCHEC ! ";
                } else {
                    msg = argStr;
                }
            }
            statusLabel.setText(msg);
            repaint();
        });
    }
}
