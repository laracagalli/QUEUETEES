package repository;

import database.Database;
import java.sql.*;
import java.util.*;
import model.*;

public final class JdbcUserRepository implements UserRepository {
    private final Database database;
    public JdbcUserRepository(Database database) { this.database = database; }
    @Override public int nextId() { return 0; } // MySQL assigns IDs when new users are inserted.
    @Override public Optional<User> findByEmailOrUsername(String identifier) {
        if (identifier == null) return Optional.empty();
        return database.read(c -> {
            try (PreparedStatement s = c.prepareStatement("SELECT * FROM users WHERE email=? OR username=? ORDER BY id LIMIT 1")) {
                s.setString(1, identifier.trim()); s.setString(2, identifier.trim());
                try (ResultSet r = s.executeQuery()) { return r.next() ? Optional.of(user(r)) : Optional.empty(); }
            }
        });
    }
    @Override public List<User> findAll() {
        return database.read(c -> {
            List<User> users = new ArrayList<>();
            try (Statement s = c.createStatement(); ResultSet r = s.executeQuery("SELECT * FROM users ORDER BY id")) {
                while (r.next()) users.add(user(r));
            }
            return users;
        });
    }
    @Override public void save(User user) {
        persist(user,null,null,null);
    }
    @Override public void saveWithActivity(User user,User actor,String action,String details) {
        persist(user,actor,action,details);
    }
    private void persist(User user,User actor,String action,String details) {
        int id = database.transaction(c -> {
            String values = "username=?,email=?,password_hash=?,role=?,email_verified=?,status=?,full_name=?,address=?,contact_number=?,gender=?,birthday=?,profile_picture=?";
            String sql = user.getId() == 0 ? "INSERT INTO users SET " + values + ",registered_at=?" : "UPDATE users SET " + values + " WHERE id=?";
            try (PreparedStatement s = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                s.setString(1, user.getUsername()); s.setString(2, user.getEmail()); s.setString(3, user.getPasswordHash());
                s.setString(4, user.getRole().name()); s.setBoolean(5, user.isEmailVerified()); s.setString(6, user.getStatus().name());
                s.setString(7, user.getFullName()); s.setString(8, user.getAddress()); s.setString(9, user.getContactNumber());
                s.setString(10, user.getGender()); s.setObject(11, user.getBirthday()); s.setString(12, user.getProfilePicture());
                if (user.getId() == 0) s.setObject(13, user.getRegisteredAt()); else s.setInt(13, user.getId());
                s.executeUpdate();
                if (action != null) JdbcActivityRepository.record(c,actor,action,details);
                if (user.getId() > 0) return user.getId();
                try (ResultSet keys = s.getGeneratedKeys()) { if (!keys.next()) throw new SQLException("Missing generated user ID"); return keys.getInt(1); }
            }
        });
        if (user.getId() == 0) user.assignDatabaseId(id);
    }
    private static User user(ResultSet r) throws SQLException {
        User user = new User(r.getInt("id"), r.getString("username"), r.getString("email"), r.getString("password_hash"),
                UserRole.valueOf(r.getString("role")), r.getBoolean("email_verified"), AccountStatus.valueOf(r.getString("status")),
                r.getString("full_name"), r.getString("address"), r.getString("contact_number"), r.getString("gender"),
                r.getObject("birthday", java.time.LocalDate.class), r.getObject("registered_at", java.time.LocalDateTime.class));
        user.setProfilePicture(r.getString("profile_picture")); return user;
    }
}
