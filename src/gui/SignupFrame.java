package gui;

import java.awt.*;
import java.awt.event.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import com.toedter.calendar.JDateChooser;
import service.AuthService;
import service.RegistrationResult;

public class SignupFrame extends JFrame implements ActionListener {

    private final RoundedButton signupbtn;
    private final OutlineButton cancelbtn;

    private final JTextField email;
    private final JTextField fullname;
    private final JTextField username;
    private final JTextField address;
    private final JTextField contactnum;
    private final JDateChooser birthday;
    private final JPasswordField pass;
    private final JCheckBox showpass;

    private final JLabel emailError;
    private final JLabel fullnameError;
    private final JLabel passError;
    private final JLabel contactError;
    private final JLabel birthdayError;

    private final JRadioButton female = new JRadioButton("Female");
    private final JRadioButton male = new JRadioButton("Male");
    private final JRadioButton other = new JRadioButton("Other");
    private final ButtonGroup gender = new ButtonGroup();

    private final AuthService authService;

    private static final String[] SLIDE_IMAGES = {
        "/Gui_Images/model1.png",
        "/Gui_Images/model2.png",
        "/Gui_Images/model3.png"
    };

    private static final double[] SLIDE_CROP_BOTTOM = {
        0.0,
        0.13,
        0.0
    };

    private static final int[] SLIDE_OFFSET_Y = { 0, 0, 0 };

    private int currentSlide = 0;

    public SignupFrame(AuthService authService) {
        this.authService = authService;

        setTitle("Sign Up");
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
        leftPanel.setBounds(0, 0, 560, 733);
        leftPanel.setBackground(Color.WHITE);
        add(leftPanel);

        Font labelFont = new Font("Fira Code", Font.PLAIN, 12);
        Font fieldFont = new Font("Fira Code", Font.PLAIN, 13);
        Font errorFont = new Font("Fira Code", Font.PLAIN, 10);
        Color fieldBg = new Color(225, 225, 225);

        // Title
        JLabel title = new JLabel("Create Account");
        title.setBounds(30, 30, 400, 45);
        title.setFont(new Font("Arial Black", Font.BOLD, 30));
        leftPanel.add(title);

        JSeparator sep = new JSeparator();
        sep.setBounds(30, 78, 500, 2);
        sep.setForeground(Color.BLACK);
        leftPanel.add(sep);

        // --- ROW 1: Email | Full Name ---
        JLabel emailLbl = new JLabel("Email:");
        emailLbl.setBounds(30, 88, 200, 18);
        emailLbl.setFont(labelFont);
        leftPanel.add(emailLbl);

        email = new JTextField();
        email.setBounds(30, 106, 230, 36);
        email.setFont(fieldFont);
        email.setBackground(fieldBg);
        email.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        email.setDocument(new LimitedDocument(36, false, "Email", this::clearPassword));
        leftPanel.add(email);

        emailError = new JLabel("");
        emailError.setBounds(30, 143, 230, 14);
        emailError.setFont(errorFont);
        emailError.setForeground(Color.RED);
        emailError.setVisible(false);
        leftPanel.add(emailError);

        JLabel fullnameLbl = new JLabel("Full Name:");
        fullnameLbl.setBounds(290, 88, 200, 18);
        fullnameLbl.setFont(labelFont);
        leftPanel.add(fullnameLbl);

        fullname = new JTextField();
        fullname.setBounds(290, 106, 230, 36);
        fullname.setFont(fieldFont);
        fullname.setBackground(fieldBg);
        fullname.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        fullname.setDocument(new LimitedDocument(36, false, "Full name", this::clearPassword));
        leftPanel.add(fullname);

        fullnameError = new JLabel("");
        fullnameError.setBounds(290, 143, 230, 14);
        fullnameError.setFont(errorFont);
        fullnameError.setForeground(Color.RED);
        fullnameError.setVisible(false);
        leftPanel.add(fullnameError);

        // --- ROW 2: Username | Password ---
        JLabel usernameLbl = new JLabel("Username:");
        usernameLbl.setBounds(30, 165, 200, 18);
        usernameLbl.setFont(labelFont);
        leftPanel.add(usernameLbl);

        username = new JTextField();
        username.setBounds(30, 183, 230, 36);
        username.setFont(fieldFont);
        username.setBackground(fieldBg);
        username.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        username.setDocument(new LimitedDocument(24, false, "Username", this::clearPassword));
        leftPanel.add(username);

        JLabel passLbl = new JLabel("Password:");
        passLbl.setBounds(290, 165, 200, 18);
        passLbl.setFont(labelFont);
        leftPanel.add(passLbl);

        pass = new JPasswordField();
        pass.setBounds(290, 183, 230, 36);
        pass.setFont(fieldFont);
        pass.setBackground(fieldBg);
        pass.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        pass.setDocument(new LimitedDocument(16, false, "Password", this::clearPassword));
        leftPanel.add(pass);

        passError = new JLabel("");
        passError.setBounds(290, 220, 230, 14);
        passError.setFont(errorFont);
        passError.setForeground(Color.RED);
        passError.setVisible(false);
        leftPanel.add(passError);

        showpass = new JCheckBox("Show Password");
        showpass.setBounds(290, 233, 160, 20);
        showpass.setOpaque(false);
        showpass.setFont(new Font("Fira Code", Font.PLAIN, 11));
        showpass.setFocusPainted(false);
        showpass.addActionListener(e -> pass.setEchoChar(showpass.isSelected() ? (char) 0 : '*'));
        leftPanel.add(showpass);

        // --- ROW 3: Full Address ---
        JLabel addressLbl = new JLabel("Full Address:");
        addressLbl.setBounds(30, 262, 200, 18);
        addressLbl.setFont(labelFont);
        leftPanel.add(addressLbl);

        address = new JTextField();
        address.setBounds(30, 280, 490, 36);
        address.setFont(fieldFont);
        address.setBackground(fieldBg);
        address.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        address.setDocument(new LimitedDocument(60, false, "Address", this::clearPassword));
        leftPanel.add(address);

        // --- ROW 4: Contact Number | Gender ---
        JLabel contactLbl = new JLabel("Contact Number:");
        contactLbl.setBounds(30, 328, 200, 18);
        contactLbl.setFont(labelFont);
        leftPanel.add(contactLbl);

        contactnum = new JTextField();
        contactnum.setBounds(30, 346, 230, 36);
        contactnum.setFont(fieldFont);
        contactnum.setBackground(fieldBg);
        contactnum.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        contactnum.setDocument(new LimitedDocument(11, true, "Contact number", this::clearPassword));
        leftPanel.add(contactnum);

        contactError = new JLabel("");
        contactError.setBounds(30, 383, 230, 14);
        contactError.setFont(errorFont);
        contactError.setForeground(Color.RED);
        contactError.setVisible(false);
        leftPanel.add(contactError);

        JLabel genderLbl = new JLabel("Gender:");
        genderLbl.setBounds(290, 328, 100, 18);
        genderLbl.setFont(labelFont);
        leftPanel.add(genderLbl);

        gender.add(female);
        gender.add(male);
        gender.add(other);

        female.setBounds(290, 348, 80, 28);
        female.setOpaque(false);
        female.setFocusPainted(false);
        female.setFont(fieldFont);
        leftPanel.add(female);

        male.setBounds(375, 348, 65, 28);
        male.setOpaque(false);
        male.setFocusPainted(false);
        male.setFont(fieldFont);
        leftPanel.add(male);

        other.setBounds(445, 348, 70, 28);
        other.setOpaque(false);
        other.setFocusPainted(false);
        other.setFont(fieldFont);
        leftPanel.add(other);

        // --- ROW 5: Birthday ---
        JLabel birthdayLbl = new JLabel("Birthday:");
        birthdayLbl.setBounds(30, 400, 300, 18);
        birthdayLbl.setFont(labelFont);
        leftPanel.add(birthdayLbl);

        // Max selectable date = 18 years ago from today
        java.util.Calendar maxCal = java.util.Calendar.getInstance();
        maxCal.add(java.util.Calendar.YEAR, -18);

        birthday = new JDateChooser();
        birthday.setDateFormatString("MM/dd/yyyy");
        birthday.setMaxSelectableDate(maxCal.getTime());
        birthday.setBounds(30, 418, 490, 36);
        birthday.setFont(fieldFont);
        birthday.setBackground(fieldBg);
        leftPanel.add(birthday);

        birthdayError = new JLabel("");
        birthdayError.setBounds(30, 455, 490, 14);
        birthdayError.setFont(errorFont);
        birthdayError.setForeground(Color.RED);
        birthdayError.setVisible(false);
        leftPanel.add(birthdayError);

        JLabel ageHint = new JLabel("You must be at least 18 years old to register.");
        ageHint.setBounds(30, 470, 490, 16);
        ageHint.setFont(new Font("Fira Code", Font.ITALIC, 10));
        ageHint.setForeground(Color.GRAY);
        leftPanel.add(ageHint);

        JSeparator sep2 = new JSeparator();
        sep2.setBounds(30, 498, 500, 2);
        sep2.setForeground(Color.LIGHT_GRAY);
        leftPanel.add(sep2);

        // Buttons
        signupbtn = new RoundedButton("Sign Up", new Color(60, 60, 60), Color.WHITE);
        signupbtn.setBounds(30, 535, 500, 50);
        signupbtn.setFont(new Font("Arial Black", Font.BOLD, 16));
        signupbtn.setHoverColor(new Color(40, 40, 40));
        signupbtn.addActionListener(this);
        leftPanel.add(signupbtn);

        cancelbtn = new OutlineButton("Cancel", Color.BLACK, Color.BLACK);
        cancelbtn.setBounds(30, 600, 500, 50);
        cancelbtn.setFont(new Font("Arial Black", Font.BOLD, 16));
        cancelbtn.setBgColor(new Color(0, 0, 0, 30));
        cancelbtn.addActionListener(this);
        leftPanel.add(cancelbtn);

        // Live validation
        DocumentListener liveValidation = new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { checkSignupRealTime(); }
            @Override public void removeUpdate(DocumentEvent e) { checkSignupRealTime(); }
            @Override public void changedUpdate(DocumentEvent e) { checkSignupRealTime(); }
        };
        email.getDocument().addDocumentListener(liveValidation);
        fullname.getDocument().addDocumentListener(liveValidation);
        pass.getDocument().addDocumentListener(liveValidation);
        contactnum.getDocument().addDocumentListener(liveValidation);


        // =========================
        // RIGHT PANEL — gradient + wipe slideshow
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
        rightPanel.setBounds(560, 0, 540, 733);
        add(rightPanel);

        Image[] currentImg = {getSlideImage(0)};
        Image[] nextImg = {null};
        int[] wipeX = {0};
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

                if (currentImg[0] != null) {
                    Shape oldClip = g2.getClip();
                    g2.setClip(wipeX[0], 0, w - wipeX[0], h);
                    drawCentered(g2, currentImg[0], w, h, 1.0f);
                    g2.setClip(oldClip);
                }
                if (nextImg[0] != null && wiping[0]) {
                    Shape oldClip = g2.getClip();
                    g2.setClip(0, 0, wipeX[0], h);
                    drawCentered(g2, nextImg[0], w, h, 1.0f);
                    g2.setClip(oldClip);
                }
            }
        };
        slidePanel.setBounds(0, 140, 540, 593);
        slidePanel.setOpaque(false);
        rightPanel.add(slidePanel);

        // Logo
        JLabel logoLabel = new JLabel();
        logoLabel.setBounds(60, 20, 420, 110);
        logoLabel.setHorizontalAlignment(SwingConstants.CENTER);
        java.net.URL logoUrl = getClass().getResource("/Gui_Images/hirayalogo2.png");
        if (logoUrl != null) {
            ImageIcon logoIcon = new ImageIcon(logoUrl);
            Image logoScaled = logoIcon.getImage().getScaledInstance(300, 95, Image.SCALE_SMOOTH);
            logoLabel.setIcon(new ImageIcon(logoScaled));
        }
        rightPanel.add(logoLabel);
        rightPanel.setComponentZOrder(logoLabel, 0);

        // Wipe timer
        javax.swing.Timer[] wipeTimer = {null};
        wipeTimer[0] = new javax.swing.Timer(16, null);

        javax.swing.Timer slideTimer = new javax.swing.Timer(3500, e -> {
            if (wiping[0]) return;
            int nextSlide = (currentSlide + 1) % SLIDE_IMAGES.length;
            nextImg[0] = getSlideImage(nextSlide);
            wipeX[0] = 0;
            wiping[0] = true;

            wipeTimer[0].addActionListener(we -> {
                wipeX[0] += 20;
                if (wipeX[0] >= 540) {
                    wipeX[0] = 540;
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

        getRootPane().setDefaultButton(signupbtn);
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
        int y = panelH - scaledH;
        g2.drawImage(img, x, y, scaledW, scaledH, null);
    }


    // =========================
    // GET SLIDE IMAGE
    // =========================

    private Image getSlideImage(int index) {
        java.net.URL url = getClass().getResource(SLIDE_IMAGES[index]);
        if (url == null) return null;

        ImageIcon icon = new ImageIcon(url);
        int imgW = icon.getIconWidth();
        int imgH = icon.getIconHeight();

        double cropBottom = SLIDE_CROP_BOTTOM[index];
        int cropH = (int)(imgH * (1.0 - cropBottom));
        java.awt.image.BufferedImage buf = new java.awt.image.BufferedImage(imgW, cropH,
                java.awt.image.BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g2c = buf.createGraphics();
        g2c.drawImage(icon.getImage(), 0, 0, null);
        g2c.dispose();

        int targetH = (int)(593 * 0.95);
        int targetW = (int)((double) imgW / cropH * targetH);

        return buf.getScaledInstance(targetW, targetH, Image.SCALE_SMOOTH);
    }


    // =========================
    // BUTTON EVENTS
    // =========================

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == cancelbtn) {
            dispose();
            new LoginFrame(authService).setVisible(true);
            return;
        }
        if (e.getSource() == signupbtn) {
            handleRegistration();
        }
    }


    // =========================
    // LIVE VALIDATION
    // =========================

    private void checkSignupRealTime() {
        String emailTxt = email.getText().trim();
        String fullnameTxt = fullname.getText().trim();
        String passTxt = new String(pass.getPassword());
        String contactTxt = contactnum.getText().trim();

        if (!emailTxt.isEmpty() && !emailTxt.contains("@")) {
            emailError.setText("Invalid email (missing @)");
            emailError.setVisible(true);
        } else { emailError.setVisible(false); }

        if (!fullnameTxt.isEmpty() && !fullnameTxt.matches("^[a-zA-Z\\s]+$")) {
            fullnameError.setText("No special characters or numbers");
            fullnameError.setVisible(true);
        } else { fullnameError.setVisible(false); }

        if (passTxt.length() > 0 && passTxt.length() < 6) {
            passError.setText("Minimum 6 characters");
            passError.setVisible(true);
        } else { passError.setVisible(false); }

        if (contactTxt.length() > 0 && contactTxt.length() < 11) {
            contactError.setText("Must be 11 digits");
            contactError.setVisible(true);
        } else { contactError.setVisible(false); }

        birthdayError.setVisible(false); // JDateChooser handles this itself
    }


    // =========================
    // REGISTRATION
    // =========================

    private void handleRegistration() {
        if (birthday.getDate() == null) {
            showWarning("Please select your birthday.");
            return;
        }

        if (!fullname.getText().trim().matches("^[a-zA-Z\\s]+$")) {
            showWarning("Full name cannot contain numbers or special characters.");
            return;
        }

        java.util.Date selectedDate = birthday.getDate();
        LocalDate parsedBirthday = selectedDate.toInstant()
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDate();

        if (parsedBirthday.isAfter(LocalDate.of(2026, 12, 31))) {
            showWarning("Birthday cannot be later than 12/31/2026.");
            return;
        }

        RegistrationResult result = authService.registerCustomer(
                email.getText(),
                fullname.getText(),
                username.getText(),
                pass.getPassword(),
                address.getText(),
                contactnum.getText(),
                selectedGender(),
                parsedBirthday);

        if (!result.isSuccess()) {
            showWarning(result.getMessage());
            return;
        }

        clearPassword();

        JOptionPane.showMessageDialog(this,
                "Account created successfully.\nPlease verify your email to continue.",
                "QueueTees", JOptionPane.INFORMATION_MESSAGE);

        dispose();
        new EmailAuthFrame(authService, result.getUser()).setVisible(true);
    }


    private String selectedGender() {
        if (female.isSelected()) return "Female";
        if (male.isSelected()) return "Male";
        if (other.isSelected()) return "Other";
        return "";
    }

    private void showWarning(String message) {
        Toolkit.getDefaultToolkit().beep();
        JOptionPane.showMessageDialog(this, message, "Registration Warning",
                JOptionPane.WARNING_MESSAGE);
        clearPassword();
    }

    private void clearPassword() {
        pass.setText("");
        showpass.setSelected(false);
        pass.setEchoChar('*');
    }
}
