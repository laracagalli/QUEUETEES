package gui.customer;

import javax.swing.JFrame;
import java.awt.Dimension;
import model.User;
import service.AuthService;

public class CustomerFrame extends JFrame {
    public CustomerFrame(AuthService authService, User user) {
        setTitle("Customer Dashboard");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1100, 700));
        setSize(1440, 820);
        setLocationRelativeTo(null);
        setResizable(true);
        setContentPane(new CustomerDashboardPanel(authService, user));
    }
}
