package org.example;

import org.example.algorithm.Message;
import org.example.algorithm.Server;
import org.example.config.ConfigLoader;
import org.example.config.ServerConfig;


import org.example.config.ServerDetails;
import org.yaml.snakeyaml.Yaml;


import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.*;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class Main {

    private static Server server;
    private static JPasswordField secretKeyField; // Changed to JPasswordField
    private static JLabel typeLabel;
    private static JTextField ipField, portField, timeoutField, hostNumberField, apiPortField; // Moved declaration to class level
    private static JButton startButton, stopButton, becomeLeaderButton;


    public static void main(String[] args) {
        ServerConfig config = ConfigLoader.loadConfig("config.yaml");
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("RAFT Server");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(600, 600);
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

            typeLabel = new JLabel("");
            typeLabel.setFont(new Font("Arial", Font.PLAIN, 16));
            typeLabel.setForeground(Color.BLACK);
            typeLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            centerPanel.add(typeLabel);
            centerPanel.add(Box.createRigidArea(new Dimension(0, 10)));


            JPanel buttonPanel = new JPanel();
            buttonPanel.setLayout(new GridBagLayout());


            JLabel ipLabel = new JLabel("Server IP Address:");
            ipLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

            ipField = new JTextField();
            ipField.setMaximumSize(new Dimension(200, 30));
            ipField.setAlignmentX(Component.CENTER_ALIGNMENT);
            ipField.setText(config.getServer().getAddress());

            JLabel portLabel = new JLabel("Port:");
            portLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

            portField = new JTextField();
            portField.setMaximumSize(new Dimension(200, 30));
            portField.setAlignmentX(Component.CENTER_ALIGNMENT);
            portField.setText(String.valueOf(config.getServer().getPort()));

            JLabel timeoutLabel = new JLabel("Timeout:");
            timeoutLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

            timeoutField = new JTextField();
            timeoutField.setMaximumSize(new Dimension(200, 30));
            timeoutField.setAlignmentX(Component.CENTER_ALIGNMENT);
            timeoutField.setText(String.valueOf(config.getServer().getTimeout()));

            JLabel hostNumberLabel = new JLabel("Number of hosts:");
            hostNumberLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

            hostNumberField = new JTextField(10);
            hostNumberField.setMaximumSize(new Dimension(200, 30));
            hostNumberField.setAlignmentX(Component.CENTER_ALIGNMENT);
            hostNumberField.setText(String.valueOf(config.getServer().getHostNumber()));

            JLabel apiPortLabel = new JLabel("Port API:");
            apiPortLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

            apiPortField = new JTextField();
            apiPortField.setMaximumSize(new Dimension(200, 30));
            apiPortField.setAlignmentX(Component.CENTER_ALIGNMENT);
            apiPortField.setText(String.valueOf(config.getServer().getApiPort()));


            JLabel secretKeyLabel = new JLabel("Secret Key:");
            secretKeyLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

            secretKeyField = new JPasswordField();
            secretKeyField.setMaximumSize(new Dimension(200, 30));
            secretKeyField.setAlignmentX(Component.CENTER_ALIGNMENT);
            secretKeyField.setEchoChar('*'); // Set echo character to '*' for password-like input

            startButton = new JButton("Start Server");
            stopButton = new JButton("Stop Server");
            becomeLeaderButton = new JButton("Become a leader!");
            startButton.setEnabled(false);
            stopButton.setEnabled(false);
            becomeLeaderButton.setEnabled(false);

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
            String portText = portField.getText().trim();
            String hostNumberText = hostNumberField.getText().trim();
            String timeoutText = timeoutField.getText().trim();
            String portApiText = apiPortField.getText().trim();
            if (isValidIPv4(ipText) && secretKeyText.length()>0 && portText.length()>0 && hostNumberText.length()>0 && timeoutText.length()>0 && portApiText.length()>0) {
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

            DocumentListener validationListener = new DocumentListener() {
                @Override
                public void insertUpdate(DocumentEvent e) {
                    validateFields();
                }

                @Override
                public void removeUpdate(DocumentEvent e) {
                    validateFields();
                }

                @Override
                public void changedUpdate(DocumentEvent e) {
                    validateFields();
                }

                private void validateFields() {
                    String ip = ipField.getText().trim();
                    String port = portField.getText().trim();
                    String timeout = timeoutField.getText().trim();
                    String hostNumber = hostNumberField.getText().trim();
                    String apiPort = apiPortField.getText().trim();
                    String secretKey = secretKeyField.getText().trim();

                    boolean isValid = isValidIPv4(ip) &&
                            !port.isEmpty() &&
                            !timeout.isEmpty() &&
                            !hostNumber.isEmpty() &&
                            !apiPort.isEmpty() &&
                            !secretKey.isEmpty();

                    startButton.setEnabled(isValid);
                }
            };

            ipField.getDocument().addDocumentListener(validationListener);
            portField.getDocument().addDocumentListener(validationListener);
            timeoutField.getDocument().addDocumentListener(validationListener);
            hostNumberField.getDocument().addDocumentListener(validationListener);
            apiPortField.getDocument().addDocumentListener(validationListener);
            secretKeyField.getDocument().addDocumentListener(validationListener);

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

            Timer timer = new Timer(1000, event -> {
                SwingUtilities.invokeLater(() -> {
                    if(server.isLeader)
                        typeLabel.setText("I am a Leader!");
                    if(server.isCandidate)
                        typeLabel.setText("I am a Candidate!");
                    if(server.isForwarded)
                        typeLabel.setText("I am a Forwarder!");
                });
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

                    Map<String, Object> data = Map.of(
                            "server", Map.of(
                                    "address", ipField.getText(),
                                    "port", Integer.parseInt(portField.getText()),
                                    "timeout", Integer.parseInt(timeoutField.getText()),
                                    "hostNumber", Integer.parseInt(hostNumberField.getText()),
                                    "apiPort", Integer.parseInt(apiPortField.getText())
                            )
                    );

                    Yaml yaml = new Yaml();
                    URL resource = Main.class.getClassLoader().getResource("config.yaml");
                    if (resource != null) {
                        try {
                            File file = Paths.get(resource.toURI()).toFile();
                            try (FileWriter writer = new FileWriter(file)) {
                                yaml.dump(data, writer);  // Zapisuje dane do pliku
                            } catch (IOException ex) {
                                ex.printStackTrace();
                            }
                        } catch (Exception exx) {
                            exx.printStackTrace();
                        }
                    } else {
                        System.out.println("Nie znaleziono pliku resources/config.yaml.");
                    }

                    ipField.setEnabled(false);
                    portField.setEnabled(false);
                    timeoutField.setEnabled(false);
                    hostNumberField.setEnabled(false);
                    apiPortField.setEnabled(false);
                    secretKeyField.setEnabled(false);
                    startButton.setEnabled(false);
                    stopButton.setEnabled(true);
                    becomeLeaderButton.setEnabled(true);
                    timer.start();
                } catch (SocketException ex) {
                    logImportant(doc, errorStyle, "Error starting server: " + ex.getMessage());
                    JOptionPane.showMessageDialog(frame, "Error starting server!", "Error", JOptionPane.ERROR_MESSAGE);
                } catch (UnknownHostException ex) {
                    throw new RuntimeException(ex);
                }
            });

            stopButton.addActionListener(e -> {
                if (server != null) {
                    server.stop();
                    logImportant(doc, successStyle, "Server stopped.");
                    ipField.setEnabled(true);
                    portField.setEnabled(true);
                    timeoutField.setEnabled(true);
                    hostNumberField.setEnabled(true);
                    apiPortField.setEnabled(true);
                    secretKeyField.setEnabled(true);
                    startButton.setEnabled(true);
                    stopButton.setEnabled(false);
                    becomeLeaderButton.setEnabled(false);
                }
            });

            becomeLeaderButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    server.setLeader();
                }
            });






            JPanel fieldsPanel = new JPanel();
            fieldsPanel.setLayout(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.insets = new Insets(5, 5, 5, 5);

            fieldsPanel.add(ipLabel);
            fieldsPanel.add(Box.createRigidArea(new Dimension(0, 0)));
            fieldsPanel.add(ipField);
            fieldsPanel.add(Box.createRigidArea(new Dimension(0, 10)));

            fieldsPanel.add(portLabel);
            fieldsPanel.add(Box.createRigidArea(new Dimension(0, 10)));
            fieldsPanel.add(portField);
            fieldsPanel.add(Box.createRigidArea(new Dimension(0, 10)));

            fieldsPanel.add(timeoutLabel);
            fieldsPanel.add(Box.createRigidArea(new Dimension(0, 10)));
            fieldsPanel.add(timeoutField);
            fieldsPanel.add(Box.createRigidArea(new Dimension(0, 10)));

            fieldsPanel.add(hostNumberLabel);
            fieldsPanel.add(Box.createRigidArea(new Dimension(0, 10)));
            fieldsPanel.add(hostNumberField);
            fieldsPanel.add(Box.createRigidArea(new Dimension(0, 10)));

            fieldsPanel.add(apiPortLabel);
            fieldsPanel.add(Box.createRigidArea(new Dimension(0, 10)));
            fieldsPanel.add(apiPortField);
            fieldsPanel.add(Box.createRigidArea(new Dimension(0, 10)));

            fieldsPanel.add(secretKeyLabel);
            fieldsPanel.add(Box.createRigidArea(new Dimension(0, 10)));
            fieldsPanel.add(secretKeyField);
            fieldsPanel.add(Box.createRigidArea(new Dimension(0, 20)));

            gbc.gridx = 0; gbc.gridy = 0;
            fieldsPanel.add(ipLabel, gbc);
            gbc.gridx = 1;
            fieldsPanel.add(ipField, gbc);

            gbc.gridx = 0; gbc.gridy = 1;
            fieldsPanel.add(portLabel, gbc);
            gbc.gridx = 1;
            fieldsPanel.add(portField, gbc);

            gbc.gridx = 0; gbc.gridy = 2;
            fieldsPanel.add(timeoutLabel, gbc);
            gbc.gridx = 1;
            fieldsPanel.add(timeoutField, gbc);

            gbc.gridx = 0; gbc.gridy = 3;
            fieldsPanel.add(hostNumberLabel, gbc);
            gbc.gridx = 1;
            fieldsPanel.add(hostNumberField, gbc);

            gbc.gridx = 0; gbc.gridy = 4;
            fieldsPanel.add(apiPortLabel, gbc);
            gbc.gridx = 1;
            fieldsPanel.add(apiPortField, gbc);

            gbc.gridx = 0; gbc.gridy = 5;
            fieldsPanel.add(secretKeyLabel, gbc);
            gbc.gridx = 1;
            fieldsPanel.add(secretKeyField, gbc);

            centerPanel.add(fieldsPanel);

            gbc.gridx = 0;
            gbc.gridy = 0;
            buttonPanel.add(startButton, gbc);

            gbc.gridx = 1;
            gbc.gridy = 0;
            buttonPanel.add(stopButton, gbc);

            gbc.gridx = 0;
            gbc.gridy = 1;
            gbc.gridwidth = 2;
            buttonPanel.add(becomeLeaderButton, gbc);

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
