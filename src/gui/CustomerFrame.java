package gui;

import java.awt.*;
import javax.swing.*;
import service.AuthService;

public class CustomerFrame extends JFrame {

    public CustomerFrame(AuthService authService) {
        setTitle("Customer Dashboard");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1100, 733);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel panel = new JPanel(null) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                ImageIcon img = new ImageIcon(getClass().getResource("/Gui_Images/dummy.png"));
                g.drawImage(img.getImage(), 0, 0, getWidth(), getHeight(), this);
            }
        };
        setContentPane(panel);

        JLabel label = new JLabel("CUSTOMER DASHBOARD", SwingConstants.CENTER);
        label.setBounds(300, 250, 500, 60);
        label.setFont(new Font("Fira Code", Font.BOLD, 30));
        panel.add(label);

        RoundedButton logout = new RoundedButton("Log Out", Color.BLACK, Color.WHITE);
        logout.setBounds(400, 350, 300, 42);
        logout.setHoverColor(Color.DARK_GRAY);
        logout.addActionListener(e -> {
            dispose();
            new LoginFrame(authService).setVisible(true);
        });
        panel.add(logout);
    }
}