package gui;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;


public class LoginFrame extends JFrame implements ActionListener {

    JButton logButton, signButton;
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
                ImageIcon img = new ImageIcon(getClass().getResource("/landingpage.png"));
                g.drawImage(img.getImage(), 0, 0, getWidth(), getHeight(), this);
            }
        };
        backgroundPanel.setLayout(new BorderLayout());
        setContentPane(backgroundPanel);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        
    }
}
