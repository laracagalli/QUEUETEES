package gui.staff;

import javax.swing.JFrame;
import gui.DashboardLayout;
import service.AuthService;

public class StaffFrame extends JFrame {
    public StaffFrame(AuthService authService) {
        setTitle("Staff Dashboard");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1500, 733);
        setLocationRelativeTo(null);
        setResizable(false);
        setContentPane(DashboardLayout.create(authService, "STAFF"));
    }
}
