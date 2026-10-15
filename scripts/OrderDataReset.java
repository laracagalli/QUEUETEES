import database.Database;
import java.nio.file.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** Explicit local maintenance command. Deletes only orders and their dependent records. */
public final class OrderDataReset {
    static long count(Connection c,String table)throws SQLException {
        try(Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT COUNT(*) FROM "+table)){r.next();return r.getLong(1);}
    }
    static boolean exists(Connection c,String table)throws SQLException {
        try(ResultSet r=c.getMetaData().getTables(c.getCatalog(),null,table,new String[]{"TABLE"})){return r.next();}
    }
    static String value(ResultSet r,int column)throws SQLException {
        Object v=r.getObject(column);
        if(v==null)return "NULL";
        if(v instanceof byte[] bytes)return "X'"+HexFormat.of().formatHex(bytes)+"'";
        if(v instanceof Number)return v.toString();
        if(v instanceof Boolean b)return b?"1":"0";
        return "CONVERT(X'"+HexFormat.of().formatHex(r.getString(column).getBytes(java.nio.charset.StandardCharsets.UTF_8))+"' USING utf8mb4)";
    }
    public static void main(String[] args)throws Exception {
        if(args.length!=1 || !(args[0].equals("--inspect")||args[0].equals("--reset")))throw new IllegalArgumentException("Use --inspect or --reset.");
        Properties p=new Properties();
        try(var in=Files.newInputStream(Path.of("database/application.properties"))){p.load(in);}
        Path local=Path.of("database/database.local.properties");
        if(Files.exists(local))try(var in=Files.newInputStream(local)){p.load(in);}
        String password=System.getenv("QUEUETEES_DB_PASSWORD");if(password==null)password=p.getProperty("password");
        if(password==null)throw new IllegalStateException("Local database password is missing.");
        Database db=new Database(p.getProperty("url"),p.getProperty("username"),password.toCharArray());
        try(Connection c=db.open()) {
            if(!"queuetees".equals(c.getCatalog()))throw new IllegalStateException("Expected the local queuetees application database.");
            List<String> tables=new ArrayList<>(List.of("orders","order_items"));
            if(exists(c,"return_requests"))tables.add("return_requests");
            if(exists(c,"refund_records"))tables.add("refund_records");
            if(exists(c,"refund_payment_references"))tables.add("refund_payment_references");
            long accounts=count(c,"users"),products=count(c,"products");
            System.out.println("Accounts: "+accounts+"; products: "+products);
            for(String table:tables)System.out.println(table+": "+count(c,table));
            if(args[0].equals("--inspect"))return;
            c.setAutoCommit(false);
            try {
                StringBuilder backup=new StringBuilder("-- QueueTees order-only backup; accounts and catalog excluded.\nUSE queuetees;\nSTART TRANSACTION;\n");
                for(String table:tables)try(Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT * FROM "+table+" FOR UPDATE")) {
                    ResultSetMetaData m=r.getMetaData();StringJoiner columns=new StringJoiner(",");
                    for(int i=1;i<=m.getColumnCount();i++)columns.add("`"+m.getColumnName(i)+"`");
                    while(r.next()){StringJoiner values=new StringJoiner(",");for(int i=1;i<=m.getColumnCount();i++)values.add(value(r,i));backup.append("INSERT INTO ").append(table).append(" (").append(columns).append(") VALUES (").append(values).append(");\n");}
                }
                backup.append("COMMIT;\n");
                Path output=Path.of("bin","order-backups","orders-"+LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))+".sql");
                Files.createDirectories(output.getParent());Files.writeString(output,backup.toString(),StandardOpenOption.CREATE_NEW);
                List<String> reverse=new ArrayList<>(tables);Collections.reverse(reverse);
                for(String table:reverse)try(Statement s=c.createStatement()){s.executeUpdate("DELETE FROM "+table);}
                for(String table:tables)if(count(c,table)!=0)throw new IllegalStateException("Reset verification failed.");
                if(count(c,"users")!=accounts||count(c,"products")!=products)throw new IllegalStateException("Account/catalog count changed.");
                c.commit();System.out.println("Order reset complete. Accounts/products preserved. Backup: "+output.toAbsolutePath());
            }catch(Exception e){c.rollback();throw e;}
        }
    }
}
