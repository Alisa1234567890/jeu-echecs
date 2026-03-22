package org.tools;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.io.IOException;

/**
 * Petite fenetre de demonstration pour prouver le fonctionnement du connecteur reseau.
 */
public class NetworkConnectorDemoFrame extends JFrame {

    private final NetworkConnector connector = new NetworkConnector();

    private final JTextField hostField = new JTextField("localhost", 14);
    private final JTextField portField = new JTextField("5000", 6);
    private final JTextField lineField = new JTextField(28);
    private final JTextArea logArea = new JTextArea(14, 56);

    public NetworkConnectorDemoFrame() {
        super("Demo connecteur reseau");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(760, 430);
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        setContentPane(root);

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        top.add(new JLabel("Host:"));
        top.add(hostField);
        top.add(new JLabel("Port:"));
        top.add(portField);

        JButton connectBtn = new JButton("Connecter");
        JButton disconnectBtn = new JButton("Deconnecter");
        top.add(connectBtn);
        top.add(disconnectBtn);

        JPanel center = new JPanel(new BorderLayout());
        center.setBorder(BorderFactory.createTitledBorder("Journal (envoye/recu)"));
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        center.add(new JScrollPane(logArea), BorderLayout.CENTER);

        JPanel bottom = new JPanel();
        bottom.setLayout(new BoxLayout(bottom, BoxLayout.Y_AXIS));
        JPanel sendRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        JButton sendBtn = new JButton("Envoyer");
        sendRow.add(new JLabel("Ligne:"));
        sendRow.add(lineField);
        sendRow.add(sendBtn);
        bottom.add(sendRow);

        JButton hintBtn = new JButton("Afficher test local (PowerShell)");
        hintBtn.addActionListener(_e -> showLocalServerHint());
        JPanel hintRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 6));
        hintRow.add(hintBtn);
        bottom.add(hintRow);

        root.add(top, BorderLayout.NORTH);
        root.add(center, BorderLayout.CENTER);
        root.add(bottom, BorderLayout.SOUTH);

        connectBtn.addActionListener(_e -> connect());
        disconnectBtn.addActionListener(_e -> disconnect());
        sendBtn.addActionListener(_e -> sendLine());
    }

    private void connect() {
        String host = hostField.getText().trim();
        int port;
        try {
            port = Integer.parseInt(portField.getText().trim());
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Port invalide.", "Erreur", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            connector.connect(host, port, line -> SwingUtilities.invokeLater(() -> appendLog("RECU  << " + line)));
            appendLog("Connecte a " + host + ":" + port);
        } catch (IOException e) {
            appendLog("Erreur connexion: " + e.getMessage());
        }
    }

    private void disconnect() {
        connector.close();
        appendLog("Deconnecte.");
    }

    private void sendLine() {
        String text = lineField.getText();
        if (text == null || text.isBlank()) {
            return;
        }
        connector.sendLine(text);
        appendLog("ENVOYE >> " + text);
        lineField.setText("");
    }

    private void appendLog(String line) {
        logArea.append(line + System.lineSeparator());
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    private void showLocalServerHint() {
        String cmd = "$listener = [System.Net.Sockets.TcpListener]::new([Net.IPAddress]::Loopback,5000);\n"
                + "$listener.Start(); Write-Host 'Echo server sur 5000';\n"
                + "$client = $listener.AcceptTcpClient();\n"
                + "$stream = $client.GetStream();\n"
                + "$reader = New-Object System.IO.StreamReader($stream);\n"
                + "$writer = New-Object System.IO.StreamWriter($stream); $writer.AutoFlush = $true;\n"
                + "while(($line = $reader.ReadLine()) -ne $null){ $writer.WriteLine('echo:' + $line) }";
        JOptionPane.showMessageDialog(this, cmd, "Commande serveur local (copier/coller)", JOptionPane.INFORMATION_MESSAGE);
    }
}

