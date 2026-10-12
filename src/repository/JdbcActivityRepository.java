package repository;

import database.Database;
import java.sql.*;
import java.util.*;
import model.User;
import model.UserRole;
import service.ActivityLogger;

public final class JdbcActivityRepository {
    private final Database database;
    public JdbcActivityRepository(Database database) { this.database = database; }
    public void record(User user, ActivityLogger.Entry entry) { database.transaction(c -> { record(c,user,entry.action(),entry.details()); return null; }); }
    public static void record(Connection c, User user, String action, String details) throws SQLException {
        if (user == null || (user.getRole() != UserRole.ADMIN && user.getRole() != UserRole.STAFF)) return;
        try (PreparedStatement s = c.prepareStatement("INSERT INTO activity_logs (user_id,username,role,action,details,occurred_at) VALUES (?,?,?,?,?,?)")) {
            s.setInt(1,user.getId()); s.setString(2,user.getUsername()); s.setString(3,user.getRole().name());
            s.setString(4,action); s.setString(5,details); s.setObject(6,java.time.LocalDateTime.now()); s.executeUpdate();
        }
    }
    public List<ActivityLogger.Entry> entries() { return database.read(c -> {
        List<ActivityLogger.Entry> result = new ArrayList<>();
        try (Statement s = c.createStatement(); ResultSet r = s.executeQuery("SELECT occurred_at,username,role,action,details,user_id FROM activity_logs ORDER BY occurred_at DESC,id DESC")) {
            while (r.next()) result.add(new ActivityLogger.Entry(r.getObject(1,java.time.LocalDateTime.class),r.getString(2),r.getString(3),r.getString(4),r.getString(5),r.getInt(6)));
        }
        return List.copyOf(result);
    }); }
}
