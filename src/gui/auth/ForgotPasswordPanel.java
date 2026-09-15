package gui.auth;

import gui.components.RoundedButton;
import gui.components.VerificationCodeInput;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import service.PasswordResetService;

/** Three-step email recovery screen, sharing the login window's fonts. */
public final class ForgotPasswordPanel extends JPanel {
    private static final Color GREEN = new Color(55, 70, 56), CREAM = new Color(246, 246, 239);
    private final PasswordResetService service;
    private final JPanel form = new JPanel();
    private final JLabel message = text(" ", 12, false);
    private final JLabel[] steps = new JLabel[3];
    private final JTextField email = new JTextField();
    private final VerificationCodeInput code = new VerificationCodeInput(this::submit);
    private final JLabel countdown = text(" ", 12, false);
    private final Timer timer = new Timer(1000, e -> updateCountdown());
    private long expiresAt, resendAt;
    private boolean busy;
    private final RoundedButton resend = button("Resend code", false);
    private final JPasswordField password = new JPasswordField(), confirm = new JPasswordField();
    private final RoundedButton action = button("Send code", true), restart = button("Use another email", false);
    private String token;
    private int stage;
    public ForgotPasswordPanel(PasswordResetService service, Runnable back) {
        this.service = service;
        setLayout(new GridBagLayout()); setBackground(GREEN); setBorder(new EmptyBorder(24, 24, 24, 24));
        JPanel brand = new JPanel(); brand.setOpaque(false); brand.setLayout(new BoxLayout(brand, BoxLayout.Y_AXIS));
        java.net.URL logo = getClass().getResource("/Gui_Images/hirayalogo2.png");
        if (logo != null) {
            ImageIcon source = new ImageIcon(logo);
            JLabel image = new JLabel(new ImageIcon(source.getImage().getScaledInstance(220, 90, Image.SCALE_SMOOTH)));
            brand.add(image);
        }
        brand.add(Box.createVerticalStrut(16)); brand.add(text("QUEUETEES", 15, true));
        brand.add(Box.createVerticalStrut(8)); brand.add(text("A Queuing Management System", 11, false));
        for (Component item : brand.getComponents()) if (item instanceof JLabel) item.setForeground(CREAM);
        GridBagConstraints g = new GridBagConstraints(); g.gridx = 0; g.insets = new Insets(0, 0, 0, 32); g.anchor = GridBagConstraints.NORTH;
        add(brand, g);
        JPanel card = new JPanel(new BorderLayout(0, 20)) {
            @Override public Dimension getPreferredSize() {
                Dimension size = super.getPreferredSize();
                return new Dimension(Math.max(670, size.width), size.height);
            }
            @Override protected void paintComponent(Graphics graphics) {
                Graphics2D draw = (Graphics2D) graphics.create();
                draw.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                draw.setColor(Color.WHITE); draw.fillRoundRect(0, 0, getWidth()-1, getHeight()-1, 28, 28); draw.dispose();
            }
        };
        card.setOpaque(false); card.setBorder(new EmptyBorder(32, 32, 28, 32));
        JPanel header = new JPanel(new GridLayout(0, 1, 0, 12)); header.setOpaque(false);
        JLabel heading = text("Forgot Password", 30, true); heading.setFont(new Font("Arial Black", Font.BOLD, 30));
        header.add(heading); header.add(text("Reset your password to regain access to your account.", 12, false));
        JPanel progress = new JPanel(new GridLayout(1, 3, 10, 0)); progress.setOpaque(false);
        String[] names = {"1  Enter email", "2  Verify code", "3  New password"};
        for (int i = 0; i < 3; i++) { steps[i] = text(names[i], 12, true); progress.add(steps[i]); }
        header.add(progress); card.add(header, BorderLayout.NORTH);
        form.setOpaque(false); form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS)); card.add(form);
        RoundedButton backButton = button("Back to login", false); backButton.addActionListener(e -> back.run());
        card.add(backButton, BorderLayout.SOUTH);
        g.gridx = 1; g.weightx = 1; g.fill = GridBagConstraints.HORIZONTAL; g.insets = new Insets(0, 0, 0, 0); add(card, g);
        action.addActionListener(e -> submit()); restart.addActionListener(e -> { timer.stop(); stage = 0; token = null; email.setText(""); code.clear(); password.setText(""); confirm.setText(""); action.setEnabled(true); showStage(); email.requestFocusInWindow(); });
        resend.addActionListener(e -> submitRequest(true));
        email.addActionListener(e -> submit()); confirm.addActionListener(e -> submit());
        showStage();
    }
    private void showStage() {
        form.removeAll(); message.setText(" ");
        for (int i=0; i<3; i++) steps[i].setForeground(i == stage ? GREEN : Color.GRAY);
        if (stage == 0) {
            field("Enter your email address", email);
            note("We will email a six-digit verification code."); action.setText("Send code");
        } else if (stage == 1) {
            note("If this email has an account, a code has been sent.");
            note("Verification code"); form.add(code); form.add(Box.createVerticalStrut(12));
            countdown.setAlignmentX(LEFT_ALIGNMENT); form.add(countdown);
            form.add(Box.createVerticalStrut(12)); action.setText("Verify code");
        } else {
            field("New password", password); field("Confirm new password", confirm);
            note("8–16 characters: uppercase, lowercase, number and symbol."); action.setText("Reset password");
        }
        form.add(Box.createVerticalStrut(12)); form.add(action); form.add(Box.createVerticalStrut(12));
        message.setAlignmentX(LEFT_ALIGNMENT); form.add(message);
        {
            JPanel options = new JPanel(new GridLayout(1, stage < 2 ? 2 : 1, 12, 0)); options.setOpaque(false);
            options.setPreferredSize(new Dimension(400, 46)); options.setMinimumSize(new Dimension(100, 46));
            options.setAlignmentX(LEFT_ALIGNMENT); options.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
            if (stage < 2) options.add(resend); options.add(restart);
            if (stage == 0) {
                resend.setText("Resend code");
                resend.setEnabled(false);
                resend.setToolTipText("Send a code first. You can resend it from the verification step.");
            } else {
                resend.setToolTipText("Request a new verification code for this email.");
            }
            form.add(Box.createVerticalStrut(12)); form.add(options);
        }
        revalidate(); repaint();
    }
    private void field(String title, JTextField field) {
        note(title); field.setFont(new Font("Fira Code", Font.PLAIN, 14));
        field.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(210,215,207)), new EmptyBorder(10,12,10,12)));
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44)); field.setAlignmentX(LEFT_ALIGNMENT);
        form.add(field); form.add(Box.createVerticalStrut(12));
    }
    private void note(String value) { JLabel label = text(value, 12, false); label.setAlignmentX(LEFT_ALIGNMENT); form.add(label); form.add(Box.createVerticalStrut(12)); }
    private void submit() { submitRequest(false); }
    private void submitRequest(boolean resending) {
        if (busy || (!resending && !action.isEnabled())) return;
        int submittedStage = resending ? 0 : stage;
        String address = email.getText(), enteredCode = code.getText().trim();
        char[] secret = password.getPassword(), confirmation = confirm.getPassword();
        busy = true; resend.setEnabled(false); action.setEnabled(false); restart.setEnabled(false); message.setForeground(GREEN); message.setText("Please wait…");
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws Exception {
                try {
                    if (submittedStage == 0) token = service.request(address);
                    else if (submittedStage == 1) service.verify(token, enteredCode);
                    else service.reset(token, secret, confirmation);
                    return null;
                } finally { java.util.Arrays.fill(secret, '\0'); java.util.Arrays.fill(confirmation, '\0'); }
            }
            @Override protected void done() {
                busy = false; action.setEnabled(true); restart.setEnabled(true);
                try {
                    get(); password.setText(""); confirm.setText("");
                    if (submittedStage < 2) {
                        stage = submittedStage + 1; showStage();
                        if (stage == 1) { code.clear(); expiresAt = System.currentTimeMillis() + 600000; resendAt = System.currentTimeMillis() + 60000; updateCountdown(); if (isDisplayable()) timer.start(); code.focusFirst(); }
                        else timer.stop();
                    }
                    else {
                        form.removeAll(); note("Your password has been reset."); note("Return to login and use your new password.");
                        revalidate(); repaint();
                    }
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() == null ? ex : ex.getCause();
                    message.setForeground(new Color(150, 45, 35));
                    if (stage == 1) updateCountdown();
                    message.setText(cause instanceof IllegalArgumentException ? cause.getMessage() : "Could not send email. Check email configuration and try again.");
                }
            }
        }.execute();
    }
    private void updateCountdown() {
        long seconds = Math.max(0, (expiresAt - System.currentTimeMillis() + 999) / 1000);
        countdown.setText(seconds == 0 ? "Code expired. Request a new code." : String.format("Time remaining: %02d:%02d", seconds/60, seconds%60));
        long wait = Math.max(0, (resendAt - System.currentTimeMillis() + 999) / 1000);
        resend.setText(wait == 0 ? "Resend code" : "Resend code (" + wait + "s)");
        resend.setEnabled(!busy && wait == 0);
        if (!busy) action.setEnabled(seconds > 0);
    }
    @Override public void removeNotify() { timer.stop(); super.removeNotify(); }
    @Override public void addNotify() { super.addNotify(); if(stage == 1) { updateCountdown(); timer.start(); } }
    private static JLabel text(String value, int size, boolean bold) {
        JLabel label = new JLabel(value); label.putClientProperty("html.disable", true);
        label.setFont(new Font("Fira Code", bold ? Font.BOLD : Font.PLAIN, size)); label.setForeground(GREEN); return label;
    }
    private static RoundedButton button(String value, boolean dark) {
        RoundedButton button = new RoundedButton(value, dark ? new Color(28,31,27) : GREEN, Color.WHITE);
        button.setHoverColor(dark ? new Color(65,70,63) : new Color(74,91,74));
        button.setFont(new Font("Fira Code", Font.BOLD, 13)); button.setPreferredSize(new Dimension(400,46));
        button.setMinimumSize(new Dimension(100,46)); button.setMaximumSize(new Dimension(Integer.MAX_VALUE,46)); button.setAlignmentX(LEFT_ALIGNMENT); return button;
    }
}
