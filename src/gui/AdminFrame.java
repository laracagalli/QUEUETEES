package gui;

import java.awt.*;
import javax.swing.*;
import service.AuthService;

public class AdminFrame extends JFrame {

    public AdminFrame(AuthService authService) {
        setTitle("Admin Dashboard");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1100, 733);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel panel = dashboardPanel("ADMIN DASHBOARD");
        setContentPane(panel);

        JButton logout = logoutButton(authService);
        panel.add(logout);
    }

    private JPanel dashboardPanel(String title) {
        JPanel panel = new JPanel(null) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                ImageIcon img = new ImageIcon(getClass().getResource("/Gui_Images/dummy.png"));
                g.drawImage(img.getImage(), 0, 0, getWidth(), getHeight(), this);
            }
        };
        JLabel label = new JLabel(title, SwingConstants.CENTER);
        label.setBounds(300, 250, 500, 60);
        label.setFont(new Font("Fira Code", Font.BOLD, 30));
        panel.add(label);
        return panel;
    }

    private JButton logoutButton(AuthService authService) {
        RoundedButton button = new RoundedButton("Log Out", Color.BLACK, Color.WHITE);
        button.setBounds(400, 350, 300, 42);
        button.setHoverColor(Color.DARK_GRAY);
        button.addActionListener(e -> {
            dispose();
            new LoginFrame(authService).setVisible(true);
        });
        return button;
    }
}