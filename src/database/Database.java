package database;

import java.sql.*;
import java.util.Properties;

/** Short-lived JDBC connections. Transactions roll back before surfacing failures. */
public final class Database {
    @FunctionalInterface public interface Work<T> { T run(Connection connection) throws SQLException; }
    private final String url;
    private final Properties credentials = new Properties();
    public Database(String url, String username, char[] password) {
        this.url = url;
        credentials.setProperty("user", username);
        credentials.setProperty("password", new String(password));
        credentials.setProperty("connectTimeout", "5000");
        credentials.setProperty("socketTimeout", "15000");
        credentials.setProperty("connectionTimeZone", "+08:00");
        credentials.setProperty("forceConnectionTimeZoneToSession", "true");
    }
    public Connection open() throws SQLException { return DriverManager.getConnection(url, credentials); }
    public <T> T read(Work<T> work) {
        try (Connection connection = open()) { return work.run(connection); }
        catch (SQLException ex) { throw failure(ex); }
    }
    public <T> T transaction(Work<T> work) {
        try (Connection connection = open()) {
            connection.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            connection.setAutoCommit(false);
            try { T result = work.run(connection); connection.commit(); return result; }
            catch (SQLException | RuntimeException ex) { connection.rollback(); throw ex; }
        } catch (SQLException ex) { throw failure(ex); }
    }
    public void verifySchema() {
        read(c -> {
            for (String table : new String[]{"users", "products", "cart_items", "orders", "order_items", "activity_logs", "verification_challenges"})
                try (PreparedStatement s = c.prepareStatement("SELECT * FROM " + table + " LIMIT 0")) { s.executeQuery().close(); }
            return null;
        });
    }
    private static IllegalStateException failure(SQLException ex) {
        String message = switch (ex.getErrorCode()) {
            case 1045 -> "MySQL rejected the username or password. Check database/database.local.properties against your Workbench connection.";
            case 1049, 1146, 1054 -> "MySQL schema is missing or out of date. Run database/queuetees.sql in Workbench.";
            case 1062 -> "That username, email or record already exists.";
            default -> "The database operation failed. Check that MySQL is running and the connection is available (SQL state " + ex.getSQLState() + ").";
        };
        return new IllegalStateException(message, ex);
    }
}
