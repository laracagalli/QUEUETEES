package gui.customer;

import javax.swing.JFrame;
import gui.DashboardLayout;
import service.AuthService;

public class CustomerFrame extends JFrame {
    public CustomerFrame(AuthService authService) {
        setTitle("Customer Dashboard");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(DashboardLayout.FRAME_WIDTH, DashboardLayout.FRAME_HEIGHT);
        setLocationRelativeTo(null);
        setResizable(false);
        setContentPane(DashboardLayout.create(authService, "CUSTOMER"));
    }
}
