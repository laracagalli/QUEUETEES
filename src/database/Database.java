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
            // Additive migration for existing installations; never replace existing orders.
            ensureColumn(c,"users","terms_version","VARCHAR(30) NOT NULL DEFAULT ''");
            ensureColumn(c,"users","terms_accepted_at","DATETIME(6) NULL");
            ensureColumn(c,"users","terms_required","BOOLEAN NOT NULL DEFAULT FALSE");
            for (String[] column : new String[][]{{"address_province","VARCHAR(120)"},{"address_city","VARCHAR(120)"},
                    {"address_barangay","VARCHAR(160)"},{"address_street","VARCHAR(100)"}}) {
                boolean exists;
                try(ResultSet r=c.getMetaData().getColumns(c.getCatalog(),null,"orders",column[0])) { exists=r.next(); }
                if(!exists) try(Statement s=c.createStatement()) { s.execute("ALTER TABLE orders ADD COLUMN " + column[0] + " " + column[1] + " NULL"); }
            }
            if(ensureColumn(c,"products","size","VARCHAR(20) NOT NULL DEFAULT 'M'"))try(Statement s=c.createStatement()){s.executeUpdate("UPDATE products SET size='One Size' WHERE category='Accessories'");}
            if(ensureColumn(c,"order_items","product_size","VARCHAR(20) NOT NULL DEFAULT 'M'"))try(Statement s=c.createStatement()){s.executeUpdate("UPDATE order_items SET product_size='One Size' WHERE product_category='Accessories'");}
            ensureColumn(c,"orders","control_number","VARCHAR(20) NULL");
            ensureColumn(c,"orders","payment_status","VARCHAR(30) NOT NULL DEFAULT 'PENDING'");
            ensureColumn(c,"orders","paid_amount","DECIMAL(12,2) NOT NULL DEFAULT 0");
            ensureColumn(c,"orders","payment_reference","VARCHAR(100) NULL");
            boolean paymentIndexed=false;
            try(ResultSet r=c.getMetaData().getIndexInfo(c.getCatalog(),null,"orders",true,false)){while(r.next())paymentIndexed|="uq_orders_payment_reference".equals(r.getString("INDEX_NAME"));}
            if(!paymentIndexed)try(Statement s=c.createStatement()){s.execute("ALTER TABLE orders ADD UNIQUE KEY uq_orders_payment_reference(payment_reference)");}
            ensureColumn(c,"orders","courier_reference","VARCHAR(100) NULL");
            ensureColumn(c,"orders","courier_confirmed_at","DATETIME(6) NULL");
            ensureColumn(c,"orders","delivered_by","INT NULL");
            ensureColumn(c,"orders","received_by","INT NULL");
            ensureColumn(c,"orders","delivery_issue","VARCHAR(1000) NOT NULL DEFAULT ''");
            ensureColumn(c,"orders","delivery_issue_status","VARCHAR(30) NOT NULL DEFAULT ''");
            ensureColumn(c,"orders","delivery_issue_response","TEXT NULL");
            ensureColumn(c,"orders","delivery_issue_reviewer","INT NULL");
            try(Statement s=c.createStatement()){s.executeUpdate("UPDATE orders SET delivery_issue_status='OPEN' WHERE delivery_issue<>'' AND delivery_issue_status='' AND status<>'COMPLETED'");}
            try(Statement s=c.createStatement()) {
                s.executeUpdate("UPDATE orders SET control_number=CONCAT('QT-',HEX(RANDOM_BYTES(6))) WHERE control_number IS NULL OR control_number=''");
            }
            boolean indexed=false;
            try(ResultSet r=c.getMetaData().getIndexInfo(c.getCatalog(),null,"orders",true,false)) { while(r.next())indexed|="uq_orders_control".equals(r.getString("INDEX_NAME")); }
            if(!indexed)try(Statement s=c.createStatement()){s.execute("ALTER TABLE orders ADD UNIQUE KEY uq_orders_control(control_number)");}
            try(Statement s=c.createStatement()) {
                s.execute("CREATE TABLE IF NOT EXISTS return_requests (id INT AUTO_INCREMENT PRIMARY KEY, order_id INT NOT NULL, customer_id INT NOT NULL, product_id INT NOT NULL, quantity INT NOT NULL, reason TEXT NOT NULL, proof LONGBLOB NOT NULL, proof_name VARCHAR(255) NOT NULL, status VARCHAR(30) NOT NULL DEFAULT 'SUBMITTED', requested_at DATETIME(6) NOT NULL, reviewed_at DATETIME(6) NULL, reviewer_id INT NULL, resolution TEXT NOT NULL, refund_reference VARCHAR(100) NOT NULL DEFAULT '', refund_amount DECIMAL(12,2) NOT NULL DEFAULT 0, FOREIGN KEY(order_id) REFERENCES orders(id), FOREIGN KEY(customer_id) REFERENCES users(id)) ENGINE=InnoDB");
                s.execute("CREATE TABLE IF NOT EXISTS security_incidents (id INT AUTO_INCREMENT PRIMARY KEY, user_id INT NULL, username VARCHAR(100) NOT NULL, reason TEXT NOT NULL, reported_at DATETIME(6) NOT NULL, resolved BOOLEAN NOT NULL DEFAULT FALSE, resolution TEXT NOT NULL, reviewer_id INT NULL) ENGINE=InnoDB");
            }
            ensureColumn(c,"security_incidents","warning_sent","BOOLEAN NOT NULL DEFAULT FALSE");
            ensureColumn(c,"security_incidents","warning_read","BOOLEAN NOT NULL DEFAULT FALSE");
            ensureColumn(c,"security_incidents","warning_sent_at","DATETIME(6) NULL");
            ensureColumn(c,"security_incidents","warning_message","TEXT NULL");
            ensureColumn(c,"security_incidents","warning_popup_seen","BOOLEAN NOT NULL DEFAULT FALSE");
            try(Statement s=c.createStatement();ResultSet r=s.executeQuery("SHOW COLUMNS FROM orders LIKE 'status'")) {
                if(r.next() && (!r.getString("Type").contains("TRANSFERRING") || !r.getString("Type").contains("LOST")))
                    try(Statement migration=c.createStatement()) { migration.execute("ALTER TABLE orders MODIFY COLUMN status ENUM('CONFIRMED','PREPARING','READY_FOR_PICKUP','TRANSFERRING','AT_DELIVERY_POINT','OUT_FOR_DELIVERY','DELIVERED','COMPLETED','CANCELLED','LOST') NOT NULL DEFAULT 'CONFIRMED'"); }
            }
            service.RefundService.ensureSchema(c);
            return null;
        });
    }
    private static boolean ensureColumn(Connection c,String table,String column,String definition)throws SQLException {
        boolean exists;
        try(ResultSet r=c.getMetaData().getColumns(c.getCatalog(),null,table,column)){exists=r.next();}
        if(!exists)try(Statement s=c.createStatement()){s.execute("ALTER TABLE "+table+" ADD COLUMN "+column+" "+definition);}
        return !exists;
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
