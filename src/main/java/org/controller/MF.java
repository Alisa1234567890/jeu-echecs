package org.controller;

import org.model.Coup;
import org.model.Jeu;
import org.model.JeuObserver;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.JButton;
import javax.swing.border.Border;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.FlowLayout;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.awt.event.WindowEvent;

public class MF extends JFrame implements JeuObserver {

    private static final Color WINDOW_BG = new Color(224, 236, 250);
    private static final Color PANEL_BG = new Color(245, 250, 255);
    private static final Color PANEL_ACCENT = new Color(35, 87, 158);
    private static final Color BOARD_FRAME = new Color(61, 98, 151);
    private static final Color ACTIVE_BORDER = new Color(89, 168, 255);
    private static final Color ANNOUNCEMENT_BG = new Color(32, 78, 140);
    private static final Color ANNOUNCEMENT_TEXT = new Color(244, 248, 255);

    private Jeu jeu;
    private Runnable restartAction;
    private Runnable reconfigureAction;

    private final JLabel modeLabel;
    private final JLabel statusLabel;
    private final JLabel announcementLabel;
    private final JLabel whiteScoreLabel;
    private final JLabel blackScoreLabel;
    private final JLabel whiteDetailLabel;
    private final JLabel blackDetailLabel;
    private final JLabel whiteTimeLabel;
    private final JLabel blackTimeLabel;
    private final JPanel whiteCard;
    private final JPanel blackCard;
    private final JPanel boardHolder;
    private final JButton resignButton;
    private final ConfettiGlassPane confettiGlassPane;
    private final Timer announcementHideTimer;
    private final Timer uiRefreshTimer;

    private boolean endDialogShown;
    private int lastWhiteCapturedScore;
    private int lastBlackCapturedScore;

    public MF() {
        super("Jeu d'echecs");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(980, 760));
        setSize(980, 760);
        setLocationRelativeTo(null);

        confettiGlassPane = new ConfettiGlassPane();
        setGlassPane(confettiGlassPane);

        announcementHideTimer = new Timer(2400, e -> hideAnnouncement());
        announcementHideTimer.setRepeats(false);
        uiRefreshTimer = new Timer(200, e -> {
            if (jeu != null) {
                jeu.tickClock();
                refreshInfo();
                if (jeu.partieTerminee() && !endDialogShown) {
                    endDialogShown = true;
                    showEndOfGameDialog();
                }
            }
        });
        uiRefreshTimer.start();

        JPanel root = new JPanel(new BorderLayout(18, 18));
        root.setBackground(WINDOW_BG);
        root.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        setContentPane(root);

        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));

        JLabel titleLabel = new JLabel("Jeu d'echecs");
        titleLabel.setAlignmentX(LEFT_ALIGNMENT);
        titleLabel.setFont(new Font("Serif", Font.BOLD, 30));
        titleLabel.setForeground(new Color(25, 51, 89));
        header.add(titleLabel);
        header.add(Box.createVerticalStrut(6));

        modeLabel = new JLabel("Initialisation...");
        modeLabel.setAlignmentX(LEFT_ALIGNMENT);
        modeLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        modeLabel.setForeground(PANEL_ACCENT);
        header.add(modeLabel);
        header.add(Box.createVerticalStrut(6));

        statusLabel = new JLabel("Preparation de la partie");
        statusLabel.setAlignmentX(LEFT_ALIGNMENT);
        statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 15));
        statusLabel.setForeground(new Color(54, 74, 102));
        header.add(statusLabel);
        header.add(Box.createVerticalStrut(10));

        announcementLabel = new JLabel(" ", SwingConstants.CENTER);
        announcementLabel.setOpaque(true);
        announcementLabel.setVisible(false);
        announcementLabel.setAlignmentX(LEFT_ALIGNMENT);
        announcementLabel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        announcementLabel.setFont(new Font("SansSerif", Font.BOLD, 15));
        announcementLabel.setForeground(ANNOUNCEMENT_TEXT);
        announcementLabel.setBackground(ANNOUNCEMENT_BG);
        announcementLabel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(127, 187, 255), 1, true),
                BorderFactory.createEmptyBorder(8, 14, 8, 14)
        ));
        header.add(announcementLabel);
        header.add(Box.createVerticalStrut(10));

        JPanel actionRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        actionRow.setOpaque(false);
        resignButton = new JButton("Resign");
        resignButton.setFocusPainted(false);
        resignButton.addActionListener(e -> confirmResignation());
        actionRow.add(resignButton);
        header.add(actionRow);

        root.add(header, BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(18, 0));
        center.setOpaque(false);
        root.add(center, BorderLayout.CENTER);

        boardHolder = new JPanel(new BorderLayout());
        boardHolder.setOpaque(true);
        boardHolder.setBackground(PANEL_BG);
        boardHolder.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BOARD_FRAME, 2, true),
                BorderFactory.createEmptyBorder(16, 16, 16, 16)
        ));
        center.add(boardHolder, BorderLayout.CENTER);

        JPanel sidePanel = new JPanel(new GridLayout(3, 1, 0, 14));
        sidePanel.setOpaque(false);
        sidePanel.setPreferredSize(new Dimension(240, 0));
        center.add(sidePanel, BorderLayout.EAST);

        whiteScoreLabel = new JLabel();
        whiteDetailLabel = new JLabel();
        whiteTimeLabel = new JLabel();
        whiteCard = createPlayerCard("White", whiteScoreLabel, whiteDetailLabel, whiteTimeLabel);
        sidePanel.add(whiteCard);

        blackScoreLabel = new JLabel();
        blackDetailLabel = new JLabel();
        blackTimeLabel = new JLabel();
        blackCard = createPlayerCard("Black", blackScoreLabel, blackDetailLabel, blackTimeLabel);
        sidePanel.add(blackCard);

        sidePanel.add(createInfoCard());
    }

    public void setJeu(Jeu jeu) {
        this.jeu = jeu;
        this.endDialogShown = false;
        this.lastWhiteCapturedScore = jeu.getCapturedScore(true);
        this.lastBlackCapturedScore = jeu.getCapturedScore(false);
        refreshInfo();
    }

    public void setVC(VC vc) {
        boardHolder.removeAll();
        boardHolder.add(vc.getPanel(), BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    public void setSessionActions(Runnable restartAction, Runnable reconfigureAction) {
        this.restartAction = restartAction;
        this.reconfigureAction = reconfigureAction;
    }

    @Override
    public void update(Object arg) {
        SwingUtilities.invokeLater(() -> {
            if (jeu == null) {
                return;
            }
            handleUpdate(arg);
            refreshInfo();
            if (jeu.partieTerminee() && !endDialogShown) {
                endDialogShown = true;
                showEndOfGameDialog();
            }
        });
    }

    private JPanel createPlayerCard(String title, JLabel scoreLabel, JLabel detailLabel, JLabel timeLabel) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setOpaque(true);
        card.setBackground(PANEL_BG);
        card.setBorder(createCardBorder(false));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setAlignmentX(LEFT_ALIGNMENT);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        titleLabel.setForeground(new Color(34, 58, 99));

        scoreLabel.setAlignmentX(LEFT_ALIGNMENT);
        scoreLabel.setFont(new Font("SansSerif", Font.BOLD, 24));
        scoreLabel.setForeground(PANEL_ACCENT);

        detailLabel.setAlignmentX(LEFT_ALIGNMENT);
        detailLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));
        detailLabel.setForeground(new Color(73, 93, 122));

        timeLabel.setAlignmentX(LEFT_ALIGNMENT);
        timeLabel.setFont(new Font("Monospaced", Font.BOLD, 20));
        timeLabel.setForeground(new Color(24, 69, 129));

        card.add(titleLabel);
        card.add(Box.createVerticalStrut(10));
        card.add(timeLabel);
        card.add(Box.createVerticalStrut(10));
        card.add(scoreLabel);
        card.add(Box.createVerticalStrut(6));
        card.add(detailLabel);
        return card;
    }

    private JPanel createInfoCard() {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setOpaque(true);
        card.setBackground(new Color(236, 245, 255));
        card.setBorder(createCardBorder(false));

        JLabel title = new JLabel("Highlights");
        title.setAlignmentX(LEFT_ALIGNMENT);
        title.setFont(new Font("SansSerif", Font.BOLD, 16));
        title.setForeground(new Color(34, 58, 99));

        JLabel line1 = infoLabel("Score = captured material points.");
        JLabel line2 = infoLabel("Special moves and captures trigger announcements.");
        JLabel line3 = infoLabel("Game over lets you restart or change mode/level.");

        card.add(title);
        card.add(Box.createVerticalStrut(10));
        card.add(line1);
        card.add(Box.createVerticalStrut(6));
        card.add(line2);
        card.add(Box.createVerticalStrut(6));
        card.add(line3);
        return card;
    }

    private JLabel infoLabel(String text) {
        JLabel label = new JLabel("<html>" + text + "</html>", SwingConstants.LEFT);
        label.setAlignmentX(LEFT_ALIGNMENT);
        label.setFont(new Font("SansSerif", Font.PLAIN, 13));
        label.setForeground(new Color(73, 93, 122));
        return label;
    }

    private Border createCardBorder(boolean active) {
        Color borderColor = active ? ACTIVE_BORDER : new Color(180, 201, 230);
        int thickness = active ? 3 : 1;
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(borderColor, thickness, true),
                BorderFactory.createEmptyBorder(16, 16, 16, 16)
        );
    }

    private void handleUpdate(Object arg) {
        if ("Game reset.".equals(arg)) {
            endDialogShown = false;
            hideAnnouncement();
            lastWhiteCapturedScore = jeu.getCapturedScore(true);
            lastBlackCapturedScore = jeu.getCapturedScore(false);
            return;
        }

        if ("RESIGN".equals(arg)) {
            showAnnouncement(jeu.getStatusMessage());
            confettiGlassPane.burst();
            return;
        }

        if ("TIMEOUT".equals(arg)) {
            showAnnouncement(jeu.getStatusMessage());
            confettiGlassPane.burst();
            return;
        }

        if (!(arg instanceof Coup coup)) {
            lastWhiteCapturedScore = jeu.getCapturedScore(true);
            lastBlackCapturedScore = jeu.getCapturedScore(false);
            return;
        }

        int whiteCaptured = jeu.getCapturedScore(true);
        int blackCaptured = jeu.getCapturedScore(false);
        int whiteDelta = whiteCaptured - lastWhiteCapturedScore;
        int blackDelta = blackCaptured - lastBlackCapturedScore;
        String actor = determineActor(whiteDelta, blackDelta);

        List<String> fragments = new ArrayList<>();
        boolean celebrate = false;

        if (whiteDelta > 0) {
            fragments.add("White +" + whiteDelta + " point" + (whiteDelta > 1 ? "s" : ""));
            celebrate = true;
        }
        if (blackDelta > 0) {
            fragments.add("Black +" + blackDelta + " point" + (blackDelta > 1 ? "s" : ""));
            celebrate = true;
        }

        String special = specialMessage(coup, actor);
        if (special != null) {
            fragments.add(special);
            celebrate = true;
        }

        if (!fragments.isEmpty()) {
            showAnnouncement(String.join("  •  ", fragments));
            if (celebrate) {
                confettiGlassPane.burst();
            }
        }

        lastWhiteCapturedScore = whiteCaptured;
        lastBlackCapturedScore = blackCaptured;
    }

    private String determineActor(int whiteDelta, int blackDelta) {
        if (whiteDelta > 0) {
            return "White";
        }
        if (blackDelta > 0) {
            return "Black";
        }
        return jeu.isWhiteToMove() ? "Black" : "White";
    }

    private String specialMessage(Coup coup, String actor) {
        return switch (coup.getType()) {
            case "PRISE EN PASSANT" -> actor + " played en passant";
            case "ROQUE" -> actor + " castled";
            case "PROMOTION" -> actor + " promoted a pawn";
            case "ECHEC" -> actor + " gives check";
            case "ECHEC ET MAT" -> actor + " wins by checkmate";
            case "PAT" -> "Stalemate";
            default -> null;
        };
    }

    private void refreshInfo() {
        if (jeu == null) {
            return;
        }

        modeLabel.setText(jeu.getModeLabel());
        statusLabel.setText(jeu.getStatusMessage());

        int whiteScore = jeu.getCapturedScore(true);
        int blackScore = jeu.getCapturedScore(false);
        int whiteMaterial = jeu.getMaterialScore(true);
        int blackMaterial = jeu.getMaterialScore(false);

        whiteScoreLabel.setText("Score " + whiteScore);
        blackScoreLabel.setText("Score " + blackScore);
        whiteDetailLabel.setText("Material left: " + whiteMaterial + formatLead(whiteScore - blackScore));
        blackDetailLabel.setText("Material left: " + blackMaterial + formatLead(blackScore - whiteScore));
        whiteTimeLabel.setText(formatTime(jeu.getRemainingTimeMillis(true)));
        blackTimeLabel.setText(formatTime(jeu.getRemainingTimeMillis(false)));

        boolean whiteToMove = !jeu.partieTerminee() && jeu.isWhiteToMove();
        whiteCard.setBorder(createCardBorder(whiteToMove));
        blackCard.setBorder(createCardBorder(!whiteToMove && !jeu.partieTerminee()));
        resignButton.setVisible(jeu.canResign());
        resignButton.setEnabled(jeu.canResign());
        resignButton.setText(jeu.canResign() ? jeu.getResignLabel() : "Game finished");

        repaint();
    }

    private String formatLead(int delta) {
        if (delta > 0) {
            return "  •  Lead +" + delta;
        }
        if (delta < 0) {
            return "  •  Lead " + delta;
        }
        return "  •  Even";
    }

    private void showAnnouncement(String message) {
        announcementLabel.setText(message);
        announcementLabel.setVisible(true);
        announcementHideTimer.restart();
    }

    private void hideAnnouncement() {
        announcementHideTimer.stop();
        announcementLabel.setVisible(false);
        announcementLabel.setText(" ");
    }

    private void showEndOfGameDialog() {
        String title = jeu.isDraw() ? "Game Over - Draw" : "Game Over - Winner";
        String result = jeu.isDraw()
                ? "The game ended in a draw."
                : "Winner: " + jeu.getWinnerLabel();
        String message = "<html><div style='width:300px;'>"
                + "<b>" + result + "</b><br><br>"
                + "White score: " + jeu.getCapturedScore(true) + "<br>"
                + "Black score: " + jeu.getCapturedScore(false) + "<br>"
                + "White material left: " + jeu.getMaterialScore(true) + "<br>"
                + "Black material left: " + jeu.getMaterialScore(false)
                + "</div></html>";

        Object[] options = {"Restart", "Change Mode/Level", "Close"};
        int choice = JOptionPane.showOptionDialog(
                this,
                message,
                title,
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.INFORMATION_MESSAGE,
                null,
                options,
                options[0]
        );

        if (choice == 0 && restartAction != null) {
            restartAction.run();
        } else if (choice == 1 && reconfigureAction != null) {
            reconfigureAction.run();
        } else {
            dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
        }
    }

    private void confirmResignation() {
        if (jeu == null || !jeu.canResign()) {
            return;
        }
        int choice = JOptionPane.showConfirmDialog(
                this,
                "Do you want to resign the game?",
                "Resign",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );
        if (choice == JOptionPane.YES_OPTION) {
            jeu.resignCurrentPlayer();
        }
    }

    private String formatTime(long millis) {
        long totalSeconds = Math.max(0L, millis / 1000L);
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;
        return String.format("%02d:%02d", minutes, seconds);
    }

    private static final class ConfettiGlassPane extends JPanel {
        private final List<Particle> particles = new ArrayList<>();
        private final Timer timer;
        private final Random random = new Random();
        private final Color[] palette = {
                new Color(67, 138, 227),
                new Color(110, 189, 255),
                new Color(29, 83, 164),
                new Color(255, 211, 92),
                new Color(255, 255, 255)
        };

        private ConfettiGlassPane() {
            setOpaque(false);
            setVisible(false);
            timer = new Timer(16, e -> tick());
        }

        private void burst() {
            particles.clear();
            int width = Math.max(1, getWidth());
            int height = Math.max(1, getHeight());
            for (int i = 0; i < 90; i++) {
                double startX = width * 0.2 + random.nextDouble() * width * 0.6;
                double startY = height * 0.12 + random.nextDouble() * height * 0.15;
                double vx = -3.0 + random.nextDouble() * 6.0;
                double vy = -7.5 + random.nextDouble() * 3.5;
                double size = 5 + random.nextDouble() * 8;
                int life = 45 + random.nextInt(30);
                particles.add(new Particle(startX, startY, vx, vy, size, life, palette[random.nextInt(palette.length)]));
            }
            setVisible(true);
            timer.start();
        }

        private void tick() {
            if (particles.isEmpty()) {
                timer.stop();
                setVisible(false);
                return;
            }
            for (int i = particles.size() - 1; i >= 0; i--) {
                Particle particle = particles.get(i);
                particle.x += particle.vx;
                particle.y += particle.vy;
                particle.vy += 0.24;
                particle.rotation += particle.spin;
                particle.life--;
                if (particle.life <= 0) {
                    particles.remove(i);
                }
            }
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setStroke(new BasicStroke(1.2f));
                for (Particle particle : particles) {
                    g2.translate(particle.x, particle.y);
                    g2.rotate(particle.rotation);
                    g2.setColor(particle.color);
                    g2.fill(new Rectangle2D.Double(-particle.size / 2.0, -particle.size / 2.0, particle.size, particle.size * 0.7));
                    g2.setColor(new Color(255, 255, 255, 90));
                    g2.draw(new Rectangle2D.Double(-particle.size / 2.0, -particle.size / 2.0, particle.size, particle.size * 0.7));
                    g2.rotate(-particle.rotation);
                    g2.translate(-particle.x, -particle.y);
                }
            } finally {
                g2.dispose();
            }
        }

        private static final class Particle {
            private double x;
            private double y;
            private final double vx;
            private double vy;
            private final double size;
            private int life;
            private final Color color;
            private double rotation;
            private final double spin;

            private Particle(double x, double y, double vx, double vy, double size, int life, Color color) {
                this.x = x;
                this.y = y;
                this.vx = vx;
                this.vy = vy;
                this.size = size;
                this.life = life;
                this.color = color;
                this.rotation = 0;
                this.spin = (Math.random() - 0.5) * 0.45;
            }
        }
    }

    @Override
    public void dispose() {
        announcementHideTimer.stop();
        uiRefreshTimer.stop();
        super.dispose();
    }
}
