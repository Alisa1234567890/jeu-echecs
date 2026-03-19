package org.controller;

import org.model.Coup;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.util.ArrayList;
import java.util.List;

public class PgnEditorDialog extends JDialog {

    private final ChessController controller;
    private final JTextArea textArea = new JTextArea();

    public PgnEditorDialog(Frame owner, ChessController controller) {
        super(owner, "PGN Editor", false);
        this.controller = controller;

        setLayout(new BorderLayout(8, 8));
        textArea.setText(controller.exportPgn());
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);

        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(640, 420));
        add(scrollPane, BorderLayout.CENTER);

        JPanel buttons = new JPanel();
        JButton refreshButton = new JButton("Refresh Export");
        JButton importButton = new JButton("Import");
        JButton closeButton = new JButton("Close");

        refreshButton.addActionListener(e -> textArea.setText(controller.exportPgn()));
        importButton.addActionListener(e -> importPgn());
        closeButton.addActionListener(e -> dispose());

        buttons.add(refreshButton);
        buttons.add(importButton);
        buttons.add(closeButton);
        buttons.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        add(buttons, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(owner);
    }

    private void importPgn() {
        try {
            controller.importPgn(parseMoves(textArea.getText()));
            JOptionPane.showMessageDialog(this, "PGN imported.");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Import Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private List<String> parseMoves(String text) {
        String cleaned = text.replaceAll("(?m)^\\[.*?]\\s*$", " ");
        cleaned = cleaned.replaceAll("\\{[^}]*}", " ");
        cleaned = cleaned.replaceAll(";.*", " ");
        cleaned = cleaned.replaceAll("\\d+\\.(\\.\\.)?", " ");
        cleaned = cleaned.replaceAll("1-0|0-1|1/2-1/2|\\*", " ");

        List<String> moves = new ArrayList<>();
        for (String token : cleaned.split("\\s+")) {
            String move = token.trim();
            if (move.isEmpty()) {
                continue;
            }
            moves.add(move);
        }
        return moves;
    }
}
