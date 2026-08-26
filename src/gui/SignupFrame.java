package gui;

import java.awt.*;
import java.awt.event.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import javax.swing.*;
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
    private final JTextField birthday;

    private final JPasswordField pass;
    private final JCheckBox showpass;

    private final JRadioButton female = new JRadioButton("Female");
    private final JRadioButton male = new JRadioButton("Male");
    private final JRadioButton other = new JRadioButton("Other");

    private final ButtonGroup gender = new ButtonGroup();

    private final AuthService authService;


    public SignupFrame(AuthService authService) {

        this.authService = authService;

        setTitle("Sign Up");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1100, 733);
        setLocationRelativeTo(null);
        setResizable(false);


        // =========================
        // BACKGROUND
        // =========================

        JPanel backgroundPanel = new JPanel() {

            @Override
            protected void paintComponent(Graphics g) {

                super.paintComponent(g);

                ImageIcon img = new ImageIcon(
                        getClass().getResource("/Gui_Images/signup4.png")
                );

                g.drawImage(
                        img.getImage(),
                        0,
                        0,
                        getWidth(),
                        getHeight(),
                        this
                );
            }
        };

        backgroundPanel.setLayout(null);
        setContentPane(backgroundPanel);


        // =========================
        // EMAIL
        // Maximum: 36 characters
        // =========================

        email = new JTextField();

        email.setBounds(33, 123, 390, 30);
        email.setFont(new Font("Fira Code", Font.PLAIN, 14));
        email.setOpaque(false);
        email.setBackground(new Color(0, 0, 0, 0));
        email.setBorder(BorderFactory.createEmptyBorder());

        email.setDocument(
                new LimitedDocument(
                        36,
                        false,
                        "Email",
                        this::clearPassword
                )
        );

        backgroundPanel.add(email);


        // =========================
        // FULL NAME
        // Maximum: 36 characters
        // =========================

        fullname = new JTextField();

        fullname.setBounds(33, 184, 390, 30);
        fullname.setFont(new Font("Fira Code", Font.PLAIN, 14));
        fullname.setOpaque(false);
        fullname.setBackground(new Color(0, 0, 0, 0));
        fullname.setBorder(BorderFactory.createEmptyBorder());

        fullname.setDocument(
                new LimitedDocument(
                        36,
                        false,
                        "Full name",
                        this::clearPassword
                )
        );

        backgroundPanel.add(fullname);


        // =========================
        // USERNAME
        // Maximum: 24 characters
        // =========================

        username = new JTextField();

        username.setBounds(33, 245, 390, 30);
        username.setFont(new Font("Fira Code", Font.PLAIN, 14));
        username.setOpaque(false);
        username.setBackground(new Color(0, 0, 0, 0));
        username.setBorder(BorderFactory.createEmptyBorder());

        username.setDocument(
                new LimitedDocument(
                        24,
                        false,
                        "Username",
                        this::clearPassword
                )
        );

        backgroundPanel.add(username);


        // =========================
        // PASSWORD
        // Maximum: 16 characters
        // =========================

        pass = new JPasswordField();

        pass.setBounds(33, 305, 390, 30);
        pass.setFont(new Font("Fira Code", Font.PLAIN, 14));
        pass.setOpaque(false);
        pass.setBackground(new Color(0, 0, 0, 0));
        pass.setBorder(BorderFactory.createEmptyBorder());

        pass.setDocument(
                new LimitedDocument(
                        16,
                        false,
                        "Password",
                        this::clearPassword
                )
        );

        backgroundPanel.add(pass);


        // =========================
        // SHOW PASSWORD
        // =========================

        showpass = new JCheckBox("Show Password");

        showpass.setBounds(33, 340, 150, 20);
        showpass.setOpaque(false);
        showpass.setForeground(Color.BLACK);
        showpass.setFocusPainted(false);
        showpass.setFont(
                new Font("Fira Code", Font.PLAIN, 12)
        );

        showpass.addActionListener(e -> {

            if (showpass.isSelected()) {

                pass.setEchoChar((char) 0);

            } else {

                pass.setEchoChar('*');
            }
        });

        backgroundPanel.add(showpass);


        // =========================
        // ADDRESS
        // Maximum: 36 characters
        // =========================

        address = new JTextField();

        address.setBounds(33, 385, 390, 30);
        address.setFont(new Font("Fira Code", Font.PLAIN, 14));
        address.setOpaque(false);
        address.setBackground(new Color(0, 0, 0, 0));
        address.setBorder(BorderFactory.createEmptyBorder());

        address.setDocument(
                new LimitedDocument(
                        36,
                        false,
                        "Address",
                        this::clearPassword
                )
        );

        backgroundPanel.add(address);


        // =========================
        // CONTACT NUMBER
        // Exactly up to 11 digits
        // Digits only
        // =========================

        contactnum = new JTextField();

        contactnum.setBounds(33, 452, 390, 30);
        contactnum.setFont(new Font("Fira Code", Font.PLAIN, 14));
        contactnum.setOpaque(false);
        contactnum.setBackground(new Color(0, 0, 0, 0));
        contactnum.setBorder(BorderFactory.createEmptyBorder());

        contactnum.setDocument(
                new LimitedDocument(
                        11,
                        true,
                        "Contact number",
                        this::clearPassword
                )
        );

        backgroundPanel.add(contactnum);


        // =========================
        // GENDER
        // =========================

        gender.add(female);
        gender.add(male);
        gender.add(other);


        female.setBounds(33, 550, 90, 25);
        female.setOpaque(false);
        female.setForeground(Color.BLACK);
        female.setFocusPainted(false);
        female.setFont(
                new Font("Fira Code", Font.PLAIN, 15)
        );


        male.setBounds(130, 550, 70, 25);
        male.setOpaque(false);
        male.setForeground(Color.BLACK);
        male.setFocusPainted(false);
        male.setFont(
                new Font("Fira Code", Font.PLAIN, 15)
        );


        other.setBounds(210, 550, 80, 25);
        other.setOpaque(false);
        other.setForeground(Color.BLACK);
        other.setFocusPainted(false);
        other.setFont(
                new Font("Fira Code", Font.PLAIN, 15)
        );


        backgroundPanel.add(female);
        backgroundPanel.add(male);
        backgroundPanel.add(other);


        // =========================
        // BIRTHDAY
        // YYYY-MM-DD
        // Maximum: 10 characters
        // =========================

        JLabel birthdayLabel =
                new JLabel("Birthday (YYYY-MM-DD):");

        birthdayLabel.setBounds(670, 185, 250, 25);
        birthdayLabel.setForeground(Color.WHITE);
        birthdayLabel.setFont(
                new Font("Fira Code", Font.BOLD, 15)
        );

        backgroundPanel.add(birthdayLabel);


        birthday = new JTextField();

        birthday.setBounds(33, 510, 300, 30);
        birthday.setFont(
                new Font("Fira Code", Font.PLAIN, 14)
        );

        birthday.setBackground(
                new Color(245, 245, 245)
        );

        birthday.setBorder(
                BorderFactory.createEmptyBorder(
                        5,
                        10,
                        5,
                        10
                )
        );

birthday.setOpaque(false);

        birthday.setBackground(
                new Color(0, 0, 0, 0)
        );


        birthday.setDocument(
                new LimitedDocument(
                        10,
                        false,
                        "Birthday",
                        this::clearPassword
                )
        );

        backgroundPanel.add(birthday);


        // =========================
        // SIGN UP BUTTON
        // =========================

        signupbtn =
                new RoundedButton(
                        "Sign Up",
                        Color.BLACK,
                        Color.WHITE
                );

        signupbtn.setBounds(
                19,
                595,
                420,
                40
        );

        signupbtn.setFont(
                new Font(
                        "Fira Code",
                        Font.BOLD,
                        18
                )
        );

        signupbtn.setHoverColor(
                Color.DARK_GRAY
        );

        signupbtn.addActionListener(this);

        backgroundPanel.add(signupbtn);


        // =========================
        // CANCEL BUTTON
        // =========================

        cancelbtn =
                new OutlineButton(
                        "Cancel",
                        Color.BLACK,
                        Color.BLACK
                );

        cancelbtn.setBounds(
                19,
                640,
                420,
                40
        );

        cancelbtn.setFont(
                new Font(
                        "Fira Code",
                        Font.BOLD,
                        18
                )
        );

        cancelbtn.setBgColor(
                new Color(0, 0, 0, 60)
        );

        cancelbtn.addActionListener(this);

        backgroundPanel.add(cancelbtn);


        // Press Enter = Sign Up
        getRootPane().setDefaultButton(signupbtn);
}


    // =========================
    // BUTTON EVENTS
    // =========================

        @Override
        public void actionPerformed(ActionEvent e) {

        if (e.getSource() == cancelbtn) {

        dispose();

                new LoginFrame(authService)
        .setVisible(true);

        return;
        }


        if (e.getSource() == signupbtn) {
                handleRegistration();
        }
        }


    // =========================
    // REGISTRATION
    // =========================

        private void handleRegistration() {

        LocalDate parsedBirthday = null;

        String birthdayText =
                birthday.getText().trim();


        // Birthday field empty
        if (birthdayText.isEmpty()) {

        showWarning(
                "Please enter your birthday."
        );

        return;
        }


        // Birthday format validation
        try {

        parsedBirthday =
                LocalDate.parse(birthdayText);

        } catch (DateTimeParseException ex) {

        showWarning(
                "Birthday must use YYYY-MM-DD.\n"
                        + "Example: 2005-08-20."
        );

        return;
        }


        // Send information to AuthService
        RegistrationResult result =
                authService.registerCustomer(

                        email.getText(),

                        fullname.getText(),

                        username.getText(),

                        pass.getPassword(),

                        address.getText(),

                        contactnum.getText(),

                        selectedGender(),

                        parsedBirthday
                );


        // Any registration error
        if (!result.isSuccess()) {

            showWarning(
                    result.getMessage()
            );

            return;
        }


        // Clear password after successful registration
        clearPassword();


        JOptionPane.showMessageDialog(
                this,
                "Account created successfully.\n"
                        + "Please verify your email to continue.",
                "QueueTees",
                JOptionPane.INFORMATION_MESSAGE
        );


        // Go to temporary Email Authentication
        dispose();

        new EmailAuthFrame(
                authService,
                result.getUser()
        ).setVisible(true);
}


    // =========================
    // GET SELECTED GENDER
    // =========================

    private String selectedGender() {

        if (female.isSelected()) {

            return "Female";
        }

        if (male.isSelected()) {

            return "Male";
        }

        if (other.isSelected()) {

            return "Other";
        }

        return "";
    }


    // =========================
    // WARNING
    // BEEP + CLEAR PASSWORD
    // =========================

    private void showWarning(String message) {

        Toolkit.getDefaultToolkit().beep();

        JOptionPane.showMessageDialog(
                this,
                message,
                "Registration Warning",
                JOptionPane.WARNING_MESSAGE
        );

        clearPassword();
    }


    // =========================
    // CLEAR PASSWORD
    // =========================

    private void clearPassword() {

        pass.setText("");

        showpass.setSelected(false);

        pass.setEchoChar('*');
    }
}