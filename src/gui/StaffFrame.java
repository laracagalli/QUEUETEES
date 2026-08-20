package gui;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import service.AuthService;
import service.LoginResult;
import javax.swing.text.PlainDocument;
public class StaffFrame extends JFrame implements ActionListener {
    
    StaffFrame (authService authService) {
        setTitle("Staff Panel");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1100, 733);
        setLocationRelativeTo(null);
        setResizable(false);

        // bg
        JPanel backgroundPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                ImageIcon img = new ImageIcon(getClass().getResource("/Gui_Images/dummy.png"));
                g.drawImage(img.getImage(), 0, 0, getWidth(), getHeight(), this);
            }
        };
        backgroundPanel.setLayout(null);
        setContentPane(backgroundPanel);



    }

    @Override
    public void actionPerformed(ActionEvent e) {
        
    }
}
