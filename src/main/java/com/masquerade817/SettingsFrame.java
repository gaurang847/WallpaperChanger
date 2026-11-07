package com.masquerade817;

import com.masquerade817.AppConfig.ConfigData;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.CompletableFuture;

public class SettingsFrame extends JFrame implements WallpaperSyncService.SyncListener {

    private final AppConfig config;
    private final WallpaperSyncService syncService;
    private final TrayManager trayManager;

    // UI Fields
    private JLabel authStatusLabel;
    private JButton authButton;
    private JButton signOutButton;
    private JLabel credHintLabel;

    private JTextField driveFolderField;
    private JButton testFolderButton;
    private JLabel testFolderResultLabel;

    private JTextField customCredentialsField;
    private JButton browseCredentialsButton;

    private JTextField localFolderField;
    private JButton browseFolderButton;
    private JButton openFolderButton;

    private JComboBox<String> frequencyPresetCombo;
    private JSpinner customFrequencySpinner;

    private JCheckBox deleteUnsetCheckbox;
    private JCheckBox startOnStartupCheckbox;
    private JComboBox<String> styleCombo;

    private JLabel statusLabel;
    private JLabel nextSyncLabel;
    private JButton changeNowButton;
    private JButton saveButton;
    private JButton closeToTrayButton;
    private JButton exitAppButton;

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("hh:mm:ss a");

    public SettingsFrame(AppConfig config, WallpaperSyncService syncService, TrayManager trayManager) {
        this.config = config;
        this.syncService = syncService;
        this.trayManager = trayManager;

        initUI();
        loadConfigToUI();
        updateAuthStatus();

        syncService.addListener(this);
    }

    private void initUI() {
        setTitle("Wallpaper Changer - Settings");
        setIconImage(AppIconUtil.getAppIcon(32));
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setResizable(false);

        // Menu bar with File -> Exit, Save, Sync shortcuts
        setJMenuBar(createMenuBar());

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                hideToTray();
            }
        });

        // Register window keyboard shortcuts: Ctrl+Q to exit, Esc to hide to tray
        var rootPane = getRootPane();
        rootPane.registerKeyboardAction(
                e -> confirmAndExit(),
                KeyStroke.getKeyStroke(KeyEvent.VK_Q, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()),
                JComponent.WHEN_IN_FOCUSED_WINDOW
        );
        rootPane.registerKeyboardAction(
                e -> hideToTray(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW
        );

        var mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(new EmptyBorder(12, 16, 12, 16));

        // 1. Header Banner
        mainPanel.add(createHeaderPanel());
        mainPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        // 2. Google Drive Section
        mainPanel.add(createDrivePanel());
        mainPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        // 3. Local Folder Section
        mainPanel.add(createLocalFolderPanel());
        mainPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        // 4. Schedule & Behavior Section
        mainPanel.add(createOptionsPanel());
        mainPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        // 5. Status Banner
        mainPanel.add(createStatusPanel());
        mainPanel.add(Box.createRigidArea(new Dimension(0, 12)));

        // 6. Action Buttons Bar
        mainPanel.add(createActionBar());

        getContentPane().add(mainPanel, BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(null);
    }

    private JMenuBar createMenuBar() {
        var menuBar = new JMenuBar();

        var fileMenu = new JMenu("File");
        fileMenu.setMnemonic(KeyEvent.VK_F);

        var changeNowItem = new JMenuItem("Change Wallpaper Now");
        changeNowItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        changeNowItem.addActionListener(e -> {
            saveUIToConfig();
            syncService.changeWallpaperNow();
        });

        var saveItem = new JMenuItem("Save Settings");
        saveItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        saveItem.addActionListener(e -> {
            saveUIToConfig();
            syncService.reschedule();
            JOptionPane.showMessageDialog(this, "Settings saved successfully!", "Wallpaper Changer", JOptionPane.INFORMATION_MESSAGE);
        });

        var closeToTrayItem = new JMenuItem("Close to Tray");
        closeToTrayItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        closeToTrayItem.addActionListener(e -> hideToTray());

        var exitItem = new JMenuItem("Exit Application");
        exitItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        exitItem.addActionListener(e -> confirmAndExit());

        fileMenu.add(changeNowItem);
        fileMenu.add(saveItem);
        fileMenu.addSeparator();
        fileMenu.add(closeToTrayItem);
        fileMenu.add(exitItem);

        menuBar.add(fileMenu);
        return menuBar;
    }

    private JPanel createHeaderPanel() {
        var header = new JPanel(new BorderLayout());
        header.setBackground(new Color(41, 128, 185));
        header.setBorder(new EmptyBorder(12, 16, 12, 16));

        var title = new JLabel("Wallpaper Changer");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(Color.WHITE);

        var subtitle = new JLabel("Synchronize Windows desktop backgrounds from a Google Drive folder");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subtitle.setForeground(new Color(230, 240, 255));

        header.add(title, BorderLayout.NORTH);
        header.add(subtitle, BorderLayout.SOUTH);
        return header;
    }

    private JPanel createDrivePanel() {
        var panel = new JPanel(new GridBagLayout());
        panel.setBorder(new CompoundBorder(
                new TitledBorder("Google Drive Settings"),
                new EmptyBorder(6, 10, 6, 10)
        ));

        var gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Row 0: Credentials File (Step 1)
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
        panel.add(new JLabel("OAuth Credentials (.json):"), gbc);

        gbc.gridx = 1; gbc.weightx = 1;
        var credRow = new JPanel(new BorderLayout(6, 0));
        customCredentialsField = new JTextField(24);
        customCredentialsField.setToolTipText("Path to Google OAuth credentials JSON (downloaded from Google Cloud Console)");

        browseCredentialsButton = new JButton("Browse...");
        browseCredentialsButton.setToolTipText("Select the credentials JSON file downloaded from Google Cloud Console");
        browseCredentialsButton.addActionListener(e -> onBrowseCredentials());

        credRow.add(customCredentialsField, BorderLayout.CENTER);
        credRow.add(browseCredentialsButton, BorderLayout.EAST);
        panel.add(credRow, gbc);

        // Row 1: Credentials status / hint
        gbc.gridx = 1; gbc.gridy = 1;
        credHintLabel = new JLabel(" ");
        credHintLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        panel.add(credHintLabel, gbc);

        // Row 2: Account Authentication (Step 2)
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0;
        panel.add(new JLabel("Google Account:"), gbc);

        gbc.gridx = 1; gbc.weightx = 1;
        var authRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        authStatusLabel = new JLabel("Checking status...");
        authStatusLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));

        authButton = new JButton("Authenticate with Google");
        authButton.addActionListener(e -> onAuthenticateClicked());

        signOutButton = new JButton("Sign Out");
        signOutButton.addActionListener(e -> onSignOutClicked());

        authRow.add(authStatusLabel);
        authRow.add(authButton);
        authRow.add(signOutButton);
        panel.add(authRow, gbc);

        // Row 3: Drive Folder ID / URL (Step 3)
        gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 0;
        panel.add(new JLabel("Drive Folder ID / URL:"), gbc);

        gbc.gridx = 1; gbc.weightx = 1;
        var folderRow = new JPanel(new BorderLayout(6, 0));
        driveFolderField = new JTextField(24);
        driveFolderField.setToolTipText("Enter Drive folder ID or paste full Google Drive folder URL");

        testFolderButton = new JButton("Test Folder");
        testFolderButton.addActionListener(e -> onTestFolderClicked());

        folderRow.add(driveFolderField, BorderLayout.CENTER);
        folderRow.add(testFolderButton, BorderLayout.EAST);
        panel.add(folderRow, gbc);

        // Row 4: Folder test feedback
        gbc.gridx = 1; gbc.gridy = 4;
        testFolderResultLabel = new JLabel(" ");
        testFolderResultLabel.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        panel.add(testFolderResultLabel, gbc);

        return panel;
    }

    private JPanel createLocalFolderPanel() {
        var panel = new JPanel(new GridBagLayout());
        panel.setBorder(new CompoundBorder(
                new TitledBorder("Local Wallpaper Folder"),
                new EmptyBorder(6, 10, 6, 10)
        ));

        var gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
        panel.add(new JLabel("Local Directory:"), gbc);

        gbc.gridx = 1; gbc.weightx = 1;
        var pathRow = new JPanel(new BorderLayout(6, 0));
        localFolderField = new JTextField(24);

        var btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        browseFolderButton = new JButton("Browse...");
        browseFolderButton.addActionListener(e -> onBrowseLocalFolder());

        openFolderButton = new JButton("Open in Explorer");
        openFolderButton.addActionListener(e -> onOpenLocalFolder());

        btnRow.add(browseFolderButton);
        btnRow.add(openFolderButton);

        pathRow.add(localFolderField, BorderLayout.CENTER);
        pathRow.add(btnRow, BorderLayout.EAST);
        panel.add(pathRow, gbc);

        return panel;
    }

    private JPanel createOptionsPanel() {
        var panel = new JPanel(new GridBagLayout());
        panel.setBorder(new CompoundBorder(
                new TitledBorder("Timing & Behavior"),
                new EmptyBorder(6, 10, 6, 10)
        ));

        var gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Frequency Row
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
        panel.add(new JLabel("Change Frequency:"), gbc);

        gbc.gridx = 1; gbc.weightx = 1;
        var freqRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        String[] presets = {"5 minutes", "15 minutes", "30 minutes", "1 hour", "2 hours", "6 hours", "12 hours", "24 hours", "Custom (minutes)"};
        frequencyPresetCombo = new JComboBox<>(presets);
        customFrequencySpinner = new JSpinner(new SpinnerNumberModel(60, 1, 10080, 5));
        customFrequencySpinner.setEnabled(false);

        frequencyPresetCombo.addActionListener(e -> {
            boolean isCustom = "Custom (minutes)".equals(frequencyPresetCombo.getSelectedItem());
            customFrequencySpinner.setEnabled(isCustom);
        });

        freqRow.add(frequencyPresetCombo);
        freqRow.add(new JLabel("Minutes:"));
        freqRow.add(customFrequencySpinner);
        panel.add(freqRow, gbc);

        // Delete unset wallpaper checkbox
        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 2;
        deleteUnsetCheckbox = new JCheckBox("Delete downloaded wallpaper after it has been unset");
        deleteUnsetCheckbox.setToolTipText("Ensures only the currently active wallpaper is kept on local disk");
        panel.add(deleteUnsetCheckbox, gbc);

        // Start on startup checkbox
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
        startOnStartupCheckbox = new JCheckBox("Start application automatically at Windows startup");
        startOnStartupCheckbox.setToolTipText("Configures Windows Registry Run key to launch in background on startup");
        panel.add(startOnStartupCheckbox, gbc);

        // Wallpaper style
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 1;
        panel.add(new JLabel("Wallpaper Style:"), gbc);

        gbc.gridx = 1;
        String[] styles = {"Fill / Stretch", "Fit", "Center", "Tile", "Span"};
        styleCombo = new JComboBox<>(styles);
        panel.add(styleCombo, gbc);

        return panel;
    }

    private JPanel createStatusPanel() {
        // Use a 2-row layout so status and next-change timing never overlap regardless of filename length
        var panel = new JPanel(new java.awt.GridLayout(2, 1, 0, 4));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(210, 215, 220)),
                new EmptyBorder(8, 12, 8, 12)
        ));
        panel.setBackground(new Color(245, 247, 250));

        statusLabel = new JLabel("Status: Ready");
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        nextSyncLabel = new JLabel("Next Change: Scheduled");
        nextSyncLabel.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        nextSyncLabel.setForeground(new Color(100, 110, 120));

        panel.add(statusLabel);
        panel.add(nextSyncLabel);
        return panel;
    }

    private JPanel createActionBar() {
        var bar = new JPanel(new BorderLayout());

        var leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        exitAppButton = new JButton("Exit Application");
        exitAppButton.setForeground(new Color(180, 40, 40));
        exitAppButton.setToolTipText("Completely exit Wallpaper Changer and stop background rotation (Ctrl+Q)");
        exitAppButton.addActionListener(e -> confirmAndExit());
        leftPanel.add(exitAppButton);

        var rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        changeNowButton = new JButton("Change Wallpaper Now");
        changeNowButton.addActionListener(e -> {
            saveUIToConfig();
            syncService.changeWallpaperNow();
        });

        saveButton = new JButton("Save Settings");
        saveButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
        saveButton.addActionListener(e -> {
            saveUIToConfig();
            syncService.reschedule();
            JOptionPane.showMessageDialog(this, "Settings saved successfully!", "Wallpaper Changer", JOptionPane.INFORMATION_MESSAGE);
        });

        closeToTrayButton = new JButton("Close to Tray");
        closeToTrayButton.addActionListener(e -> hideToTray());

        rightPanel.add(changeNowButton);
        rightPanel.add(saveButton);
        rightPanel.add(closeToTrayButton);

        bar.add(leftPanel, BorderLayout.WEST);
        bar.add(rightPanel, BorderLayout.EAST);
        return bar;
    }

    private void updateAuthStatus() {
        boolean isServiceAcc = DriveAPI.isServiceAccount();
        boolean authorized = DriveAPI.isAuthorized();
        boolean hasCreds = DriveAPI.hasCredentialsFile();

        if (isServiceAcc) {
            String email = DriveAPI.getServiceAccountEmail();
            authStatusLabel.setText("● Service Account Ready");
            authStatusLabel.setForeground(new Color(39, 174, 96));
            authButton.setText("Test Connection");
            signOutButton.setEnabled(false);
            credHintLabel.setText("Share Drive folder with: " + (email != null ? email : "the Service Account") + " (Viewer)");
            credHintLabel.setForeground(new Color(41, 128, 185));
            credHintLabel.setToolTipText(email != null ? "Service Account: " + email : "");
        } else if (authorized) {
            authStatusLabel.setText("● Connected to Google");
            authStatusLabel.setForeground(new Color(39, 174, 96));
            authButton.setText("Re-authenticate");
            signOutButton.setEnabled(true);
            credHintLabel.setText("✓ Connected. Google Drive OAuth tokens are active.");
            credHintLabel.setForeground(new Color(39, 174, 96));
            credHintLabel.setToolTipText("");
        } else {
            authStatusLabel.setText("○ Not Authenticated");
            authStatusLabel.setForeground(new Color(192, 57, 43));
            authButton.setText("Authenticate with Google");
            signOutButton.setEnabled(false);
            credHintLabel.setToolTipText("");

            if (!hasCreds) {
                credHintLabel.setText("Step 1: Click 'Browse...' above to select your Service Account or OAuth JSON.");
                credHintLabel.setForeground(new Color(211, 84, 0));
            } else {
                credHintLabel.setText("✓ Credentials ready! Step 2: Click 'Authenticate with Google' to sign in.");
                credHintLabel.setForeground(new Color(41, 128, 185));
            }
        }
    }

    private void onAuthenticateClicked() {
        saveUIToConfig();

        if (!DriveAPI.hasCredentialsFile()) {
            int pick = JOptionPane.showConfirmDialog(
                    this,
                    "Credentials JSON is required before authenticating.\n\n" +
                    "Would you like to select the JSON file you downloaded from Google Cloud?",
                    "Credentials Required",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.INFORMATION_MESSAGE
            );
            if (pick == JOptionPane.YES_OPTION) {
                onBrowseCredentials();
            }
            return;
        }

        if (DriveAPI.isServiceAccount()) {
            authButton.setEnabled(false);
            authStatusLabel.setText("Testing Service Account...");

            CompletableFuture.runAsync(() -> {
                try {
                    DriveAPI.authenticate();
                    SwingUtilities.invokeLater(() -> {
                        authButton.setEnabled(true);
                        updateAuthStatus();
                        String email = DriveAPI.getServiceAccountEmail();
                        JOptionPane.showMessageDialog(
                                SettingsFrame.this,
                                "Service Account connection verified successfully!\n\n" +
                                "Service Account Email:\n" + (email != null ? email : "") + "\n\n" +
                                "Next Steps:\n" +
                                "1. Right-click your wallpapers folder in Google Drive -> Share\n" +
                                "2. Paste this email as 'Viewer'\n" +
                                "3. Paste your folder URL below and click 'Test Folder'!",
                                "Service Account Active",
                                JOptionPane.INFORMATION_MESSAGE
                        );
                    });
                } catch (Exception e) {
                    SwingUtilities.invokeLater(() -> {
                        authButton.setEnabled(true);
                        updateAuthStatus();
                        var msg = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
                        JOptionPane.showMessageDialog(
                                SettingsFrame.this,
                                "Service Account verification failed: " + msg,
                                "Verification Error",
                                JOptionPane.ERROR_MESSAGE
                        );
                    });
                }
            });
            return;
        }

        authButton.setEnabled(false);
        authStatusLabel.setText("Opening browser...");

        CompletableFuture.runAsync(() -> {
            try {
                DriveAPI.authenticate();
                SwingUtilities.invokeLater(() -> {
                    authButton.setEnabled(true);
                    updateAuthStatus();
                    JOptionPane.showMessageDialog(SettingsFrame.this,
                            "Google Drive authorization successful!",
                            "Authentication",
                            JOptionPane.INFORMATION_MESSAGE);
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    authButton.setEnabled(true);
                    updateAuthStatus();
                    var msg = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
                    JOptionPane.showMessageDialog(SettingsFrame.this,
                            "Authentication failed: " + msg,
                            "Authentication Error",
                            JOptionPane.ERROR_MESSAGE);
                });
            }
        });
    }

    private void onSignOutClicked() {
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to sign out and clear stored Google credentials?",
                "Sign Out",
                JOptionPane.YES_NO_OPTION
        );
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                DriveAPI.signOut();
                updateAuthStatus();
                JOptionPane.showMessageDialog(this, "Successfully signed out.", "Signed Out", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Error signing out: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void onTestFolderClicked() {
        var input = driveFolderField.getText();
        var folderId = AppConfig.extractDriveFolderId(input);
        if (folderId.isBlank()) {
            JOptionPane.showMessageDialog(this, "Please enter a valid Google Drive Folder ID or URL.", "Input Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        testFolderButton.setEnabled(false);
        testFolderResultLabel.setForeground(Color.DARK_GRAY);
        testFolderResultLabel.setText("Connecting to Google Drive...");

        CompletableFuture.supplyAsync(() -> {
            try {
                var connector = new DriveConnector();
                return connector.countImagesInFolder(folderId);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).whenComplete((count, error) -> SwingUtilities.invokeLater(() -> {
            testFolderButton.setEnabled(true);
            if (error == null) {
                testFolderResultLabel.setForeground(new Color(39, 174, 96));
                testFolderResultLabel.setText("✓ Success: Found %d wallpaper image(s) in folder.".formatted(count));
            } else {
                testFolderResultLabel.setForeground(new Color(192, 57, 43));
                var cause = error.getCause() != null ? error.getCause().getMessage() : error.getMessage();
                testFolderResultLabel.setText("✗ Failed: " + cause);
            }
        }));
    }

    private void onBrowseCredentials() {
        var chooser = new JFileChooser();
        chooser.setDialogTitle("Select Google OAuth Credentials JSON");
        chooser.setFileFilter(new FileNameExtensionFilter("JSON Files (*.json)", "json"));

        var downloadsDir = Path.of(System.getProperty("user.home"), "Downloads");
        if (Files.isDirectory(downloadsDir)) {
            chooser.setCurrentDirectory(downloadsDir.toFile());
        }

        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            var selectedFile = chooser.getSelectedFile().toPath();
            var targetPath = selectedFile;
            // Copy to app dir so user clearing their Downloads folder doesn't break credentials later
            try {
                var appDir = DriveAPI.getDefaultCredentialsPath().getParent();
                if (appDir != null && !Files.exists(appDir)) {
                    Files.createDirectories(appDir);
                }
                var defaultTarget = DriveAPI.getDefaultCredentialsPath();
                Files.copy(selectedFile, defaultTarget, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                targetPath = defaultTarget;
            } catch (Exception ignored) {
            }

            customCredentialsField.setText(targetPath.toAbsolutePath().toString());
            saveUIToConfig();
            updateAuthStatus();

            if (DriveAPI.isServiceAccount()) {
                String email = DriveAPI.getServiceAccountEmail();
                JOptionPane.showMessageDialog(
                        this,
                        "Service Account key loaded successfully!\n\n" +
                        "Service Account Email:\n" + (email != null ? email : "") + "\n\n" +
                        "Next Steps:\n" +
                        "1. Right-click your wallpapers folder in Google Drive -> Share\n" +
                        "2. Paste this email as 'Viewer'\n" +
                        "3. Paste your folder URL below and click 'Test Folder'!",
                        "Service Account Ready (Permanent)",
                        JOptionPane.INFORMATION_MESSAGE
                );
            } else {
                JOptionPane.showMessageDialog(
                        this,
                        "OAuth credentials loaded successfully!\nNow click 'Authenticate with Google' to sign in.",
                        "Credentials Ready",
                        JOptionPane.INFORMATION_MESSAGE
                );
            }
        }
    }

    private void onBrowseLocalFolder() {
        var chooser = new JFileChooser();
        chooser.setDialogTitle("Select Local Wallpaper Folder");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            localFolderField.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }

    private void onOpenLocalFolder() {
        var pathStr = localFolderField.getText().strip();
        if (pathStr.isBlank()) return;
        var dir = Path.of(pathStr);
        try {
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
            }
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(dir.toFile());
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Could not open folder: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadConfigToUI() {
        ConfigData data = config.data();
        driveFolderField.setText(data.driveFolderId());

        var customPath = data.customCredentialsPath();
        if (customPath != null && !customPath.isBlank()) {
            customCredentialsField.setText(customPath);
        } else if (Files.isRegularFile(DriveAPI.getDefaultCredentialsPath())) {
            customCredentialsField.setText(DriveAPI.getDefaultCredentialsPath().toAbsolutePath().toString());
        } else {
            customCredentialsField.setText("");
        }

        localFolderField.setText(data.localFolderPath());

        deleteUnsetCheckbox.setSelected(data.deleteUnsetWallpaper());
        startOnStartupCheckbox.setSelected(data.startOnStartup());

        int freq = data.frequencyMinutes();
        String preset = switch (freq) {
            case 5 -> "5 minutes";
            case 15 -> "15 minutes";
            case 30 -> "30 minutes";
            case 60 -> "1 hour";
            case 120 -> "2 hours";
            case 360 -> "6 hours";
            case 720 -> "12 hours";
            case 1440 -> "24 hours";
            default -> "Custom (minutes)";
        };

        frequencyPresetCombo.setSelectedItem(preset);
        if ("Custom (minutes)".equals(preset)) {
            customFrequencySpinner.setValue(freq);
            customFrequencySpinner.setEnabled(true);
        }

        String style = data.wallpaperStyle();
        String styleName = switch (style) {
            case "6" -> "Fit";
            case "0" -> "Center";
            case "1" -> "Tile";
            case "22" -> "Span";
            default -> "Fill / Stretch";
        };
        styleCombo.setSelectedItem(styleName);
    }

    private void saveUIToConfig() {
        var folderId = AppConfig.extractDriveFolderId(driveFolderField.getText());
        var credPath = customCredentialsField.getText().strip();
        var localPath = localFolderField.getText().strip();
        boolean deleteUnset = deleteUnsetCheckbox.isSelected();
        boolean startStartup = startOnStartupCheckbox.isSelected();

        StartupManager.setStartupEnabled(startStartup);

        var selectedPreset = (String) frequencyPresetCombo.getSelectedItem();
        int minutes = switch (selectedPreset != null ? selectedPreset : "1 hour") {
            case "5 minutes" -> 5;
            case "15 minutes" -> 15;
            case "30 minutes" -> 30;
            case "1 hour" -> 60;
            case "2 hours" -> 120;
            case "6 hours" -> 360;
            case "12 hours" -> 720;
            case "24 hours" -> 1440;
            case "Custom (minutes)" -> (Integer) customFrequencySpinner.getValue();
            default -> 60;
        };

        var styleSel = (String) styleCombo.getSelectedItem();
        var styleVal = WallpaperSetter.toRegistryStyle(styleSel != null ? styleSel : "Fill / Stretch");

        var current = config.data();
        var updated = new ConfigData(
                folderId, localPath, minutes, deleteUnset, startStartup,
                credPath, current.lastWallpaperPath(), current.lastWallpaperDriveId(), styleVal
        );
        config.update(updated);
        updateAuthStatus();
    }

    public void hideToTray() {
        setVisible(false);
        if (trayManager != null) {
            trayManager.showNotification(
                    "Wallpaper Changer",
                    "Application is running in the background. Use the System Tray icon to configure or exit.",
                    TrayIcon.MessageType.INFO
            );
        }
    }

    /**
     * Smartly truncates long file names while keeping the file extension visible.
     */
    public static String truncateFileName(String name, int maxLen) {
        if (name == null || name.length() <= maxLen) {
            return name != null ? name : "";
        }
        int extIdx = name.lastIndexOf('.');
        String ext = extIdx != -1 ? name.substring(extIdx) : "";
        String baseName = extIdx != -1 ? name.substring(0, extIdx) : name;
        int avail = maxLen - ext.length() - 3;
        if (avail < 4) {
            return name.substring(0, Math.max(1, maxLen - 3)) + "...";
        }
        int half = avail / 2;
        int secondHalf = avail - half;
        return baseName.substring(0, half) + "..." + baseName.substring(baseName.length() - secondHalf) + ext;
    }

    // SyncListener callbacks (Thread-safe UI updates)

    @Override
    public void onSyncStarted() {
        SwingUtilities.invokeLater(() -> {
            changeNowButton.setEnabled(false);
            statusLabel.setText("Status: Syncing with Google Drive...");
            statusLabel.setToolTipText("Sync in progress");
        });
    }

    @Override
    public void onProgress(String status) {
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText("Status: " + status);
            statusLabel.setToolTipText(status);
        });
    }

    @Override
    public void onSyncSuccess(String wallpaperName, Path localPath) {
        SwingUtilities.invokeLater(() -> {
            changeNowButton.setEnabled(true);
            String timeStr = dateFormat.format(new Date());
            String display = truncateFileName(wallpaperName, 42);
            statusLabel.setText("Status: Updated to %s (%s)".formatted(display, timeStr));
            statusLabel.setToolTipText("Active Wallpaper: %s\nPath: %s\nUpdated: %s".formatted(wallpaperName, localPath, timeStr));
        });
    }

    @Override
    public void onSyncFailed(String reason, Throwable error) {
        SwingUtilities.invokeLater(() -> {
            changeNowButton.setEnabled(true);
            String display = truncateFileName(reason, 50);
            statusLabel.setText("Status: Error: " + display);
            statusLabel.setToolTipText("Error: " + reason);
        });
    }

    @Override
    public void onScheduleUpdated(Date nextRunTime) {
        SwingUtilities.invokeLater(() -> {
            if (nextRunTime != null) {
                nextSyncLabel.setText("Next Change: %s (Every %d min)".formatted(
                        dateFormat.format(nextRunTime), config.data().frequencyMinutes()
                ));
            }
        });
    }

    public void confirmAndExit() {
        int choice = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to exit Wallpaper Changer?\n(Background wallpaper rotation will stop)",
                "Exit Wallpaper Changer",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );
        if (choice == JOptionPane.YES_OPTION) {
            performExit();
        }
    }

    public void performExit() {
        if (trayManager != null) {
            trayManager.cleanup();
        }
        if (syncService != null) {
            syncService.shutdown();
        }
        SingleInstanceManager.cleanup();
        dispose();
        System.exit(0);
    }

    public JButton getExitAppButton() {
        return exitAppButton;
    }

    public JButton getCloseToTrayButton() {
        return closeToTrayButton;
    }

    public JButton getSaveButton() {
        return saveButton;
    }

    public JButton getChangeNowButton() {
        return changeNowButton;
    }
}
