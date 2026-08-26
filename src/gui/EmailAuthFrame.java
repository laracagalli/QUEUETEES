package gui;

import backend.EmailService;
import java.awt.*;
import java.awt.event.*;
import java.security.SecureRandom;
import javax.swing.*;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;
import model.User;
import service.AuthService;

public class EmailAuthFrame extends JFrame {

    private final AuthService authService;
    private final User user;
    private final JTextField[] codeFields = new JTextField[6];
    private final SecureRandom random = new SecureRandom();
    private String currentOtp;

    // Timer variables
    private JLabel timerLabel;
    private Timer countdownTimer;
    private int timeLeft = 300; // 5 minutes in seconds

    public EmailAuthFrame(AuthService authService, User user) {
        this.authService = authService;
        this.user = user;

        setTitle("Email Authentication");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1100, 733);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel backgroundPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                java.net.URL imageUrl = getClass().getResource("/Gui_Images/emailauth2.png");
                if (imageUrl != null) {
                    ImageIcon image = new ImageIcon(imageUrl);
                    g.drawImage(image.getImage(), 0, 0, getWidth(), getHeight(), this);
                }
            }
        };
        backgroundPanel.setLayout(null);
        setContentPane(backgroundPanel);

        int fieldWidth = 52;
        int fieldHeight = 55;
        int gap = 6;
        int totalWidth = 6 * fieldWidth + 5 * gap;
        int startX = (1100 - totalWidth) / 2 - 5;
        int y = 320;

        for (int i = 0; i < codeFields.length; i++) {
            final int index = i;
            JTextField field = new JTextField();
            field.setBounds(startX + i * (fieldWidth + gap), y, fieldWidth, fieldHeight);
            field.setFont(new Font("Fira Code", Font.BOLD, 26));
            field.setHorizontalAlignment(JTextField.CENTER);
            field.setBackground(Color.WHITE);
            field.setForeground(Color.BLACK);
            field.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2, true));
            field.setOpaque(true);
            field.setDocument(new OneDigitDocument());

            field.addKeyListener(new KeyAdapter() {
                @Override
                public void keyReleased(KeyEvent e) {
                    if (!field.getText().isEmpty() && index < codeFields.length - 1) {
                        codeFields[index + 1].requestFocusInWindow();
                    } else if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE
                            && field.getText().isEmpty() && index > 0) {
                        codeFields[index - 1].requestFocusInWindow();
                    }
                }
            });

            codeFields[i] = field;
            backgroundPanel.add(field);
        }

        // =========================
        // TIMER LABEL
        // =========================
        timerLabel = new JLabel("Time remaining: 05:00", SwingConstants.CENTER);
        // Moved up to y=280 to sit above the text fields
        timerLabel.setBounds(390, 280, 310, 20);
        timerLabel.setForeground(Color.BLACK);
        timerLabel.setFont(new Font("Fira Code", Font.PLAIN, 14));
        backgroundPanel.add(timerLabel);

        JLabel resendCode = new JLabel("<html><u>Resend Code</u></html>", SwingConstants.CENTER);
        resendCode.setBounds(390, 430, 310, 30);
        resendCode.setForeground(Color.BLACK);
        resendCode.setFont(new Font("Fira Code", Font.PLAIN, 15));
        resendCode.setCursor(new Cursor(Cursor.HAND_CURSOR));
        resendCode.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                generateTemporaryCode();
                clearCodeFields();
                codeFields[0].requestFocusInWindow();
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                resendCode.setForeground(Color.LIGHT_GRAY);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                resendCode.setForeground(Color.BLACK);
            }
        });
        backgroundPanel.add(resendCode);

        RoundedButton enterButton = new RoundedButton("Enter", Color.BLACK, Color.WHITE);
        enterButton.setBounds(360, 480, 360, 45);
        enterButton.setFont(new Font("Fira Code", Font.BOLD, 17));
        enterButton.setHoverColor(new Color(50, 50, 50));
        enterButton.addActionListener(e -> verifyCode());
        backgroundPanel.add(enterButton);

        OutlineButton backButton = new OutlineButton("Back", Color.BLACK, Color.BLACK);
        backButton.setBounds(360, 535, 360, 45);
        backButton.setFont(new Font("Fira Code", Font.BOLD, 17));
        backButton.setBgColor(new Color(0, 0, 0, 40));
        backButton.addActionListener(e -> {
            if (countdownTimer != null)
                countdownTimer.stop();
            dispose();
            new LoginFrame(authService).setVisible(true);
        });
        backgroundPanel.add(backButton);

        getRootPane().setDefaultButton(enterButton);

        SwingUtilities.invokeLater(() -> {
            generateTemporaryCode();
            codeFields[0].requestFocusInWindow();
        });
    }

    // =========================
    // TIMER METHODS
    // =========================
    private void startCountdown() {
        if (countdownTimer != null && countdownTimer.isRunning()) {
            countdownTimer.stop();
        }

        timeLeft = 300;
        updateTimerLabel();

        countdownTimer = new Timer(1000, e -> {
            timeLeft--;
            if (timeLeft <= 0) {
                countdownTimer.stop();
                timerLabel.setText("OTP Expired. Please resend.");
                timerLabel.setForeground(Color.RED);
                currentOtp = null; // Erase OTP so they can't force it through
            } else {
                updateTimerLabel();
            }
        });

        countdownTimer.start();
    }

    private void updateTimerLabel() {
        int minutes = timeLeft / 60;
        int seconds = timeLeft % 60;
        timerLabel.setText(String.format("Time remaining: %02d:%02d", minutes, seconds));
        timerLabel.setForeground(Color.BLACK);
    }

    private void generateTemporaryCode() {
        currentOtp = String.valueOf(100000 + random.nextInt(900000));

        JOptionPane.showMessageDialog(
                this,
                "Sending verification code to " + user.getEmail() + "...\nThis may take a few seconds.",
                "Email Verification",
                JOptionPane.INFORMATION_MESSAGE);

        new Thread(() -> {
            EmailService.sendOtpEmail(user.getEmail(), currentOtp);

            SwingUtilities.invokeLater(() -> {
                startCountdown(); // Start timer exactly when the email is sent
                JOptionPane.showMessageDialog(
                        this,
                        "OTP sent successfully! Please check your inbox.",
                        "Email Verification",
                        JOptionPane.INFORMATION_MESSAGE);
            });
        }).start();
    }

    private void verifyCode() {
        String enteredCode = getEnteredCode();

        if (currentOtp == null) {
            JOptionPane.showMessageDialog(
                    this,
                    "Your OTP has expired. Please click 'Resend Code'.",
                    "Verification",
                    JOptionPane.WARNING_MESSAGE);
            clearCodeFields();
            return;
        }

        if (enteredCode.length() != 6) {
            JOptionPane.showMessageDialog(
                    this,
                    "Please enter all six digits.",
                    "Verification",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (!enteredCode.equals(currentOtp)) {
            JOptionPane.showMessageDialog(
                    this,
                    "Incorrect verification code.",
                    "Verification",
                    JOptionPane.ERROR_MESSAGE);
            clearCodeFields();
            codeFields[0].requestFocusInWindow();
            return;
        }

        // Stop timer on success
        if (countdownTimer != null)
            countdownTimer.stop();

        authService.verifyEmail(user);

        JOptionPane.showMessageDialog(
                this,
                "Email verified successfully!",
                "QueueTees",
                JOptionPane.INFORMATION_MESSAGE);

        dispose();
        new CustomerFrame(authService).setVisible(true);
    }

    private String getEnteredCode() {
        StringBuilder builder = new StringBuilder();
        for (JTextField field : codeFields) {
            builder.append(field.getText().trim());
        }
        return builder.toString();
    }

    private void clearCodeFields() {
        for (JTextField field : codeFields) {
            field.setText("");
        }
    }

    private static class OneDigitDocument extends PlainDocument {
        @Override
        public void insertString(int offset, String text, AttributeSet attributes)
                throws BadLocationException {
            if (text == null || !text.matches("\\d") || getLength() >= 1) {
                Toolkit.getDefaultToolkit().beep();
                return;
            }
            super.insertString(offset, text, attributes);
        }
    }
}