import gui.LoginFrame;
import model.AccountStatus;
import model.User;
import model.UserRole;
import repository.InMemoryUserRepository;
import repository.UserRepository;
import service.AuthService;
import service.PasswordUtil;

public class main {

    public static void main(String[] args) {

        javax.swing.SwingUtilities.invokeLater(() -> {

            UserRepository userRepository = createTestUsers();

            AuthService authService =
                    new AuthService(userRepository);

            LoginFrame frame =
                    new LoginFrame(authService);

            frame.setVisible(true);
        });
    }

    private static UserRepository createTestUsers() {

        InMemoryUserRepository repository =
                new InMemoryUserRepository();

        // VERIFIED CUSTOMER
        repository.save(new User(
                1,
                "verifiedcustomer",
                "verified@queuetees.local",
                PasswordUtil.hashPassword("Customer123!"),
                UserRole.CUSTOMER,
                true,
                AccountStatus.ACTIVE
        ));

        // UNVERIFIED CUSTOMER
        repository.save(new User(
                2,
                "unverifiedcustomer",
                "unverified@queuetees.local",
                PasswordUtil.hashPassword("Customer123!"),
                UserRole.CUSTOMER,
                false,
                AccountStatus.ACTIVE
        ));

        // STAFF WAITING FOR ADMIN APPROVAL
        repository.save(new User(
                3,
                "pendingstaff",
                "staff@queuetees.local",
                PasswordUtil.hashPassword("Staff123!"),
                UserRole.STAFF,
                true,
                AccountStatus.PENDING_APPROVAL
        ));

        // ADMIN
        repository.save(new User(
                4,
                "admin",
                "admin@queuetees.local",
                PasswordUtil.hashPassword("Admin123!"),
                UserRole.ADMIN,
                true,
                AccountStatus.ACTIVE
        ));

        return repository;
    }
}