package gui.admin;

import javax.swing.JFrame;
import gui.DashboardLayout;
import service.AuthService;

public class AdminFrame extends JFrame {
    public AdminFrame(AuthService authService) {
        setTitle("Admin Dashboard");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1500, 733);
        setLocationRelativeTo(null);
        setResizable(false);
        setContentPane(DashboardLayout.create(authService, "ADMIN"));
    }
}
