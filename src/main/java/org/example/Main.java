package org.example;

import org.example.algorithm.Message;
import org.example.algorithm.Server;
import org.example.config.ConfigLoader;
import org.example.config.ServerConfig;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class Main {

    private static Server server;
    private static JPasswordField secretKeyField; // Changed to JPasswordField
    private static JTextField ipField; // Moved declaration to class level
    private static JButton startButton; // Moved declaration to class level
    private static JButton stopButton; // Moved declaration to class level

    public static void main(String[] args) {
        ServerConfig config = ConfigLoader.loadConfig("config.yaml");
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

            ipField = new JTextField();
            ipField.setMaximumSize(new Dimension(200, 30));
            ipField.setAlignmentX(Component.CENTER_ALIGNMENT);
            ipField.setText(config.getServer().getAddress());


            JLabel secretKeyLabel = new JLabel("Secret Key:");
            secretKeyLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

            secretKeyField = new JPasswordField();
            secretKeyField.setMaximumSize(new Dimension(200, 30));
            secretKeyField.setAlignmentX(Component.CENTER_ALIGNMENT);
            secretKeyField.setEchoChar('*'); // Set echo character to '*' for password-like input

            startButton = new JButton("Start Server");
            stopButton = new JButton("Stop Server");
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
            Style successStyle = logPane.addStyle("Success", null);
            Style errorStyle = logPane.addStyle("Error", null);
            Style boldStyle = logPane.addStyle("Bold", null);
            StyleConstants.setBold(boldStyle, true);
            StyleConstants.setForeground(successStyle, new Color(0, 128, 0));
            StyleConstants.setForeground(errorStyle, new Color(255, 0, 0));

            String ipText = ipField.getText().trim();
            String secretKeyText = secretKeyField.getText().trim();
            if (isValidIPv4(ipText) && secretKeyText.length()>0) {
                startButton.setEnabled(true);
            } else {
                startButton.setEnabled(false);
            }

            PrintStream logStream = new PrintStream(new OutputStream() {
                @Override
                public void write(int b) {
                    SwingUtilities.invokeLater(() -> {
                        try {
                            doc.insertString(doc.getLength(), String.valueOf((char) b), regularStyle);
                            logPane.setCaretPosition(doc.getLength());
                        } catch (BadLocationException ex) {
                            ex.printStackTrace();
                        }
                    });
                }
            });
            System.setOut(logStream);
            System.setErr(logStream);

            ipField.addCaretListener(e -> {
                String ip = ipField.getText().trim();
                String secretKey = secretKeyField.getText().trim();
                if (isValidIPv4(ip) && secretKey.length()>0) {
                    startButton.setEnabled(true);
                } else {
                    startButton.setEnabled(false);
                }
            });

            secretKeyField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
                @Override
                public void insertUpdate(javax.swing.event.DocumentEvent e) {
                    checkSecretKey();
                }

                @Override
                public void removeUpdate(javax.swing.event.DocumentEvent e) {
                    checkSecretKey();
                }

                @Override
                public void changedUpdate(javax.swing.event.DocumentEvent e) {
                    checkSecretKey();
                }
            });

            startButton.addActionListener(e -> {
                String ip = ipField.getText().trim();
                String secretKey = new String(secretKeyField.getPassword()).trim(); // Get secret key from JPasswordField
                try {
                    InetAddress address = InetAddress.getByName(ip);
                    server = new Server();
                    server.start(address);
                    Message.SECRET_KEY = secretKey;
                    logImportant(doc, successStyle, "Server started at address: " + ip);
                    ipField.setEnabled(false);
                    secretKeyField.setEnabled(false);
                    startButton.setEnabled(false);
                    stopButton.setEnabled(true);
                } catch (UnknownHostException ex) {
                    JOptionPane.showMessageDialog(frame, "Invalid IP address!", "Error", JOptionPane.ERROR_MESSAGE);
                    ipField.setText("");
                } catch (SocketException ex) {
                    logImportant(doc, errorStyle, "Error starting server: " + ex.getMessage());
                    JOptionPane.showMessageDialog(frame, "Error starting server!", "Error", JOptionPane.ERROR_MESSAGE);
                }
            });

            stopButton.addActionListener(e -> {
                if (server != null) {
                    server.stop();
                    logImportant(doc, successStyle, "Server stopped.");
                    ipField.setEnabled(true);
                    secretKeyField.setEnabled(true);
                    startButton.setEnabled(true);
                    stopButton.setEnabled(false);
                }
            });

            centerPanel.add(ipLabel);
            centerPanel.add(Box.createRigidArea(new Dimension(0, 10)));
            centerPanel.add(ipField);
            centerPanel.add(Box.createRigidArea(new Dimension(0, 10)));
            centerPanel.add(secretKeyLabel); // Add Secret Key label
            centerPanel.add(secretKeyField); // Add Secret Key input field
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
                logImportant(null, null, "Ctrl+C intercepted! Stopping server...");
                server.stop();
            }
        }));
    }

    private static void checkSecretKey() {
        String secretKey = new String(secretKeyField.getPassword()).trim(); // Get the text from password field
        startButton.setEnabled(isValidIPv4(ipField.getText().trim()) && !secretKey.isEmpty());
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

    private static void logImportant(StyledDocument doc, Style style, String message) {
        if (doc != null && style != null) {
            SwingUtilities.invokeLater(() -> {
                try {
                    doc.insertString(doc.getLength(), message + "\n", style);
                } catch (BadLocationException ex) {
                    ex.printStackTrace();
                }
            });
        } else {
            System.out.println(message);
        }
    }
}
