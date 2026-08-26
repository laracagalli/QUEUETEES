package gui;

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
                java.net.URL imageUrl = getClass().getResource("/Gui_Images/emailauth.png");
                if (imageUrl != null) {
                    ImageIcon image = new ImageIcon(imageUrl);
                    g.drawImage(image.getImage(), 0, 0, getWidth(), getHeight(), this);
                }
            }
        };
        backgroundPanel.setLayout(null);
        setContentPane(backgroundPanel);

        int startX = 340;
        int y = 315;
        int fieldWidth = 54;
        int fieldHeight = 55;
        int gap = 10;

        for (int i = 0; i < codeFields.length; i++) {
            final int index = i;
            JTextField field = new JTextField();
            field.setBounds(startX + i * (fieldWidth + gap), y, fieldWidth, fieldHeight);
            field.setFont(new Font("Fira Code", Font.BOLD, 26));
            field.setHorizontalAlignment(JTextField.CENTER);
            field.setBackground(Color.WHITE);
            field.setForeground(Color.BLACK);
            field.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2, true));
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

        // JLabel emailLabel = new JLabel("Verification for: " + user.getEmail(), SwingConstants.CENTER);
        // emailLabel.setBounds(330, 430, 440, 25);
        // emailLabel.setForeground(Color.WHITE);
        // emailLabel.setFont(new Font("Fira Code", Font.PLAIN, 13));
        // backgroundPanel.add(emailLabel);

        // Temporary clickable HTML link. No email backend is used yet.
        JLabel resendCode = new JLabel("<html><u>Resend Code</u></html>", SwingConstants.CENTER);
        resendCode.setBounds(370, 445, 320, 30);
        resendCode.setForeground(Color.WHITE);
        resendCode.setFont(new Font("Fira Code", Font.PLAIN, 13));
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
                resendCode.setForeground(Color.WHITE);
            }
        });
        backgroundPanel.add(resendCode);

        RoundedButton enterButton = new RoundedButton("Enter", Color.WHITE, Color.BLACK);
        enterButton.setBounds(370, 500, 320, 45);
        enterButton.setFont(new Font("Fira Code", Font.BOLD, 17));
        enterButton.setHoverColor(new Color(210, 210, 210));
        enterButton.addActionListener(e -> verifyCode());
        backgroundPanel.add(enterButton);

        OutlineButton backButton = new OutlineButton("Back", Color.WHITE, Color.WHITE);
        backButton.setBounds(370, 555, 320, 42);
        backButton.setFont(new Font("Fira Code", Font.BOLD, 17));
        backButton.setBgColor(new Color(255, 255, 255, 35));
        backButton.addActionListener(e -> {
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

    private void generateTemporaryCode() {
        currentOtp = String.valueOf(100000 + random.nextInt(900000));

        JOptionPane.showMessageDialog(
                this,
                "TEMPORARY TEST PIN: " + currentOtp
                        + "\n\nEmail sending is not connected yet.",
                "QueueTees Test Mode",
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    private void verifyCode() {
        String enteredCode = getEnteredCode();

        if (enteredCode.length() != 6) {
            JOptionPane.showMessageDialog(
                    this,
                    "Please enter all six digits.",
                    "Verification",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        if (!enteredCode.equals(currentOtp)) {
            JOptionPane.showMessageDialog(
                    this,
                    "Incorrect verification code.",
                    "Verification",
                    JOptionPane.ERROR_MESSAGE
            );
            clearCodeFields();
            codeFields[0].requestFocusInWindow();
            return;
        }

        // Temporary in-memory update. This is lost when the program closes.
        authService.verifyEmail(user);

        JOptionPane.showMessageDialog(
                this,
                "Email verified successfully!",
                "QueueTees",
                JOptionPane.INFORMATION_MESSAGE
        );

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