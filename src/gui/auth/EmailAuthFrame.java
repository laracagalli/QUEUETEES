package gui.auth;

import gui.components.RoundedButton;
import gui.components.OutlineButton;

import backend.EmailService;
import java.awt.*;
import java.awt.event.*;
import java.security.SecureRandom;
import javax.swing.*;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;
import gui.customer.CustomerFrame;
import model.User;
import service.AuthService;

public class EmailAuthFrame extends JFrame {

    private final AuthService authService;
    private final User user;
    private final JTextField[] codeFields = new JTextField[6];
    private final SecureRandom random = new SecureRandom();
    private String currentOtp;
    private boolean sendingCode;
    @FunctionalInterface interface CodeSender { void send(String email, String code) throws Exception; }
    private final CodeSender codeSender;
    private JButton resendCode;
    private RoundedButton enterButton;
    private JTextArea deliveryHint;

    // Timer variables
    private JLabel timerLabel;
    private Timer countdownTimer;
    private int timeLeft = 300; // 5 minutes in seconds

    public EmailAuthFrame(AuthService authService, User user) {
        this(authService, user, EmailService::sendOtpEmail);
    }

    EmailAuthFrame(AuthService authService, User user, CodeSender codeSender) {
        this.authService = authService;
        this.user = user;
        this.codeSender = codeSender;

        setTitle("Email Authentication");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(900, 650));
        setSize(1100, 733);
        setLocationRelativeTo(null);
        setResizable(true);

        JPanel root = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setPaint(new GradientPaint(0, 0, CREAM, 0, getHeight(), SAGE));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        root.setOpaque(false);
        setContentPane(root);
        root.add(createBrandPanel(), BorderLayout.WEST);

        JPanel formArea = new JPanel(new GridBagLayout());
        formArea.setOpaque(false);
        formArea.setBorder(new javax.swing.border.EmptyBorder(24, 24, 24, 24));

        JPanel card = roundedPanel(PAPER, 28);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new javax.swing.border.EmptyBorder(32, 32, 32, 32));
        card.setPreferredSize(new Dimension(570, 480));
        card.setMinimumSize(new Dimension(440, 480));
        card.setMaximumSize(new Dimension(570, 480));

        JLabel eyebrow = label("EMAIL VERIFICATION", 10, Font.BOLD, FOREST);
        eyebrow.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(eyebrow);
        card.add(Box.createVerticalStrut(7));
        JLabel title = label("Check your inbox", 27, Font.BOLD, INK);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(title);
        card.add(Box.createVerticalStrut(9));
        JLabel description = label("Enter the six-digit code sent to", 11, Font.PLAIN, MUTED);
        description.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(description);
        card.add(Box.createVerticalStrut(3));
        JLabel email = label(user.getEmail(), 11, Font.BOLD, FOREST);
        email.setAlignmentX(Component.CENTER_ALIGNMENT);
        email.setHorizontalAlignment(SwingConstants.CENTER);
        email.setToolTipText(user.getEmail());
        card.add(email);
        card.add(Box.createVerticalStrut(25));

        timerLabel = label("Preparing your verification code...", 12, Font.PLAIN, MUTED);
        timerLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(timerLabel);
        card.add(Box.createVerticalStrut(12));

        JPanel codeRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        codeRow.setOpaque(false);
        codeRow.setAlignmentX(Component.CENTER_ALIGNMENT);
        codeRow.setPreferredSize(new Dimension(364, 58));
        codeRow.setMinimumSize(new Dimension(364, 58));
        codeRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 58));

        for (int i = 0; i < codeFields.length; i++) {
            final int index = i;
            JTextField field = new JTextField();
            field.setPreferredSize(new Dimension(54, 58));
            field.setFont(font(25, Font.BOLD));
            field.setHorizontalAlignment(JTextField.CENTER);
            field.setBackground(new Color(248, 248, 242));
            field.setForeground(INK);
            field.setCaretColor(FOREST);
            field.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(LINE, 2, true),
                    new javax.swing.border.EmptyBorder(4, 4, 4, 4)));
            field.setOpaque(true);
            field.setDocument(new OneDigitDocument());
            field.getAccessibleContext().setAccessibleName("Verification digit " + (i + 1));
            field.addFocusListener(new FocusAdapter() {
                private void border(Color color) {
                    field.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(color, 2, true),
                            new javax.swing.border.EmptyBorder(4, 4, 4, 4)));
                }
                @Override public void focusGained(FocusEvent e) { border(FOREST); }
                @Override public void focusLost(FocusEvent e) { border(LINE); }
            });

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
            codeRow.add(field);
        }
        card.add(codeRow);
        card.add(Box.createVerticalStrut(16));

        resendCode = new JButton("Resend verification code");
        resendCode.setFont(font(11, Font.BOLD).deriveFont(java.util.Collections.singletonMap(
                java.awt.font.TextAttribute.UNDERLINE, java.awt.font.TextAttribute.UNDERLINE_ON)));
        resendCode.setForeground(FOREST);
        resendCode.setOpaque(false);
        resendCode.setContentAreaFilled(false);
        resendCode.setBorderPainted(false);
        resendCode.setFocusPainted(false);
        resendCode.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        resendCode.setAlignmentX(Component.CENTER_ALIGNMENT);
        resendCode.addActionListener(e -> {
            generateTemporaryCode();
            clearCodeFields();
            codeFields[0].requestFocusInWindow();
        });
        card.add(resendCode);
        card.add(Box.createVerticalStrut(8));

        deliveryHint = new JTextArea("Your code is valid for five minutes. Check your spam folder if it has not arrived.");
        deliveryHint.setEditable(false);
        deliveryHint.setFocusable(false);
        deliveryHint.setOpaque(false);
        deliveryHint.setLineWrap(true);
        deliveryHint.setWrapStyleWord(true);
        deliveryHint.setFont(font(11, Font.PLAIN));
        deliveryHint.setForeground(MUTED);
        deliveryHint.setAlignmentX(Component.CENTER_ALIGNMENT);
        deliveryHint.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        deliveryHint.setPreferredSize(new Dimension(440, 46));
        card.add(deliveryHint);
        card.add(Box.createVerticalGlue());
        card.add(Box.createVerticalStrut(14));

        enterButton = new RoundedButton("Verify Email", INK, Color.WHITE);
        enterButton.setFont(font(13, Font.BOLD));
        enterButton.setHoverColor(new Color(74, 91, 74));
        enterButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        enterButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        enterButton.setPreferredSize(new Dimension(480, 44));
        enterButton.addActionListener(e -> verifyCode());
        card.add(enterButton);
        card.add(Box.createVerticalStrut(10));

        OutlineButton backButton = new OutlineButton("Back to Login", FOREST, FOREST);
        backButton.setFont(font(12, Font.BOLD));
        backButton.setBgColor(new Color(145, 155, 145, 75));
        backButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        backButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        backButton.setPreferredSize(new Dimension(480, 44));
        backButton.addActionListener(e -> {
            if (countdownTimer != null)
                countdownTimer.stop();
            dispose();
            new LoginFrame(authService).setVisible(true);
        });
        card.add(backButton);
        GridBagConstraints placement = new GridBagConstraints();
        placement.fill = GridBagConstraints.HORIZONTAL;
        placement.weightx = 1;
        JPanel cardHolder = new JPanel(new GridBagLayout());
        cardHolder.setOpaque(false);
        cardHolder.setPreferredSize(new Dimension(570, 480));
        cardHolder.add(card, placement);
        formArea.add(cardHolder);
        root.add(formArea);

        getRootPane().setDefaultButton(enterButton);
        setCodeEntryEnabled(false);

        addWindowListener(new WindowAdapter() {
            @Override public void windowClosed(WindowEvent e) {
                if (countdownTimer != null) countdownTimer.stop();
            }
        });

        SwingUtilities.invokeLater(() -> {
            if (!isDisplayable()) return;
            generateTemporaryCode();
            codeFields[0].requestFocusInWindow();
        });
    }

    private JPanel createBrandPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(FOREST);
        panel.setPreferredSize(new Dimension(300, 0));
        JPanel brand = new JPanel();
        brand.setOpaque(false);
        brand.setLayout(new BoxLayout(brand, BoxLayout.Y_AXIS));
        java.net.URL logoUrl = getClass().getResource("/Gui_Images/hirayalogo2.png");
        JLabel logo = new JLabel();
        if (logoUrl != null) {
            Image image = new ImageIcon(logoUrl).getImage().getScaledInstance(245, 98, Image.SCALE_SMOOTH);
            logo.setIcon(new ImageIcon(image));
        }
        logo.setAlignmentX(Component.CENTER_ALIGNMENT);
        brand.add(logo);
        brand.add(Box.createVerticalStrut(12));
        JLabel name = label("QUEUETEES", 22, Font.BOLD, Color.WHITE);
        name.setAlignmentX(Component.CENTER_ALIGNMENT);
        brand.add(name);
        brand.add(Box.createVerticalStrut(7));
        JLabel tagline = label("QUEUE WITH EASE", 10, Font.PLAIN, new Color(221, 230, 216));
        tagline.setAlignmentX(Component.CENTER_ALIGNMENT);
        brand.add(tagline);
        panel.add(brand);
        return panel;
    }

    private static JPanel roundedPanel(Color color, int radius) {
        JPanel panel = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(color);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);
                g2.setColor(LINE);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        panel.setOpaque(false);
        return panel;
    }

    private static Font font(int size, int style) { return new Font("Fira Code", style, size); }

    private static JLabel label(String text, int size, int style, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(font(size, style));
        label.setForeground(color);
        return label;
    }

    private static final Color INK = new Color(28, 31, 27);
    private static final Color MUTED = new Color(99, 106, 96);
    private static final Color FOREST = new Color(55, 70, 56);
    private static final Color SAGE = new Color(145, 155, 145);
    private static final Color CREAM = new Color(241, 241, 232);
    private static final Color PAPER = new Color(252, 252, 247);
    private static final Color LINE = new Color(218, 220, 209);

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
                timerLabel.setForeground(new Color(164, 56, 56));
                currentOtp = null; // Erase OTP so they can't force it through
                setCodeEntryEnabled(false);
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
        timerLabel.setForeground(MUTED);
    }

    private void generateTemporaryCode() {
        if (sendingCode) return;
        sendingCode = true;
        currentOtp = null;
        if (countdownTimer != null) countdownTimer.stop();
        setCodeEntryEnabled(false);
        resendCode.setEnabled(false);
        String candidate = String.valueOf(100000 + random.nextInt(900000));
        timerLabel.setText("Sending verification code...");
        timerLabel.setForeground(MUTED);
        deliveryHint.setText("Please wait while we send a new code to your email.");
        deliveryHint.setForeground(MUTED);
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws Exception {
                codeSender.send(user.getEmail(), candidate);
                return null;
            }
            @Override protected void done() {
                sendingCode = false;
                if (!isDisplayable()) return;
                resendCode.setEnabled(true);
                try {
                    get(); currentOtp = candidate; startCountdown();
                    setCodeEntryEnabled(true);
                    resendCode.setText("Resend verification code");
                    deliveryHint.setText("Your code is valid for five minutes. Check your spam folder if it has not arrived.");
                    codeFields[0].requestFocusInWindow();
                } catch (Exception ex) {
                    timerLabel.setText("We could not send your code");
                    timerLabel.setForeground(new Color(164, 56, 56));
                    deliveryHint.setText(EmailService.deliveryFailureMessage(ex));
                    deliveryHint.setForeground(new Color(164, 56, 56));
                    resendCode.setText("Try sending again");
                }
            }
        }.execute();
    }

    private void verifyCode() {
        if (sendingCode) return;
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
        new CustomerFrame(authService, user).setVisible(true);
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

    private void setCodeEntryEnabled(boolean enabled) {
        enterButton.setEnabled(enabled);
        for (JTextField field : codeFields) field.setEnabled(enabled);
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
