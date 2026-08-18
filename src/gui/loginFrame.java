package gui;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;


public class LoginFrame extends JFrame implements ActionListener {

    RoundedButton logButton, signButton;
    JTextField username;
    JPasswordField pass;
    JCheckBox showPass;



    public LoginFrame() {
        setTitle("login and sign up");
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

        logButton = new RoundedButton("Log In", Color.WHITE, Color.BLACK);
        logButton.setBounds(334, 490, 390, 40);
        logButton.setFont(new Font("Fira Code", Font.BOLD, 18));
        logButton.setBackground(Color.WHITE);
        backgroundPanel.add(logButton);

        signButton = new RoundedButton("Sign Up",new Color(30, 30, 30), Color.WHITE);
        signButton.setBounds(334, 540, 390, 40);
        signButton.setFont(new Font("Fira Code", Font.BOLD, 18));
        backgroundPanel.add(signButton);

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

        

        showPass = new JCheckBox("Show Password");
        showPass.setBounds(342, 410, 150, 20);
        showPass.setOpaque(false);
        showPass.setForeground(Color.BLACK);
        showPass.setFont(new Font("Fira Code", Font.PLAIN, 12));
        showPass.setFocusPainted(false);
        showPass.addActionListener(e -> {
            if (showPass.isSelected()) {
                pass.setEchoChar((char) 0);
            } else {
                pass.setEchoChar('•');
            }
        });
        backgroundPanel.add(showPass);

    }

    @Override
    public void actionPerformed(ActionEvent e) {
        
    }
}
