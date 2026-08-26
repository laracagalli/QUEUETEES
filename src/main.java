import gui.LoginFrame;
import javax.swing.SwingUtilities;
import model.AccountStatus;
import model.User;
import model.UserRole;
import repository.InMemoryUserRepository;
import repository.UserRepository;
import service.AuthService;
import service.PasswordUtil;

public class main {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            UserRepository userRepository = createTestUsers();
            AuthService authService = new AuthService(userRepository);
            new LoginFrame(authService).setVisible(true);
        });
    }

    private static UserRepository createTestUsers() {
        InMemoryUserRepository repository = new InMemoryUserRepository();

        repository.save(new User(
                1,
                "verifiedcustomer",
                "verified@queuetees.local",
                PasswordUtil.hashPassword("Customer123!"),
                UserRole.CUSTOMER,
                true,
                AccountStatus.ACTIVE
        ));

        repository.save(new User(
                2,
                "unverifiedcustomer",
                "unverified@queuetees.local",
                PasswordUtil.hashPassword("Customer123!"),
                UserRole.CUSTOMER,
                false,
                AccountStatus.ACTIVE
        ));

        repository.save(new User(
                3,
                "pendingstaff",
                "pendingstaff@queuetees.local",
                PasswordUtil.hashPassword("Staff123!"),
                UserRole.STAFF,
                true,
                AccountStatus.PENDING_APPROVAL
        ));

        repository.save(new User(
                4,
                "staff",
                "staff@queuetees.local",
                PasswordUtil.hashPassword("Staff123!"),
                UserRole.STAFF,
                true,
                AccountStatus.ACTIVE
        ));

        repository.save(new User(
                5,
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