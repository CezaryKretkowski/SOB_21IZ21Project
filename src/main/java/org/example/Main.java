package org.example;

import org.example.algorithm.Server;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class Main {

    private static Server server;
    private static final BlockingQueue<String> logQueue = new LinkedBlockingQueue<>();
    private static boolean isRunning = true;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("RAFT Server");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(600, 400);
            frame.setLayout(new BorderLayout());

            JPanel titlePanel = new JPanel();
            JLabel titleLabel = new JLabel("Consensus in Secure Systems");
            titleLabel.setFont(new Font("Arial", Font.BOLD, 20));
            titleLabel.setForeground(Color.BLUE);
            titlePanel.add(titleLabel);
            titlePanel.setAlignmentX(Component.CENTER_ALIGNMENT);

            JPanel centerPanel = new JPanel();
            centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
            centerPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

            JPanel buttonPanel = new JPanel();
            buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER));

            JLabel ipLabel = new JLabel("Server IP Address:");
            ipLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

            JTextField ipField = new JTextField();
            ipField.setMaximumSize(new Dimension(200, 30));
            ipField.setAlignmentX(Component.CENTER_ALIGNMENT);

            JLabel secretKeyLabel = new JLabel("Secret Key:");
            secretKeyLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

            JTextField secretKeyField = new JTextField();
            secretKeyField.setMaximumSize(new Dimension(200, 30));
            secretKeyField.setAlignmentX(Component.CENTER_ALIGNMENT);

            JButton startButton = new JButton("Start Server");
            JButton stopButton = new JButton("Stop Server");
            startButton.setEnabled(false);
            stopButton.setEnabled(false);

            JLabel logsLabel = new JLabel("Server Logs:");
            logsLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

            JTextPane logPane = new JTextPane();
            logPane.setEditable(false);
            JScrollPane logScrollPane = new JScrollPane(logPane);
            logScrollPane.setPreferredSize(new Dimension(500, 200));

            StyledDocument doc = logPane.getStyledDocument();
            Style regularStyle = logPane.addStyle("Regular", null);
            StyleConstants.setForeground(regularStyle, Color.BLACK);

            // Wątek do obsługi logów
            Thread logUpdater = new Thread(() -> {
                while (isRunning) {
                    try {
                        String log = logQueue.take(); // Pobranie logu z kolejki
                        SwingUtilities.invokeLater(() -> {
                            try {
                                doc.insertString(doc.getLength(), log + "\n", regularStyle);
                                logPane.setCaretPosition(doc.getLength());
                            } catch (BadLocationException ex) {
                                ex.printStackTrace();
                            }
                        });
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            });
            logUpdater.start();

            ipField.addCaretListener(e -> {
                String ipText = ipField.getText().trim();
                if (isValidIPv4(ipText)) {
                    startButton.setEnabled(true);
                } else {
                    startButton.setEnabled(false);
                }
            });

            startButton.addActionListener(e -> {
                String ip = ipField.getText().trim();
                String secretKey = secretKeyField.getText().trim();
                try {
                    InetAddress address = InetAddress.getByName(ip);
                    server = new Server(logQueue::offer); // Przekazanie funkcji logującej
                    server.start(address);
                    logQueue.offer("Server started at address: " + ip + " with Secret Key: " + secretKey);
                    ipField.setEnabled(false);
                    secretKeyField.setEnabled(false);
                    startButton.setEnabled(false);
                    stopButton.setEnabled(true);
                } catch (UnknownHostException ex) {
                    JOptionPane.showMessageDialog(frame, "Invalid IP address!", "Error", JOptionPane.ERROR_MESSAGE);
                    ipField.setText("");
                } catch (SocketException ex) {
                    logQueue.offer("Error starting server: " + ex.getMessage());
                    JOptionPane.showMessageDialog(frame, "Error starting server!", "Error", JOptionPane.ERROR_MESSAGE);
                } catch (IOException ex) {
                    logQueue.offer("Unexpected error: " + ex.getMessage());
                }
            });


            stopButton.addActionListener(e -> {
                if (server != null) {
                    server.stop();
                    logQueue.offer("Server stopped.");
                    ipField.setEnabled(true);
                    secretKeyField.setEnabled(true);
                    startButton.setEnabled(true);
                    stopButton.setEnabled(false);
                }
            });

            centerPanel.add(ipLabel);
            centerPanel.add(Box.createRigidArea(new Dimension(0, 10)));
            centerPanel.add(ipField);
            centerPanel.add(Box.createRigidArea(new Dimension(0, 20)));

            centerPanel.add(secretKeyLabel);
            centerPanel.add(Box.createRigidArea(new Dimension(0, 10)));
            centerPanel.add(secretKeyField);
            centerPanel.add(Box.createRigidArea(new Dimension(0, 20)));

            buttonPanel.add(startButton);
            buttonPanel.add(stopButton);
            centerPanel.add(buttonPanel);
            centerPanel.add(Box.createRigidArea(new Dimension(0, 20)));
            centerPanel.add(logsLabel);
            centerPanel.add(Box.createRigidArea(new Dimension(0, 10)));
            centerPanel.add(logScrollPane);

            frame.add(titlePanel, BorderLayout.NORTH);
            frame.add(centerPanel, BorderLayout.CENTER);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (server != null) {
                logQueue.offer("Ctrl+C intercepted! Stopping server...");
                server.stop();
            }
            isRunning = false;
        }));
    }

    public static boolean isValidIPv4(String ip) {
        String ipv4Pattern = "^([0-9]{1,3}\\.){3}[0-9]{1,3}$";
        if (!ip.matches(ipv4Pattern)) return false;

        String[] parts = ip.split("\\.");
        for (String part : parts) {
            int num = Integer.parseInt(part);
            if (num < 0 || num > 255 || (part.length() > 1 && part.startsWith("0"))) {
                return false;
            }
        }
        return true;
    }
}
