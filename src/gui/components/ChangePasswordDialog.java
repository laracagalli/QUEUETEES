package gui.components;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import model.User;
import service.AuthService;

/**
 * Modal dialog for changing password, shared across all roles.
 * Shows current password, new password, confirm password with
 * real-time validation indicators matching ForgotPasswordPanel style.
 */
public final class ChangePasswordDialog extends JDialog {

    private static final Color GREEN  = new Color(55, 70, 56);
    private static final Color INK    = new Color(28, 31, 27);
    private static final Color MUTED  = new Color(99, 106, 96);
    private static final Color PAPER  = new Color(252, 252, 247);
    private static final Color LINE   = new Color(218, 220, 209);
    private static final Color ERROR  = new Color(175, 50, 40);
    private static final Color OK     = new Color(40, 135, 45);

    private final JPasswordField currentPass  = passwordField();
    private final JPasswordField newPass      = passwordField();
    private final JPasswordField confirmPass  = passwordField();

    private final JLabel lengthCheck  = checkLabel("8–16 characters");
    private final JLabel upperCheck   = checkLabel("1 uppercase letter");
    private final JLabel specialCheck = checkLabel("1 special character");
    private final JLabel matchCheck   = checkLabel("Passwords match");

    private final JLabel statusLabel  = new JLabel(" ");

    public ChangePasswordDialog(Window owner, AuthService authService, User user) {
        super(owner, "Change Password", ModalityType.APPLICATION_MODAL);
        setResizable(false);

        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBackground(PAPER);
        root.setBorder(new EmptyBorder(28, 32, 24, 32));

        // ── Title ──
        JLabel title = new JLabel("Change password");
        title.setFont(new Font("Fira Code", Font.BOLD, 18));
        title.setForeground(INK);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        root.add(title);
        root.add(Box.createVerticalStrut(4));

        JLabel subtitle = new JLabel("Enter your current password, then choose a new one.");
        subtitle.setFont(new Font("Fira Code", Font.PLAIN, 11));
        subtitle.setForeground(MUTED);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        root.add(subtitle);
        root.add(Box.createVerticalStrut(22));

        // ── Fields ──
        root.add(fieldBlock("Current password", currentPass));
        root.add(Box.createVerticalStrut(12));
        root.add(fieldBlock("New password", newPass));
        root.add(Box.createVerticalStrut(4));

        // Validation indicators (2x2 grid)
        JPanel checks = new JPanel(new GridLayout(2, 2, 8, 4));
        checks.setOpaque(false);
        checks.setAlignmentX(Component.LEFT_ALIGNMENT);
        checks.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        checks.add(lengthCheck);
        checks.add(upperCheck);
        checks.add(specialCheck);
        checks.add(matchCheck);
        root.add(checks);
        root.add(Box.createVerticalStrut(12));

        root.add(fieldBlock("Confirm new password", confirmPass));
        root.add(Box.createVerticalStrut(16));

        // Show/hide checkbox
        JCheckBox showPass = new JCheckBox("Show passwords");
        showPass.setOpaque(false);
        showPass.setFont(new Font("Fira Code", Font.PLAIN, 11));
        showPass.setForeground(GREEN);
        showPass.setFocusPainted(false);
        showPass.setAlignmentX(Component.LEFT_ALIGNMENT);
        showPass.addActionListener(e -> {
            char echo = showPass.isSelected() ? (char) 0 : '•';
            newPass.setEchoChar(echo);
            confirmPass.setEchoChar(echo);
        });
        root.add(showPass);
        root.add(Box.createVerticalStrut(18));

        // Status message
        statusLabel.setFont(new Font("Fira Code", Font.PLAIN, 11));
        statusLabel.setForeground(ERROR);
        statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        root.add(statusLabel);
        root.add(Box.createVerticalStrut(6));

        // ── Buttons ──
        JPanel buttons = new JPanel(new GridLayout(1, 2, 12, 0));
        buttons.setOpaque(false);
        buttons.setAlignmentX(Component.LEFT_ALIGNMENT);
        buttons.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));

        RoundedButton cancelBtn = new RoundedButton("Cancel", new Color(230, 233, 225), INK);
        cancelBtn.setFont(new Font("Fira Code", Font.BOLD, 12));
        cancelBtn.setHoverColor(new Color(218, 222, 212));
        cancelBtn.addActionListener(e -> dispose());

        RoundedButton saveBtn = new RoundedButton("Save password", INK, Color.WHITE);
        saveBtn.setFont(new Font("Fira Code", Font.BOLD, 12));
        saveBtn.setHoverColor(new Color(65, 70, 63));
        saveBtn.addActionListener(e -> {
            statusLabel.setText(" ");
            try {
                authService.changePassword(user,
                        currentPass.getPassword(),
                        newPass.getPassword(),
                        confirmPass.getPassword());
                JOptionPane.showMessageDialog(this,
                        "Password changed successfully.",
                        "QueueTees", JOptionPane.INFORMATION_MESSAGE);
                dispose();
            } catch (IllegalArgumentException ex) {
                statusLabel.setText(ex.getMessage());
                statusLabel.setForeground(ERROR);
                currentPass.setText("");
                newPass.setText("");
                confirmPass.setText("");
            }
        });

        buttons.add(cancelBtn);
        buttons.add(saveBtn);
        root.add(buttons);

        // ── Real-time validation ──
        DocumentListener validator = new DocumentListener() {
            public void insertUpdate(DocumentEvent e)  { validate(); }
            public void removeUpdate(DocumentEvent e)  { validate(); }
            public void changedUpdate(DocumentEvent e) { validate(); }
            void validate() {
                String pwd = new String(newPass.getPassword());
                String chk = new String(confirmPass.getPassword());
                update(lengthCheck,  pwd.length() >= 8 && pwd.length() <= 16, "8–16 characters");
                update(upperCheck,   pwd.matches(".*[A-Z].*"),                 "1 uppercase letter");
                update(specialCheck, pwd.matches(".*[^a-zA-Z0-9].*"),          "1 special character");
                update(matchCheck,   !pwd.isEmpty() && pwd.equals(chk),        "Passwords match");
            }
        };
        newPass.getDocument().addDocumentListener(validator);
        confirmPass.getDocument().addDocumentListener(validator);

        setContentPane(root);
        pack();
        setSize(420, getHeight() + 10);
        setLocationRelativeTo(owner);
    }

    /** Convenience: show the dialog from any component. */
    public static void show(Component parent, AuthService authService, User user) {
        Window owner = SwingUtilities.getWindowAncestor(parent);
        new ChangePasswordDialog(owner, authService, user).setVisible(true);
    }

    // ── Helpers ──

    private static JPasswordField passwordField() {
        JPasswordField f = new JPasswordField();
        f.setFont(new Font("Fira Code", Font.PLAIN, 13));
        f.setBackground(new Color(248, 248, 242));
        f.setForeground(INK);
        f.setCaretColor(GREEN);
        f.setEchoChar('•');
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(LINE),
                new EmptyBorder(8, 12, 8, 12)));
        return f;
    }

    private static JPanel fieldBlock(String labelText, JComponent field) {
        JPanel block = new JPanel();
        block.setLayout(new BoxLayout(block, BoxLayout.Y_AXIS));
        block.setOpaque(false);
        block.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel lbl = new JLabel(labelText);
        lbl.setFont(new Font("Fira Code", Font.PLAIN, 11));
        lbl.setForeground(MUTED);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        block.add(lbl);
        block.add(Box.createVerticalStrut(4));
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
        block.add(field);
        return block;
    }

    private static JLabel checkLabel(String text) {
        JLabel label = new JLabel("✕ " + text);
        label.setFont(new Font("Fira Code", Font.PLAIN, 10));
        label.setForeground(ERROR);
        return label;
    }

    private static void update(JLabel label, boolean valid, String text) {
        label.setText((valid ? "✓ " : "✕ ") + text);
        label.setForeground(valid ? OK : ERROR);
    }
}
