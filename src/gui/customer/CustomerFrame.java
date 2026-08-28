package gui.customer;

import javax.swing.JFrame;
import service.AuthService;

public class CustomerFrame extends JFrame {
    public CustomerFrame(AuthService authService) {
        setTitle("Customer Dashboard");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1500, 733);
        setLocationRelativeTo(null);
        setResizable(false);
        setContentPane(new CustomerDashboardPanel(authService));
    }
}
