package gui;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import service.AuthService;
import service.LoginResult;


public class LoginFrame extends JFrame implements ActionListener {

    RoundedButton logbtn;
    OutlineButton signbtn;
    JTextField username;
    JPasswordField pass;
    JCheckBox showpass;

private final AuthService authService;

    public LoginFrame(AuthService authService) {
        this.authService = authService;
        setTitle("Log In");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1100, 733);
        setLocationRelativeTo(null);
        setResizable(false);

        // Background image panel
        JPanel backgroundPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                ImageIcon img = new ImageIcon(getClass().getResource("/Gui_Images/landpage5.png"));
                g.drawImage(img.getImage(), 0, 0, getWidth(), getHeight(), this);
            }
        };
        backgroundPanel.setLayout(null);
        setContentPane(backgroundPanel);

        logbtn = new RoundedButton("Log In", Color.WHITE, Color.BLACK);
        logbtn.setBounds(334, 490, 390, 40);
        logbtn.setFont(new Font("Fira Code", Font.BOLD, 18));
        logbtn.setBackground(Color.WHITE);
        logbtn.addActionListener(this);
        backgroundPanel.add(logbtn);

        signbtn = new OutlineButton("Sign Up", Color.WHITE, Color.WHITE);
        signbtn.addActionListener(this);
        signbtn.setBounds(334, 540, 390, 40);
        signbtn.setFont(new Font("Fira Code", Font.BOLD, 18));
        signbtn.setBgColor(Color.DARK_GRAY);
        backgroundPanel.add(signbtn);

        JLabel forgotPass = new JLabel("<html><u>Forgot Password?</u></html>");
        forgotPass.setBounds(478, 590, 150, 20);
        forgotPass.setForeground(Color.WHITE);
        forgotPass.setFont(new Font("Fira Code", Font.PLAIN, 12));
        forgotPass.setCursor(new Cursor(Cursor.HAND_CURSOR));
        forgotPass.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                // handle forgot password here
                System.out.println("Forgot password clicked");
            }

            public void mouseEntered(java.awt.event.MouseEvent e) {
                forgotPass.setForeground(Color.LIGHT_GRAY);
            }
            public void mouseExited(java.awt.event.MouseEvent e) {
                forgotPass.setForeground(Color.WHITE);
            }
        });
        backgroundPanel.add(forgotPass);


        username = new JTextField();
        username.setBounds(342, 291, 370, 30);
        username.setFont(new Font("Fira Code", Font.PLAIN, 15));
        username.setOpaque(false);
        username.setBackground(new Color(0, 0, 0, 0));
        username.setForeground(Color.BLACK);
        username.setBorder(BorderFactory.createEmptyBorder());
        backgroundPanel.add(username);

        pass = new JPasswordField();
        pass.setBounds(342, 368, 370, 30);
        pass.setFont(new Font("Fira Code", Font.PLAIN, 15));
        pass.setOpaque(false);
        pass.setBackground(new Color(0, 0, 0, 0));
        pass.setForeground(Color.BLACK);
        pass.setBorder(BorderFactory.createEmptyBorder());
        backgroundPanel.add(pass);

        

        showpass = new JCheckBox("Show Password");
        showpass.setBounds(342, 410, 150, 20);
        showpass.setOpaque(false);
        showpass.setForeground(Color.BLACK);
        showpass.setFont(new Font("Fira Code", Font.PLAIN, 12));
        showpass.setFocusPainted(false);
        showpass.addActionListener(e -> {
            if (showpass.isSelected()) {
                pass.setEchoChar((char) 0);
            } else {
                pass.setEchoChar('•');
            }
        });
        backgroundPanel.add(showpass);

    }

    @Override
        public void actionPerformed(ActionEvent e) {

            if (e.getSource() == logbtn) {
                handleLogin();
            }

            else if (e.getSource() == signbtn) {
                new SignupFrame(authService).
                setVisible(true);
                this.dispose();
            }
        }

        private void handleLogin() {

            LoginResult result = authService.login(
                    username.getText(),
                    pass.getPassword()
            );

            if (!result.isSuccess()) {

                JOptionPane.showMessageDialog(this,result.getMessage(),"Login",JOptionPane.WARNING_MESSAGE);

                pass.setText("");
                return;
            }

            openDashboard();
        }

        private void openDashboard() {

            JOptionPane.showMessageDialog(this,"Login successful!\nDashboard is not created yet.","QueueTees",JOptionPane.INFORMATION_MESSAGE);
        }

        private void openRegistrationForm() {

                JOptionPane.showMessageDialog(this,"Registration form is not created yet.","QueueTees",JOptionPane.INFORMATION_MESSAGE);
            }
}
