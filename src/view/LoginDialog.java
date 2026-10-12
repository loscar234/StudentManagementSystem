package view;

import util.AccessDeniedException;
import util.AuthService;
import util.SessionManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.sql.SQLException;

/**
 * LoginDialog – Secure authentication modal dialog.
 * Supports first-run zero-credential administrator provisioning
 * and role-based login with attempt throttling.
 */
public class LoginDialog extends JDialog {

    private final AuthService authService;
    private boolean authenticated = false;

    private JTextField txtUsername;
    private JPasswordField txtPassword;
    private JPasswordField txtConfirmPassword;

    public LoginDialog(Frame parent) {
        super(parent, "System Authentication", true);
        this.authService = new AuthService();
        UITheme.applyTheme();
        initDialog();
    }

    private void initDialog() {
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        setResizable(false);

        boolean provisioned = false;
        try {
            provisioned = authService.isSystemProvisioned();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(
                this,
                "Unable to connect to database to verify accounts: " + e.getMessage(),
                "Database Connection Error",
                JOptionPane.ERROR_MESSAGE
            );
        }

        if (!provisioned) {
            buildProvisioningUI();
        } else {
            buildLoginUI();
        }

        pack();
        setLocationRelativeTo(getParent());

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (!authenticated) {
                    authenticated = false;
                }
            }
        });
    }

    private void buildProvisioningUI() {
        setTitle("Initial Setup – Administrator Provisioning");
        JPanel content = new JPanel(new BorderLayout(0, 15));
        content.setBackground(UITheme.BACKGROUND);
        content.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));

        JPanel header = new JPanel(new GridLayout(2, 1, 0, 4));
        header.setBackground(UITheme.BACKGROUND);
        JLabel lblTitle = new JLabel("First-Time Security Setup");
        lblTitle.setFont(UITheme.FONT_TITLE);
        lblTitle.setForeground(UITheme.PRIMARY);

        JLabel lblSub = new JLabel("<html>Create the root system administrator account.<br/>No hardcoded default accounts exist.</html>");
        lblSub.setFont(UITheme.FONT_SMALL);
        lblSub.setForeground(UITheme.TEXT_SECONDARY);
        header.add(lblTitle);
        header.add(lblSub);
        content.add(header, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(UITheme.BACKGROUND);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        txtUsername = UITheme.styledField(18);
        txtPassword = new JPasswordField(18);
        txtPassword.setFont(UITheme.FONT_BODY);
        txtConfirmPassword = new JPasswordField(18);
        txtConfirmPassword.setFont(UITheme.FONT_BODY);

        gbc.gridx = 0; gbc.gridy = 0;
        form.add(new JLabel("Admin Username:"), gbc);
        gbc.gridx = 1;
        form.add(txtUsername, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        form.add(new JLabel("Password (min 8 chars):"), gbc);
        gbc.gridx = 1;
        form.add(txtPassword, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        form.add(new JLabel("Confirm Password:"), gbc);
        gbc.gridx = 1;
        form.add(txtConfirmPassword, gbc);

        content.add(form, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnPanel.setBackground(UITheme.BACKGROUND);

        JButton btnSubmit = UITheme.primaryButton("Provision Administrator");
        JButton btnCancel = UITheme.dangerButton("Exit");

        btnSubmit.addActionListener(e -> handleProvisioning());
        btnCancel.addActionListener(e -> {
            authenticated = false;
            dispose();
        });

        btnPanel.add(btnCancel);
        btnPanel.add(btnSubmit);
        content.add(btnPanel, BorderLayout.SOUTH);

        getRootPane().setDefaultButton(btnSubmit);
        setContentPane(content);
    }

    private void handleProvisioning() {
        String username = txtUsername.getText().trim();
        String pass = new String(txtPassword.getPassword());
        String confirm = new String(txtConfirmPassword.getPassword());

        if (username.length() < 3) {
            showError("Username must be at least 3 characters.");
            return;
        }
        if (pass.length() < 8) {
            showError("Password must be at least 8 characters long.");
            return;
        }
        if (!pass.equals(confirm)) {
            showError("Passwords do not match.");
            return;
        }

        try {
            authService.provisionInitialAdmin(username, pass);
            boolean ok = authService.login(username, pass);
            if (ok) {
                authenticated = true;
                JOptionPane.showMessageDialog(
                    this,
                    "Administrator account '" + username + "' provisioned successfully!\nLogging into system...",
                    "Setup Complete",
                    JOptionPane.INFORMATION_MESSAGE
                );
                dispose();
            } else {
                showError("Admin created, but initial login failed. Please sign in.");
                getContentPane().removeAll();
                buildLoginUI();
                revalidate();
                repaint();
                pack();
            }
        } catch (IllegalArgumentException | IllegalStateException ex) {
            showError(ex.getMessage());
        } catch (SQLException ex) {
            showError("Database error during provisioning: " + ex.getMessage());
        }
    }

    private void buildLoginUI() {
        setTitle("Sign In – Student Management System");
        JPanel content = new JPanel(new BorderLayout(0, 15));
        content.setBackground(UITheme.BACKGROUND);
        content.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));

        JPanel header = new JPanel(new GridLayout(2, 1, 0, 4));
        header.setBackground(UITheme.BACKGROUND);
        JLabel lblTitle = new JLabel("System Login");
        lblTitle.setFont(UITheme.FONT_TITLE);
        lblTitle.setForeground(UITheme.PRIMARY);

        JLabel lblSub = new JLabel("Please enter your credentials to authenticate.");
        lblSub.setFont(UITheme.FONT_SMALL);
        lblSub.setForeground(UITheme.TEXT_SECONDARY);
        header.add(lblTitle);
        header.add(lblSub);
        content.add(header, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(UITheme.BACKGROUND);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        txtUsername = UITheme.styledField(18);
        txtPassword = new JPasswordField(18);
        txtPassword.setFont(UITheme.FONT_BODY);

        gbc.gridx = 0; gbc.gridy = 0;
        form.add(new JLabel("Username:"), gbc);
        gbc.gridx = 1;
        form.add(txtUsername, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        form.add(new JLabel("Password:"), gbc);
        gbc.gridx = 1;
        form.add(txtPassword, gbc);

        content.add(form, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnPanel.setBackground(UITheme.BACKGROUND);

        JButton btnLogin = UITheme.primaryButton("Sign In");
        JButton btnCancel = UITheme.dangerButton("Exit");

        btnLogin.addActionListener(e -> handleLogin());
        btnCancel.addActionListener(e -> {
            authenticated = false;
            dispose();
        });

        btnPanel.add(btnCancel);
        btnPanel.add(btnLogin);
        content.add(btnPanel, BorderLayout.SOUTH);

        getRootPane().setDefaultButton(btnLogin);
        setContentPane(content);
    }

    private void handleLogin() {
        String username = txtUsername.getText().trim();
        String pass = new String(txtPassword.getPassword());

        if (username.isEmpty() || pass.isEmpty()) {
            showError("Please enter both username and password.");
            return;
        }

        try {
            boolean success = authService.login(username, pass);
            if (success) {
                authenticated = true;
                dispose();
            } else {
                showError("Invalid username or password.");
                txtPassword.setText("");
                txtPassword.requestFocus();
            }
        } catch (AccessDeniedException ex) {
            showError("Access Denied: " + ex.getMessage());
        } catch (SQLException ex) {
            showError("Database connection error: " + ex.getMessage());
        }
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Authentication Notice", JOptionPane.WARNING_MESSAGE);
    }

    public boolean isAuthenticated() {
        return authenticated;
    }
}
