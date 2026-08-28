package gui;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import gui.admin.AdminFrame;
import gui.customer.CustomerFrame;
import gui.staff.StaffFrame;
import model.User;
import service.AuthService;
import service.AuthStatus;
import service.LoginResult;

public class LoginFrame extends JFrame implements ActionListener {

    private final RoundedButton logbtn;
    private final OutlineButton signbtn;
    private final JTextField username;
    private final JPasswordField pass;
    private final JCheckBox showpass;
    private final JLabel errorLabel;
    private final AuthService authService;

    // Slideshow images on right panel
    private static final String[] SLIDE_IMAGES = {
        "/Gui_Images/model1.png",
        "/Gui_Images/model2.png",
        "/Gui_Images/model3.png"
    };
    private int currentSlide = 0;

    public LoginFrame(AuthService authService) {
        this.authService = authService;

        setTitle("Log In");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1100, 733);
        setLocationRelativeTo(null);
        setResizable(false);
        setLayout(null);


        // =========================
        // LEFT PANEL — white form
        // =========================

        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(null);
        leftPanel.setBounds(0, 0, 460, 733);
        leftPanel.setBackground(Color.WHITE);
        add(leftPanel);


        // Title
        JLabel title = new JLabel("Log in");
        title.setBounds(40, 60, 300, 50);
        title.setFont(new Font("Arial Black", Font.BOLD, 36));
        title.setForeground(Color.BLACK);
        leftPanel.add(title);

        // Divider line
        JSeparator sep = new JSeparator();
        sep.setBounds(40, 115, 370, 2);
        sep.setForeground(Color.BLACK);
        leftPanel.add(sep);

        // Email label
        JLabel emailLabel = new JLabel("Email or Username:");
        emailLabel.setBounds(40, 135, 200, 20);
        emailLabel.setFont(new Font("Fira Code", Font.PLAIN, 13));
        leftPanel.add(emailLabel);

        // Username field
        username = new JTextField();
        username.setBounds(40, 158, 370, 38);
        username.setFont(new Font("Fira Code", Font.PLAIN, 14));
        username.setBackground(new Color(225, 225, 225));
        username.setBorder(BorderFactory.createEmptyBorder(5, 12, 5, 12));
        username.setDocument(new LimitedDocument(36, false, "Username or email", this::clearPassword));
        leftPanel.add(username);

        // Password label
        JLabel passLabel = new JLabel("Password:");
        passLabel.setBounds(40, 218, 200, 20);
        passLabel.setFont(new Font("Fira Code", Font.PLAIN, 13));
        leftPanel.add(passLabel);

        // Password field
        pass = new JPasswordField();
        pass.setBounds(40, 241, 370, 38);
        pass.setFont(new Font("Fira Code", Font.PLAIN, 14));
        pass.setBackground(new Color(225, 225, 225));
        pass.setBorder(BorderFactory.createEmptyBorder(5, 12, 5, 12));
        pass.setDocument(new LimitedDocument(16, false, "Password", this::clearPassword));
        leftPanel.add(pass);

        // Show password
        showpass = new JCheckBox("Show Password");
        showpass.setBounds(40, 285, 150, 22);
        showpass.setOpaque(false);
        showpass.setFont(new Font("Fira Code", Font.PLAIN, 12));
        showpass.setFocusPainted(false);
        showpass.addActionListener(e -> pass.setEchoChar(showpass.isSelected() ? (char) 0 : '•'));
        leftPanel.add(showpass);

        // Divider line 2
        JSeparator sep2 = new JSeparator();
        sep2.setBounds(40, 330, 370, 2);
        sep2.setForeground(Color.LIGHT_GRAY);
        leftPanel.add(sep2);

        // Error label
        errorLabel = new JLabel("Invalid username or password.");
        errorLabel.setBounds(40, 340, 370, 20);
        errorLabel.setFont(new Font("Fira Code", Font.PLAIN, 11));
        errorLabel.setForeground(Color.RED);
        errorLabel.setVisible(false);
        leftPanel.add(errorLabel);

        // Log in button
        logbtn = new RoundedButton("Sign in", new Color(60, 60, 60), Color.WHITE);
        logbtn.setBounds(40, 390, 370, 48);
        logbtn.setFont(new Font("Arial Black", Font.BOLD, 18));
        logbtn.setHoverColor(new Color(40, 40, 40));
        logbtn.addActionListener(this);
        leftPanel.add(logbtn);

        // Sign up button
        signbtn = new OutlineButton("Sign Up", Color.BLACK, Color.BLACK);
        signbtn.setBounds(40, 448, 370, 44);
        signbtn.setFont(new Font("Arial Black", Font.BOLD, 16));
        signbtn.setBgColor(new Color(0, 0, 0, 30));
        signbtn.addActionListener(this);
        leftPanel.add(signbtn);

        // Forgot password
        JLabel forgotPass = new JLabel("<html><u>Forgot Password?</u></html>", SwingConstants.CENTER);
        forgotPass.setBounds(40, 510, 370, 22);
        forgotPass.setFont(new Font("Fira Code", Font.PLAIN, 13));
        forgotPass.setForeground(Color.DARK_GRAY);
        forgotPass.setCursor(new Cursor(Cursor.HAND_CURSOR));
        forgotPass.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                JOptionPane.showMessageDialog(LoginFrame.this,
                    "Password reset is not available yet.", "QueueTees",
                    JOptionPane.INFORMATION_MESSAGE);
            }
            @Override public void mouseEntered(MouseEvent e) { forgotPass.setForeground(Color.GRAY); }
            @Override public void mouseExited(MouseEvent e) { forgotPass.setForeground(Color.DARK_GRAY); }
        });
        leftPanel.add(forgotPass);


        // =========================
        // RIGHT PANEL — gradient + slideshow
        // =========================

        // =========================
        // RIGHT PANEL — gradient + zoom slideshow
        // =========================

        JPanel rightPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gradient = new GradientPaint(
                    0, 0, new Color(0xF1, 0xF1, 0xE8),
                    0, getHeight(), new Color(0x91, 0x9B, 0x91)
                );
                g2.setPaint(gradient);
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        rightPanel.setLayout(null);
        rightPanel.setBounds(460, 0, 640, 733);
        add(rightPanel);

        // Zoom state
        Image[] currentImg = {getSlideImage(0)};
        Image[] nextImg = {null};
        int[] wipeX = {0};       // how many pixels of next image revealed (0 → panelW)
        boolean[] wiping = {false};

        JPanel slidePanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                setOpaque(false);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_BILINEAR);

                int w = getWidth();
                int h = getHeight();

                // Draw current image (right portion — being wiped away)
                if (currentImg[0] != null) {
                    Shape oldClip = g2.getClip();
                    g2.setClip(wipeX[0], 0, w - wipeX[0], h);
                    drawCentered(g2, currentImg[0], w, h, 1.0f);
                    g2.setClip(oldClip);
                }

                // Draw next image (left portion — being revealed)
                if (nextImg[0] != null && wiping[0]) {
                    Shape oldClip = g2.getClip();
                    g2.setClip(0, 0, wipeX[0], h);
                    drawCentered(g2, nextImg[0], w, h, 1.0f);
                    g2.setClip(oldClip);
                }
            }
        };
        slidePanel.setBounds(0, 140, 640, 593);
        slidePanel.setOpaque(false);
        rightPanel.add(slidePanel);

        // Logo on top
        JLabel logoLabel = new JLabel();
        logoLabel.setBounds(110, 20, 440, 140);
        logoLabel.setHorizontalAlignment(SwingConstants.CENTER);
        java.net.URL logoUrl = getClass().getResource("/Gui_Images/hirayalogo2.png");
        if (logoUrl != null) {
            ImageIcon logoIcon = new ImageIcon(logoUrl);
            Image logoScaled = logoIcon.getImage().getScaledInstance(320, 100, Image.SCALE_SMOOTH);
            logoLabel.setIcon(new ImageIcon(logoScaled));
        }
        rightPanel.add(logoLabel);
        rightPanel.setComponentZOrder(logoLabel, 0);

        // Wipe animation timer ~60fps
        javax.swing.Timer[] wipeTimer = {null};
        wipeTimer[0] = new javax.swing.Timer(16, null);

        // Slide switch timer — every 3.5 seconds start wipe
        javax.swing.Timer slideTimer = new javax.swing.Timer(3500, e -> {
            if (wiping[0]) return;
            int nextSlide = (currentSlide + 1) % SLIDE_IMAGES.length;
            nextImg[0] = getSlideImage(nextSlide);
            wipeX[0] = 0;
            wiping[0] = true;

            wipeTimer[0].addActionListener(we -> {
                wipeX[0] += 20; // speed of wipe — increase for faster
                if (wipeX[0] >= 640) {
                    wipeX[0] = 640;
                    currentImg[0] = nextImg[0];
                    nextImg[0] = null;
                    wiping[0] = false;
                    wipeX[0] = 0;
                    currentSlide = nextSlide;
                    for (ActionListener al : wipeTimer[0].getActionListeners()) {
                        wipeTimer[0].removeActionListener(al);
                    }
                }
                slidePanel.repaint();
            });
            wipeTimer[0].start();
        });
        slideTimer.start();

        getRootPane().setDefaultButton(logbtn);
    }


    // =========================
    // DRAW IMAGE CENTERED + BOTTOM ANCHORED
    // =========================

    private void drawCentered(Graphics2D g2, Image img, int panelW, int panelH, float scale) {
        int imgW = img.getWidth(null);
        int imgH = img.getHeight(null);
        if (imgW <= 0 || imgH <= 0) return;
        int scaledW = (int)(imgW * scale);
        int scaledH = (int)(imgH * scale);
        int x = (panelW - scaledW) / 2;
        int y = panelH - scaledH; // anchor to bottom
        g2.drawImage(img, x, y, scaledW, scaledH, null);
    }


    // =========================
    // GET SLIDE IMAGE (scaled + cropped)
    // =========================

    // Per-model crop: how much % of the bottom to cut off (0.0 = no crop, 0.2 = cut 20% from bottom)
    private static final double[] SLIDE_CROP_BOTTOM = {
        0.0,  // model1.png — cut 15% from bottom
        0.0,   // model2.png — no crop
        0.0    // model3.png — no crop
    };

    private Image getSlideImage(int index) {
        java.net.URL url = getClass().getResource(SLIDE_IMAGES[index]);
        if (url == null) return null;

        ImageIcon icon = new ImageIcon(url);
        int imgW = icon.getIconWidth();
        int imgH = icon.getIconHeight();

        // Crop bottom
        double cropBottom = SLIDE_CROP_BOTTOM[index];
        int cropH = (int)(imgH * (1.0 - cropBottom));
        java.awt.image.BufferedImage buf = new java.awt.image.BufferedImage(imgW, cropH,
                java.awt.image.BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g2c = buf.createGraphics();
        g2c.drawImage(icon.getImage(), 0, 0, null);
        g2c.dispose();

        // Scale to 80% of panel height
        int targetH = (int)(593 * 0.95);
        int targetW = (int)((double) imgW / cropH * targetH);

        return buf.getScaledInstance(targetW, targetH, Image.SCALE_SMOOTH);
    }


    // =========================
    // BUTTON EVENTS
    // =========================

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == logbtn) {
            handleLogin();
        } else if (e.getSource() == signbtn) {
            dispose();
            new SignupFrame(authService).setVisible(true);
        }
    }


    // =========================
    // LOGIN
    // =========================

    private void handleLogin() {
        LoginResult result = authService.login(username.getText(), pass.getPassword());
        clearPassword();

        if (result.getStatus() == AuthStatus.EMAIL_NOT_VERIFIED && result.getUser() != null) {
            dispose();
            new EmailAuthFrame(authService, result.getUser()).setVisible(true);
            return;
        }

        if (!result.isSuccess()) {
            Toolkit.getDefaultToolkit().beep();
            errorLabel.setVisible(true);
            JOptionPane.showMessageDialog(this, result.getMessage(), "Login Warning",
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        openDashboard(result.getUser());
    }


    // =========================
    // ROLE NAVIGATION
    // =========================

    private void openDashboard(User user) {
        if (user == null) {
            Toolkit.getDefaultToolkit().beep();
            JOptionPane.showMessageDialog(this, "Unable to load account information.",
                "Login Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        dispose();

        switch (user.getRole()) {
            case ADMIN:   new AdminFrame(authService).setVisible(true);    break;
            case STAFF:   new StaffFrame(authService).setVisible(true);    break;
            case CUSTOMER: new CustomerFrame(authService).setVisible(true); break;
            default:
                Toolkit.getDefaultToolkit().beep();
                JOptionPane.showMessageDialog(this, "Unknown account role.",
                    "Login Warning", JOptionPane.WARNING_MESSAGE);
                new LoginFrame(authService).setVisible(true);
                break;
        }
    }


    // =========================
    // CLEAR PASSWORD
    // =========================

    private void clearPassword() {
        pass.setText("");
        showpass.setSelected(false);
        pass.setEchoChar('•');
        errorLabel.setVisible(false);
    }
}
