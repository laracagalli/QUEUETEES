package gui.staff;

import javax.swing.JFrame;
import java.awt.Dimension;
import service.AuthService;

public class StaffFrame extends JFrame {
    public StaffFrame(AuthService authService, model.User user) {
        setTitle("Staff Dashboard");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1100, 700));
        setSize(1440, 820);
        setLocationRelativeTo(null);
        setResizable(true);
        setContentPane(new StaffDashboardPanel(authService, user));
    }
}
