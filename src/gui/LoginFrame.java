package gui;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;


public class LoginFrame extends JFrame implements ActionListener {

    RoundedButton logButton, signButton;
    JTextField username;
    JPasswordField pass;



    public LoginFrame() {
        setTitle("login and sign up");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1100, 730);
        setLocationRelativeTo(null);

        // Background image panel
        JPanel backgroundPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                ImageIcon img = new ImageIcon(getClass().getResource("/Gui_Images/landingpage.png"));
                g.drawImage(img.getImage(), 0, 0, getWidth(), getHeight(), this);
            }
        };
        backgroundPanel.setLayout(null);
        setContentPane(backgroundPanel);

        logButton = new RoundedButton("Log In", new Color(30, 30, 30), Color.WHITE);
        logButton.setBounds(334, 490, 390, 40);
        logButton.setFont(new Font("Fira Code", Font.BOLD, 18));
        backgroundPanel.add(logButton);

        signButton = new RoundedButton("Sign Up",new Color(30, 30, 30), Color.WHITE);
        signButton.setBounds(334, 540, 390, 40);
        signButton.setFont(new Font("Fira Code", Font.BOLD, 18));
        backgroundPanel.add(signButton);
        
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        
    }
}
