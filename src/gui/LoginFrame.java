package gui;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
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

    private final AuthService authService;


    public LoginFrame(AuthService authService) {

        this.authService = authService;

        setTitle("Log In");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1100, 733);
        setLocationRelativeTo(null);
        setResizable(false);


        
        JPanel backgroundPanel = new JPanel() {

            @Override
            protected void paintComponent(Graphics g) {

                super.paintComponent(g);

                ImageIcon img = new ImageIcon(
                        getClass().getResource(
                                "/Gui_Images/landpage8.png"
                        )
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
        // USERNAME / EMAIL
        // Maximum: 36 characters
        // =========================

        username = new JTextField();
        username.setBounds(363,325,355,30);
        username.setFont(new Font("Fira Code",Font.PLAIN,15));
        username.setOpaque(false);
        username.setBackground(new Color(0, 0, 0, 0));
        username.setForeground(Color.BLACK);
        username.setBorder(BorderFactory.createEmptyBorder());
        username.setDocument(
                new LimitedDocument(36, false,"Username or email",this::clearPassword));

        backgroundPanel.add(username);


        // =========================
        // PASSWORD
        // Maximum: 16 characters
        // =========================

        pass = new JPasswordField();
        pass.setBounds(363,403,355,30);
        pass.setFont(new Font("Fira Code",Font.PLAIN,15));
        pass.setOpaque(false);
        pass.setBackground(new Color(0, 0, 0, 0));
        pass.setForeground(Color.BLACK);
        pass.setBorder(BorderFactory.createEmptyBorder());


        // Character limit
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

        showpass =new JCheckBox("Show Password");
        showpass.setBounds(363,438,150,20);
        showpass.setOpaque(false);
        showpass.setForeground(Color.BLACK);
        showpass.setFont(new Font( "Fira Code",Font.PLAIN,12));
        showpass.setFocusPainted(false);
        showpass.addActionListener(e -> {
                if (showpass.isSelected()) {

                pass.setEchoChar(
                        (char) 0
                );

        } else {

                pass.setEchoChar('•');
         }
        });

        backgroundPanel.add(showpass);

        logbtn =new RoundedButton("Log In",Color.BLACK,Color.WHITE);
        logbtn.setBounds(344,500,390,40);
        logbtn.setFont(new Font("Fira Code",Font.BOLD,18));
        logbtn.setBackground(Color.WHITE);
        logbtn.setHoverColor(new Color(220,220,220));
        logbtn.addActionListener(this);
        backgroundPanel.add(logbtn);



        signbtn =new OutlineButton("Sign Up",Color.BLACK,Color.BLACK);
        signbtn.setBounds(344,550,390,40);
        signbtn.setFont(new Font("Fira Code",Font.BOLD,18));
        signbtn.setBgColor(Color.DARK_GRAY);
        signbtn.addActionListener(this);
        backgroundPanel.add(signbtn);


        // =========================
        // FORGOT PASSWORD
        // Temporary only
        // =========================

        JLabel forgotPass =
                new JLabel(
                        "<html><u>Forgot Password?</u></html>"
                );

        forgotPass.setBounds(486,594,150,20);
        forgotPass.setForeground(Color.BLACK);
        forgotPass.setFont(new Font("Fira Code",Font.PLAIN,12));
        forgotPass.setCursor(new Cursor(Cursor.HAND_CURSOR));
        forgotPass.addMouseListener(
                new MouseAdapter() {
                    @Override
                    public void mouseClicked(
                            MouseEvent e) {

                        JOptionPane.showMessageDialog(
                                LoginFrame.this,
                                "Password reset is not available yet.",
                                "QueueTees",
                                JOptionPane.INFORMATION_MESSAGE
                        );
                    }


                    @Override
                    public void mouseEntered(
                            MouseEvent e) {

                        forgotPass.setForeground(
                                Color.GRAY
                        );
                    }


                    @Override
                    public void mouseExited(
                            MouseEvent e) {

                        forgotPass.setForeground(
                                Color.BLACK
                        );
                    }
                }
        );

        backgroundPanel.add(forgotPass);


        // Pressing ENTER logs in
        getRootPane().setDefaultButton(logbtn);
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

            new SignupFrame(
                    authService
            ).setVisible(true);
        }
    }


    // =========================
    // LOGIN
    // =========================

    private void handleLogin() {

        LoginResult result =
                authService.login(
                        username.getText(),
                        pass.getPassword()
                );


        // Password should disappear
        // after every login attempt
        clearPassword();


        // =========================
        // UNVERIFIED CUSTOMER
        // =========================

        if (result.getStatus()
                == AuthStatus.EMAIL_NOT_VERIFIED
                && result.getUser() != null) {

            dispose();

            new EmailAuthFrame(
                    authService,
                    result.getUser()
            ).setVisible(true);

            return;
        }


        // =========================
        // FAILED LOGIN
        // =========================

        if (!result.isSuccess()) {

            Toolkit
                    .getDefaultToolkit()
                    .beep();


            JOptionPane.showMessageDialog(
                    this,
                    result.getMessage(),
                    "Login Warning",
                    JOptionPane.WARNING_MESSAGE
            );

            return;
        }


        // =========================
        // SUCCESSFUL LOGIN
        // =========================

        openDashboard(
                result.getUser()
        );
    }


    // =========================
    // ROLE NAVIGATION
    // =========================

    private void openDashboard(User user) {

        if (user == null) {

            Toolkit
                    .getDefaultToolkit()
                    .beep();

            JOptionPane.showMessageDialog(
                    this,
                    "Unable to load account information.",
                    "Login Warning",
                    JOptionPane.WARNING_MESSAGE
            );

            return;
        }


        dispose();


        switch (user.getRole()) {

            case ADMIN:

                new AdminFrame(
                        authService
                ).setVisible(true);

                break;


            case STAFF:

                new StaffFrame(
                        authService
                ).setVisible(true);

                break;


            case CUSTOMER:

                new CustomerFrame(
                        authService
                ).setVisible(true);

                break;


            default:

                Toolkit
                        .getDefaultToolkit()
                        .beep();

                JOptionPane.showMessageDialog(
                        this,
                        "Unknown account role.",
                        "Login Warning",
                        JOptionPane.WARNING_MESSAGE
                );

                new LoginFrame(
                        authService
                ).setVisible(true);

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
    }
}