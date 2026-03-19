package org.controller;

import org.model.Jeu;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.util.Observable;
import java.util.Observer;

public class MF extends JFrame implements Observer {

    private Jeu jeu;
    private final ChessController controller;
    private final JLabel statusLabel;
    private final JPanel boardHolder;

    public MF(ChessController controller) {
        super("Chess");
        this.controller = controller;
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 900);
        setLayout(new BorderLayout(8, 8));
        setJMenuBar(createMenuBar());

        statusLabel = new JLabel("Ready", SwingConstants.CENTER);
        statusLabel.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        add(statusLabel, BorderLayout.NORTH);

        boardHolder = new JPanel(new BorderLayout());
        add(boardHolder, BorderLayout.CENTER);
    }

    public void setJeu(Jeu j) {
        this.jeu = j;
        refreshUi();
    }

    public void setVC(VC vc) {
        boardHolder.removeAll();
        boardHolder.add(vc.getPanel(), BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    @Override
    public void update(Observable o, Object arg) {
        SwingUtilities.invokeLater(this::refreshUi);
    }

    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");

        JMenuItem pgnEditorItem = new JMenuItem("PGN Editor");
        pgnEditorItem.addActionListener(e -> {
            PgnEditorDialog dialog = new PgnEditorDialog(this, controller);
            dialog.setVisible(true);
        });

        JMenuItem resetItem = new JMenuItem("Reset Game");
        resetItem.addActionListener(e -> controller.resetGame());

        fileMenu.add(pgnEditorItem);
        fileMenu.add(resetItem);
        menuBar.add(fileMenu);
        return menuBar;
    }

    private void refreshUi() {
        if (jeu == null) {
            return;
        }

        String modeText = controller.getMode() == Jeu.GameMode.HUMAN_VS_AI
                ? "Mode: AI (" + controller.getDifficulty().name() + ")"
                : "Mode: Friend";
        if (controller.isNetworkGame()) {
            modeText = "Mode: Network (" + (controller.isLocalWhite() ? "White" : "Black") + ")";
            modeText = modeText + " | " + controller.getConnectionStatus();
        }
        statusLabel.setText(modeText + " | " + controller.getStatusMessage());
        repaint();
    }
}
