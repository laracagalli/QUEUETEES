package gui.admin;

import javax.swing.JFrame;
import java.awt.Dimension;
import service.AuthService;

public class AdminFrame extends JFrame {
    public AdminFrame(AuthService authService) { this(authService,null); }
    public AdminFrame(AuthService authService, model.User user) {
        setTitle("Admin Dashboard");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1100, 700));
        setSize(1440, 820);
        setLocationRelativeTo(null);
        setResizable(true);
        setContentPane(new AdminDashboardPanel(authService,user));
    }
}
