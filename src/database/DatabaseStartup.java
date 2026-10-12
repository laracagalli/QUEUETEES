package database;

import java.nio.file.*;
import java.util.*;
import javax.swing.*;
import model.*;
import repository.*;
import service.*;

/** Connect quietly before showing login, using private local credentials. */
public final class DatabaseStartup {
    private DatabaseStartup() { }
    public static void start(java.util.function.Consumer<AuthService> ready,java.util.function.Supplier<java.util.List<User>> defaults) {
        Properties properties = new Properties();
        try {
            try (var stream = Files.newInputStream(Path.of("database/application.properties"))) { properties.load(stream); }
            Path local = Path.of("database/database.local.properties");
            if (Files.exists(local)) try (var stream = Files.newInputStream(local)) { properties.load(stream); }
        } catch (Exception ex) { JOptionPane.showMessageDialog(null,"Cannot read database/application.properties.","Database setup",JOptionPane.ERROR_MESSAGE);return; }
        String secret = System.getenv("QUEUETEES_DB_PASSWORD");
        if (secret == null) secret = properties.getProperty("password");
        if (secret == null) {
            JOptionPane.showMessageDialog(null,
                    "Set your MySQL password in database/database.local.properties (password=...) and restart QueueTees.",
                    "Database setup",JOptionPane.ERROR_MESSAGE);
            return;
        }
        connect(properties,secret.toCharArray(),ready,defaults);
    }
    private static void connect(Properties properties,char[] password,java.util.function.Consumer<AuthService> ready,java.util.function.Supplier<java.util.List<User>> defaults) {
        new SwingWorker<AuthService,Void>() {
            protected AuthService doInBackground() {
                try {
                    Database db=new Database(properties.getProperty("url"),properties.getProperty("username"),password);
                    db.verifySchema();
                    JdbcUserRepository users=new JdbcUserRepository(db);
                    // Add defense accounts once; never reset an existing account's details/password.
                    for(User existing:defaults.get()) {
                        if (users.findByEmailOrUsername(existing.getUsername()).isEmpty()
                                && users.findByEmailOrUsername(existing.getEmail()).isEmpty()) {
                            User user=new User(0,existing.getUsername(),existing.getEmail(),existing.getPasswordHash(),existing.getRole(),existing.isEmailVerified(),existing.getStatus(),
                                    existing.getFullName(),existing.getAddress(),existing.getContactNumber(),existing.getGender(),existing.getBirthday(),existing.getRegisteredAt());
                            user.setProfilePicture(existing.getProfilePicture());users.save(user);
                        }
                    }
                    StoreService.getInstance().useDatabase(db);ActivityLogger.useDatabase(db);
                    return new AuthService(users);
                } finally { Arrays.fill(password,'\0'); }
            }
            protected void done() {
                try { ready.accept(get()); }
                catch(Exception ex) {
                    Throwable cause=ex.getCause()==null?ex:ex.getCause();
                    JOptionPane.showMessageDialog(null,cause.getMessage(),"MySQL connection failed",JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }
}
