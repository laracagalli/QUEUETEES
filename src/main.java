import gui.auth.LoginFrame;
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
                        gui.components.AppTheme.install();
                        if ("memory".equals(System.getProperty("queuetees.storage"))) {
                                new LoginFrame(new AuthService(createTestUsers())).setVisible(true);
                        } else {
                                database.DatabaseStartup.start(auth -> new LoginFrame(auth).setVisible(true),
                                        () -> createTestUsers().findAll().stream().filter(user -> java.util.Set.of("admin","staff","verifiedcustomer").contains(user.getUsername())).toList());
                        }
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
                                AccountStatus.ACTIVE));

                repository.save(new User(
                                2,
                                "unverifiedcustomer",
                                "hakutorreta@gmail.com",
                                PasswordUtil.hashPassword("Customer123!"),
                                UserRole.CUSTOMER,
                                false,
                                AccountStatus.ACTIVE));

                repository.save(new User(
                                3,
                                "pendingstaff",
                                "pendingstaff@queuetees.local",
                                PasswordUtil.hashPassword("Staff123!"),
                                UserRole.STAFF,
                                true,
                                AccountStatus.PENDING_APPROVAL));

                repository.save(new User(
                                4,
                                "staff",
                                "staff@queuetees.local",
                                PasswordUtil.hashPassword("Staff123!"),
                                UserRole.STAFF,
                                true,
                                AccountStatus.ACTIVE));

                repository.save(new User(
                                5,
                                "admin",
                                "admin@queuetees.local",
                                PasswordUtil.hashPassword("Admin123!"),
                                UserRole.ADMIN,
                                true,
                                AccountStatus.ACTIVE));

                return repository;
        }
}
