package org.editor;

import org.util.ImageGenerator;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Editeur PNG simplifie : image uniquement, sans metadonnees.
 */
public class PngChessEditor extends JFrame {

    private BufferedImage currentImage;
    private File currentFile;

    private JLabel imageLabel;
    private JLabel infoLabel;

    public PngChessEditor() {
        initializeUI();
        setTitle("Editeur PNG d'Echecs");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(900, 650);
        setLocationRelativeTo(null);
        loadLatestOrCreateEmptyBoard();
    }

    private void initializeUI() {
        setLayout(new BorderLayout());

        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("Fichier");
        JMenuItem openItem = new JMenuItem("Ouvrir PNG...");
        JMenuItem saveItem = new JMenuItem("Enregistrer");
        JMenuItem saveAsItem = new JMenuItem("Enregistrer sous...");
        JMenuItem newImageItem = new JMenuItem("Nouvelle image vide");

        openItem.addActionListener(_ -> openPngFile());
        saveItem.addActionListener(_ -> saveCurrentFile());
        saveAsItem.addActionListener(_ -> saveAsNewFile());
        newImageItem.addActionListener(_ -> createEmptyBoardImage());

        fileMenu.add(openItem);
        fileMenu.addSeparator();
        fileMenu.add(saveItem);
        fileMenu.add(saveAsItem);
        fileMenu.addSeparator();
        fileMenu.add(newImageItem);
        menuBar.add(fileMenu);
        setJMenuBar(menuBar);

        JPanel imagePanel = new JPanel(new BorderLayout());
        imagePanel.setBorder(new TitledBorder("Apercu du PNG"));
        imageLabel = new JLabel("Chargement de l'image...", SwingConstants.CENTER);
        imageLabel.setPreferredSize(new Dimension(620, 620));
        imagePanel.add(new JScrollPane(imageLabel), BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        infoLabel = new JLabel("L'editeur enregistre maintenant uniquement l'image, sans nom de joueur ni metadonnees.");
        JButton emptyButton = new JButton("Remplacer par echiquier vide");
        emptyButton.addActionListener(_ -> replaceWithEmptyBoard());
        bottomPanel.add(infoLabel);
        bottomPanel.add(emptyButton);

        add(imagePanel, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    private void openPngFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new FileNameExtensionFilter("PNG Files", "png"));
        fileChooser.setCurrentDirectory(new File(System.getProperty("user.dir")));

        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            loadPngFile(fileChooser.getSelectedFile());
        }
    }

    private void loadPngFile(File file) {
        try {
            BufferedImage image = ImageIO.read(file);
            if (image == null) {
                JOptionPane.showMessageDialog(this, "Impossible de lire ce PNG.", "Erreur", JOptionPane.ERROR_MESSAGE);
                return;
            }
            currentImage = image;
            currentFile = file;
            refreshPreview();
            setTitle("Editeur PNG d'Echecs - " + file.getName());
            infoLabel.setText("Image chargee: " + file.getName());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Erreur de chargement: " + e.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadLatestOrCreateEmptyBoard() {
        File latestPng = new File(System.getProperty("user.dir"), "partie_echecs.png");
        if (latestPng.isFile()) {
            loadPngFile(latestPng);
            return;
        }
        createEmptyBoardImage();
        infoLabel.setText("Aucun partie_echecs.png trouve. Un echiquier vide a ete charge.");
    }

    private void saveCurrentFile() {
        if (currentImage == null) {
            JOptionPane.showMessageDialog(this, "Aucune image a enregistrer.", "Information", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        if (currentFile == null) {
            saveAsNewFile();
            return;
        }
        saveToFile(currentFile);
    }

    private void saveAsNewFile() {
        if (currentImage == null) {
            JOptionPane.showMessageDialog(this, "Aucune image a enregistrer.", "Information", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new FileNameExtensionFilter("PNG Files", "png"));
        fileChooser.setCurrentDirectory(new File(System.getProperty("user.dir")));

        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            if (!selectedFile.getName().toLowerCase().endsWith(".png")) {
                selectedFile = new File(selectedFile.getAbsolutePath() + ".png");
            }
            saveToFile(selectedFile);
        }
    }

    private void saveToFile(File file) {
        try {
            ImageGenerator.saveAsPng(currentImage, file.getAbsolutePath());
            currentFile = file;
            setTitle("Editeur PNG d'Echecs - " + file.getName());
            infoLabel.setText("Image enregistree sans metadonnees: " + file.getName());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Erreur lors de la sauvegarde: " + e.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void createEmptyBoardImage() {
        currentImage = ImageGenerator.renderBoard(null);
        currentFile = null;
        refreshPreview();
        setTitle("Editeur PNG d'Echecs - Nouvelle image vide");
        infoLabel.setText("Nouvel echiquier vide cree. Enregistrez-le en PNG si besoin.");
    }

    private void replaceWithEmptyBoard() {
        if (currentImage == null) {
            createEmptyBoardImage();
            return;
        }
        int choice = JOptionPane.showConfirmDialog(
                this,
                "Remplacer l'image actuelle par un echiquier vide ?",
                "Confirmation",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );
        if (choice == JOptionPane.YES_OPTION) {
            currentImage = ImageGenerator.renderBoard(null);
            refreshPreview();
            infoLabel.setText("Apercu remplace par un echiquier vide. Cliquez sur Enregistrer pour mettre a jour le PNG.");
        }
    }

    private void refreshPreview() {
        if (currentImage == null) {
            imageLabel.setIcon(null);
            imageLabel.setText("Aucune image disponible");
            return;
        }
        imageLabel.setIcon(new ImageIcon(currentImage));
        imageLabel.setText("");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
            }
            new PngChessEditor().setVisible(true);
        });
    }
}
