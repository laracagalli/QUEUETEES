package gui.auth;

import gui.components.RoundedButton;
import gui.components.VerificationCodeInput;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import service.PasswordResetService;

/** Three-step email recovery screen, sharing the login window's fonts. */
public final class ForgotPasswordPanel extends JPanel {
    private static final Color GREEN = new Color(55, 70, 56), CREAM = new Color(241, 241, 232),
            PAPER = new Color(252, 252, 247), LINE = new Color(218, 220, 209);
    private final PasswordResetService service;
    private final JPanel form = new JPanel();
    private JPanel recoveryCard;
    private final JLabel message = text(" ", 12, false);
    private final JLabel[] steps = new JLabel[3];
    private final JTextField email = new JTextField();
    private final VerificationCodeInput code = new VerificationCodeInput(this::submit);
    private final JLabel countdown = text(" ", 12, false);
    private final Timer timer = new Timer(1000, e -> updateCountdown());
    private long expiresAt, resendAt;
    private boolean busy;
    private final JButton resend = link("Resend code");
    private final JPasswordField password = new JPasswordField(), confirm = new JPasswordField();
    private final JCheckBox showPasswordCheckBox = new JCheckBox("Show Password");
    private final RoundedButton action = button("Send code", true);
    private final JButton restartLink = link("Use another email");
    private final RoundedButton restartBtn = button("Use another email", true);
    private String token;
    private int stage;

    // Real-time password validation indicators
    private final JLabel upperCheck = text("✕ 1 uppercase letter", 10, false);
    private final JLabel specialCheck = text("✕ 1 special character", 10, false);
    private final JLabel lengthCheck = text("✕ 8–16 characters", 10, false);
    private final JLabel matchCheck = text("✕ Passwords match", 10, false);
    private final JPanel valPanel = new JPanel(new GridLayout(2, 2, 8, 4));

    public ForgotPasswordPanel(PasswordResetService service, Runnable back) {
        this.service = service;
        setLayout(new BorderLayout());
        setBackground(CREAM);

        // Checkbox setup for toggling password visibility
        showPasswordCheckBox.setOpaque(false);
        showPasswordCheckBox.setFont(new Font("Fira Code", Font.PLAIN, 11));
        showPasswordCheckBox.setForeground(GREEN);
        showPasswordCheckBox.setAlignmentX(CENTER_ALIGNMENT);
        showPasswordCheckBox.setFocusPainted(false);
        showPasswordCheckBox.addActionListener(e -> {
            char echoChar = showPasswordCheckBox.isSelected() ? (char) 0 : '•';
            password.setEchoChar(echoChar);
            confirm.setEchoChar(echoChar);
        });

        // Validation panel setup
        valPanel.setOpaque(false);
        valPanel.setMaximumSize(new Dimension(480, 44));
        valPanel.setAlignmentX(CENTER_ALIGNMENT);
        valPanel.add(lengthCheck);
        valPanel.add(upperCheck);
        valPanel.add(specialCheck);
        valPanel.add(matchCheck);

        // Listeners for real-time validation
        DocumentListener valListener = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                validatePasswordInputs();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                validatePasswordInputs();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                validatePasswordInputs();
            }
        };
        password.getDocument().addDocumentListener(valListener);
        confirm.getDocument().addDocumentListener(valListener);

        JPanel brandArea = new JPanel(new GridBagLayout());
        brandArea.setBackground(GREEN);
        brandArea.setPreferredSize(new Dimension(340, 0));
        JPanel brand = new JPanel();
        brand.setOpaque(false);
        brand.setLayout(new BoxLayout(brand, BoxLayout.Y_AXIS));
        java.net.URL logo = getClass().getResource("/Gui_Images/hirayalogo2.png");
        if (logo != null) {
            ImageIcon source = new ImageIcon(logo);
            JLabel image = new JLabel(new ImageIcon(source.getImage().getScaledInstance(245, 98, Image.SCALE_SMOOTH)));
            brand.add(image);
        }
        brand.add(Box.createVerticalStrut(12));
        brand.add(text("QUEUETEES", 22, true));
        brand.add(Box.createVerticalStrut(7));
        brand.add(text("QUEUE WITH EASE", 10, false));
        for (Component item : brand.getComponents())
            if (item instanceof JLabel) {
                item.setForeground(CREAM);
                ((JLabel) item).setAlignmentX(CENTER_ALIGNMENT);
            }
        brandArea.add(brand);
        add(brandArea, BorderLayout.WEST);
        JPanel formArea = new JPanel(new GridBagLayout());
        formArea.setOpaque(false);
        formArea.setBorder(new EmptyBorder(16, 24, 16, 24));
        JPanel card = new JPanel(new BorderLayout(0, 16)) {
            @Override
            protected void paintComponent(Graphics graphics) {
                Graphics2D draw = (Graphics2D) graphics.create();
                draw.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                draw.setColor(PAPER);
                draw.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 28, 28);
                draw.setColor(LINE);
                draw.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 28, 28);
                draw.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(40, 48, 40, 48));
        recoveryCard = card;
        card.setPreferredSize(new Dimension(620, 700));
        card.setMinimumSize(new Dimension(500, 700));
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setOpaque(false);
        JLabel eyebrow = text("ACCOUNT RECOVERY", 10, true);
        eyebrow.setAlignmentX(CENTER_ALIGNMENT);
        header.add(eyebrow);
        header.add(Box.createVerticalStrut(7));
        JLabel heading = text("Forgot password", 27, true);
        heading.setForeground(new Color(28, 31, 27));
        heading.setAlignmentX(CENTER_ALIGNMENT);
        header.add(heading);
        header.add(Box.createVerticalStrut(9));
        JLabel description = text("Reset your password to regain access.", 11, false);
        description.setAlignmentX(CENTER_ALIGNMENT);
        header.add(description);
        header.add(Box.createVerticalStrut(28));
        JPanel progress = new JPanel(new GridLayout(1, 3, 16, 0));
        progress.setOpaque(false);
        String[] names = { "1  Email", "2  Verify code", "3  Password" };
        for (int i = 0; i < 3; i++) {
            steps[i] = text(names[i], 10, true);
            steps[i].setHorizontalAlignment(SwingConstants.CENTER);
            steps[i].setOpaque(true);
            steps[i].setBorder(new EmptyBorder(12, 4, 12, 4));
            progress.add(steps[i]);
        }
        header.add(progress);
        card.add(header, BorderLayout.NORTH);
        form.setOpaque(false);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        card.add(form);
        gui.components.OutlineButton backButton = new gui.components.OutlineButton("Back to Login", GREEN, GREEN);
        backButton.setFont(new Font("Fira Code", Font.BOLD, 12));
        backButton.setBgColor(new Color(239, 243, 236));
        backButton.setPreferredSize(new Dimension(400, 48));
        backButton.addActionListener(e -> back.run());
        card.add(backButton, BorderLayout.SOUTH);
        formArea.add(card);
        add(formArea, BorderLayout.CENTER);
        code.setLayout(new FlowLayout(FlowLayout.CENTER, 8, 0));
        code.setAlignmentX(CENTER_ALIGNMENT);
        code.setMinimumSize(new Dimension(364, 58));
        code.setPreferredSize(new Dimension(364, 58));
        action.addActionListener(e -> submit());

        Runnable restartAction = () -> {
            timer.stop();
            stage = 0;
            token = null;
            email.setText("");
            code.clear();
            password.setText("");
            confirm.setText("");
            showPasswordCheckBox.setSelected(false);
            password.setEchoChar('•');
            confirm.setEchoChar('•');
            action.setEnabled(true);
            showStage();
            email.requestFocusInWindow();
        };
        restartLink.addActionListener(e -> restartAction.run());
        restartBtn.addActionListener(e -> restartAction.run());

        resend.addActionListener(e -> submitRequest(true));
        email.addActionListener(e -> submit());
        confirm.addActionListener(e -> submit());
        showStage();
    }

    private void showStage() {
        form.removeAll();
        message.setText(" ");
        recoveryCard.setPreferredSize(new Dimension(620, 700));
        recoveryCard.setMinimumSize(new Dimension(500, 700));
        for (int i = 0; i < 3; i++) {
            steps[i].setForeground(i == stage ? Color.WHITE : GREEN);
            steps[i].setBackground(i == stage ? GREEN : new Color(237, 240, 231));
        }
        if (stage == 0) {
            field("Enter your email address", email);
            note("We will email a six-digit verification code.");
            action.setText("Send code");
            action.setEnabled(true);
        } else if (stage == 1) {
            note("If this email has an account, a code has been sent.");
            note("Verification code");
            form.add(code);
            form.add(Box.createVerticalStrut(10));
            countdown.setAlignmentX(CENTER_ALIGNMENT);
            form.add(countdown);
            form.add(Box.createVerticalStrut(10));
            action.setText("Verify code");
        } else {
            field("New password", password);
            field("Confirm new password", confirm);
            form.add(showPasswordCheckBox);
            form.add(Box.createVerticalStrut(6));
            form.add(valPanel);
            form.add(Box.createVerticalStrut(10));
            action.setText("Reset password");
            validatePasswordInputs();
        }
        form.add(Box.createVerticalStrut(12));
        form.add(action);
        form.add(Box.createVerticalStrut(6));
        message.setAlignmentX(CENTER_ALIGNMENT);
        form.add(message);
        if (stage > 0) {
            if (stage < 2) {
                JPanel options = new JPanel(new GridLayout(1, 2, 16, 0));
                options.setOpaque(false);
                options.setPreferredSize(new Dimension(400, 48));
                options.setMinimumSize(new Dimension(100, 48));
                options.setAlignmentX(CENTER_ALIGNMENT);
                options.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
                options.add(resend);
                options.add(restartLink);
                resend.setToolTipText("Request a new verification code for this email.");
                form.add(Box.createVerticalStrut(6));
                form.add(options);
            } else {
                form.add(Box.createVerticalStrut(6));
                form.add(restartBtn);
            }
        }
        revalidate();
        repaint();
    }

    private void validatePasswordInputs() {
        if (stage != 2)
            return;

        String pwd = new String(password.getPassword());
        String chk = new String(confirm.getPassword());

        boolean hasText = !pwd.isEmpty() || !chk.isEmpty();
        if (valPanel.isVisible() != hasText) {
            valPanel.setVisible(hasText);
            form.revalidate();
            form.repaint();
        }

        boolean hasUpper = pwd.matches(".*[A-Z].*");
        boolean hasSpecial = pwd.matches(".*[^a-zA-Z0-9].*");
        boolean validLen = pwd.length() >= 8 && pwd.length() <= 16;
        boolean matches = !pwd.isEmpty() && pwd.equals(chk);

        updateCheckLabel(upperCheck, hasUpper, "1 uppercase letter");
        updateCheckLabel(specialCheck, hasSpecial, "1 special character");
        updateCheckLabel(lengthCheck, validLen, "8–16 characters");
        updateCheckLabel(matchCheck, matches, "Passwords match");

        if (!busy) {
            action.setEnabled(hasUpper && hasSpecial && validLen && matches);
        }
    }

    private void updateCheckLabel(JLabel label, boolean valid, String text) {
        if (valid) {
            label.setText("✓ " + text);
            label.setForeground(new Color(40, 135, 45));
        } else {
            label.setText("✕ " + text);
            label.setForeground(new Color(175, 50, 40));
        }
    }

    private void field(String title, JTextField field) {
        note(title);
        field.setFont(new Font("Fira Code", Font.PLAIN, 14));
        field.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(210, 215, 207)),
                new EmptyBorder(10, 16, 10, 16)));
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        field.setAlignmentX(CENTER_ALIGNMENT);
        field.setBackground(new Color(248, 248, 242));
        field.setForeground(new Color(28, 31, 27));
        field.setCaretColor(GREEN);
        form.add(field);
        form.add(Box.createVerticalStrut(10));
    }

    private void note(String value) {
        JLabel label = text(value, 11, false);
        label.setAlignmentX(CENTER_ALIGNMENT);
        form.add(label);
        form.add(Box.createVerticalStrut(4));
    }

    private void submit() {
        submitRequest(false);
    }

    private void submitRequest(boolean resending) {
        if (busy || (!resending && !action.isEnabled()))
            return;
        int submittedStage = resending ? 0 : stage;
        String address = email.getText(), enteredCode = code.getText().trim();
        char[] secret = password.getPassword(), confirmation = confirm.getPassword();
        busy = true;
        resend.setEnabled(false);
        action.setEnabled(false);
        restartLink.setEnabled(false);
        restartBtn.setEnabled(false);
        message.setForeground(GREEN);
        message.setText("Please wait…");
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                try {
                    if (submittedStage == 0)
                        token = service.request(address);
                    else if (submittedStage == 1)
                        service.verify(token, enteredCode);
                    else
                        service.reset(token, secret, confirmation);
                    return null;
                } finally {
                    java.util.Arrays.fill(secret, '\0');
                    java.util.Arrays.fill(confirmation, '\0');
                }
            }

            @Override
            protected void done() {
                busy = false;
                restartLink.setEnabled(true);
                restartBtn.setEnabled(true);
                try {
                    get();
                    password.setText("");
                    confirm.setText("");
                    if (submittedStage < 2) {
                        stage = submittedStage + 1;
                        showStage();
                        if (stage == 1) {
                            code.clear();
                            expiresAt = System.currentTimeMillis() + 600000;
                            resendAt = System.currentTimeMillis() + 60000;
                            updateCountdown();
                            if (isDisplayable())
                                timer.start();
                            code.focusFirst();
                        } else
                            timer.stop();
                    } else {
                        form.removeAll();
                        note("Your password has been reset.");
                        note("Return to login and use your new password.");
                        revalidate();
                        repaint();
                    }
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() == null ? ex : ex.getCause();
                    message.setForeground(new Color(150, 45, 35));
                    if (stage == 1)
                        updateCountdown();
                    else if (stage == 2)
                        validatePasswordInputs();
                    else
                        action.setEnabled(true);
                    message.setText(cause instanceof IllegalArgumentException ? cause.getMessage()
                            : "Could not send email. Check email configuration and try again.");
                }
            }
        }.execute();
    }

    private void updateCountdown() {
        long seconds = Math.max(0, (expiresAt - System.currentTimeMillis() + 999) / 1000);
        countdown.setText(seconds == 0 ? "Code expired. Request a new code."
                : String.format("Time remaining: %02d:%02d", seconds / 60, seconds % 60));
        long wait = Math.max(0, (resendAt - System.currentTimeMillis() + 999) / 1000);
        resend.setText(wait == 0 ? "Resend code" : "Resend code (" + wait + "s)");
        resend.setEnabled(!busy && wait == 0);
        if (!busy)
            action.setEnabled(seconds > 0);
    }

    @Override
    public void removeNotify() {
        timer.stop();
        super.removeNotify();
    }

    @Override
    public void addNotify() {
        super.addNotify();
        if (stage == 1) {
            updateCountdown();
            timer.start();
        }
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        g.setPaint(new GradientPaint(0, 0, CREAM, 0, getHeight(), new Color(145, 155, 145)));
        g.fillRect(0, 0, getWidth(), getHeight());
        g.dispose();
    }

    private static JButton link(String value) {
        JButton button = new JButton(value);
        button.setFont(new Font("Fira Code", Font.BOLD, 11).deriveFont(java.util.Collections.singletonMap(
                java.awt.font.TextAttribute.UNDERLINE, java.awt.font.TextAttribute.UNDERLINE_ON)));
        button.setForeground(GREEN);
        button.setOpaque(false);
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    private static JLabel text(String value, int size, boolean bold) {
        JLabel label = new JLabel(value);
        label.putClientProperty("html.disable", true);
        label.setFont(new Font("Fira Code", bold ? Font.BOLD : Font.PLAIN, size));
        label.setForeground(GREEN);
        return label;
    }

    private static RoundedButton button(String value, boolean dark) {
        RoundedButton button = new RoundedButton(value, dark ? new Color(28, 31, 27) : GREEN, Color.WHITE);
        button.setHoverColor(dark ? new Color(65, 70, 63) : new Color(74, 91, 74));
        button.setFont(new Font("Fira Code", Font.BOLD, 13));
        button.setPreferredSize(new Dimension(400, 48));
        button.setMinimumSize(new Dimension(100, 48));
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        button.setAlignmentX(CENTER_ALIGNMENT);
        return button;
    }
}