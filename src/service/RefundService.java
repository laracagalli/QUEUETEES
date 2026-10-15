package service;
import database.Database;import model.*;import java.sql.*;import java.time.LocalDateTime;import java.util.*;

/** One refund ledger for returned items, cancellations and lost parcels. Records payments only. */
public final class RefundService {
    private static Database database;
    private static final Map<String,RefundRecord> memory=new LinkedHashMap<>();
    private RefundService(){}
    public static void ensureSchema(Connection c)throws SQLException{
        try(Statement s=c.createStatement()){
            s.execute("CREATE TABLE IF NOT EXISTS refund_records(source VARCHAR(80) PRIMARY KEY,order_id INT NOT NULL,kind VARCHAR(30) NOT NULL,amount DECIMAL(12,2) NOT NULL,status VARCHAR(20) NOT NULL,reference VARCHAR(100) NOT NULL,note TEXT NOT NULL,requested_at DATETIME(6) NOT NULL,paid_at DATETIME(6) NULL,paid_by INT NULL,FOREIGN KEY(order_id) REFERENCES orders(id)) ENGINE=InnoDB");
            s.execute("CREATE TABLE IF NOT EXISTS refund_payment_references(reference VARCHAR(100) PRIMARY KEY,source VARCHAR(80) NOT NULL,FOREIGN KEY(source) REFERENCES refund_records(source) ON DELETE CASCADE) ENGINE=InnoDB");
            s.executeUpdate("INSERT IGNORE INTO refund_records(source,order_id,kind,amount,status,reference,note,requested_at,paid_at,paid_by) SELECT CONCAT('RETURN:',id),order_id,'RETURN',refund_amount,'PAID',refund_reference,resolution,requested_at,COALESCE(reviewed_at,requested_at),reviewer_id FROM return_requests WHERE status='REFUNDED' AND refund_amount>0");
        }
    }
    public static void useDatabase(Database db){database=db;}
    public static double paidAmount(Order o){
        return o.getPaidAmount();
    }
    public static boolean terminal(Order o){return o.getStatus()==OrderStatus.COMPLETED||o.getStatus()==OrderStatus.CANCELLED||o.getStatus()==OrderStatus.LOST;}
    public static void admin(User actor){if(actor==null||actor.getRole()!=UserRole.ADMIN||actor.getStatus()!=AccountStatus.ACTIVE)throw new IllegalStateException("An active administrator is required to record refunds.");}
    private static RefundRecord row(ResultSet r)throws SQLException{return new RefundRecord(r.getString("source"),r.getInt("order_id"),r.getString("kind"),r.getDouble("amount"),r.getString("status"),r.getString("reference"),r.getString("note"),r.getObject("requested_at",LocalDateTime.class),r.getObject("paid_at",LocalDateTime.class));}
    public static List<RefundRecord> records(User actor){
        if(actor==null||actor.getStatus()!=AccountStatus.ACTIVE)throw new IllegalStateException("Active account access is required.");
        if(database==null){List<RefundRecord> snapshot;synchronized(memory){snapshot=List.copyOf(memory.values());}return snapshot.stream().filter(r->actor.getRole()!=UserRole.CUSTOMER||StoreService.getInstance().getOrdersForCustomer(actor.getId()).stream().anyMatch(o->o.getId()==r.orderId())).toList();}
        return database.read(c->{try(PreparedStatement access=c.prepareStatement("SELECT role,status FROM users WHERE id=?")){access.setInt(1,actor.getId());try(ResultSet r=access.executeQuery()){if(!r.next()||!r.getString(1).equals(actor.getRole().name())||!r.getString(2).equals("ACTIVE"))throw new IllegalStateException("Account access changed.");}}try(PreparedStatement s=c.prepareStatement("SELECT * FROM refund_records WHERE (?=0 OR order_id IN (SELECT id FROM orders WHERE customer_id=?)) ORDER BY requested_at DESC")){s.setInt(1,actor.getRole()==UserRole.CUSTOMER?1:0);s.setInt(2,actor.getId());try(ResultSet r=s.executeQuery()){List<RefundRecord> list=new ArrayList<>();while(r.next())list.add(row(r));return list;}}});
    }
    public static void request(Connection c,Order o,String source,String kind,double amount,String note)throws SQLException{
        if(amount<=0)return;
        if(c==null){synchronized(memory){if(memory.containsKey(source))return;double reserved=memory.values().stream().filter(r->r.orderId()==o.getId()).mapToDouble(RefundRecord::amount).sum();if(reserved+amount>paidAmount(o)+.001)throw new IllegalStateException("Refund exceeds the recorded payment.");memory.put(source,new RefundRecord(source,o.getId(),kind,amount,"PENDING","",note,LocalDateTime.now(),null));return;}}
        try(PreparedStatement lock=c.prepareStatement("SELECT id FROM orders WHERE id=? FOR UPDATE")){lock.setInt(1,o.getId());lock.executeQuery().close();}
        try(PreparedStatement s=c.prepareStatement("SELECT source FROM refund_records WHERE source=?")){s.setString(1,source);try(ResultSet r=s.executeQuery()){if(r.next())return;}}
        try(PreparedStatement s=c.prepareStatement("SELECT COALESCE(SUM(amount),0) FROM refund_records WHERE order_id=?")){s.setInt(1,o.getId());try(ResultSet r=s.executeQuery()){r.next();if(r.getDouble(1)+amount>paidAmount(o)+.001)throw new IllegalStateException("Refund exceeds the recorded payment.");}}
        try(PreparedStatement s=c.prepareStatement("INSERT INTO refund_records(source,order_id,kind,amount,status,reference,note,requested_at) VALUES(?,?,?,?,'PENDING','',?,?)")){s.setString(1,source);s.setInt(2,o.getId());s.setString(3,kind);s.setDouble(4,amount);s.setString(5,note);s.setObject(6,LocalDateTime.now());s.executeUpdate();}
    }
    public static void pay(Connection c,String source,String reference,String note,User actor)throws SQLException{
        admin(actor);
        if(reference==null||!reference.trim().matches("[A-Za-z0-9][A-Za-z0-9 ./_-]{3,99}")||note==null||note.trim().length()<5||note.length()>1000)throw new IllegalArgumentException("Enter a payment reference (4–100 characters) and payment note (5–1000 characters).");
        if(c==null){synchronized(memory){RefundRecord r=memory.get(source);if(r==null||!r.status().equals("PENDING"))throw new IllegalStateException("This refund is no longer pending.");if(memory.values().stream().anyMatch(v->v.status().equals("PAID")&&v.reference().equalsIgnoreCase(reference.trim())))throw new IllegalArgumentException("That refund reference has already been recorded.");memory.put(source,new RefundRecord(source,r.orderId(),r.kind(),r.amount(),"PAID",reference.trim(),r.note()+"\n"+note.trim(),r.requestedAt(),LocalDateTime.now()));return;}}
        try(PreparedStatement s=c.prepareStatement("SELECT id FROM users WHERE id=? AND role='ADMIN' AND status='ACTIVE'")){s.setInt(1,actor.getId());try(ResultSet r=s.executeQuery()){if(!r.next())throw new IllegalStateException("Administrator access changed.");}}
        try(PreparedStatement s=c.prepareStatement("SELECT source FROM refund_records WHERE status='PAID' AND reference=?")){s.setString(1,reference.trim());try(ResultSet r=s.executeQuery()){if(r.next())throw new IllegalArgumentException("That refund reference has already been recorded.");}}
        try(PreparedStatement s=c.prepareStatement("INSERT INTO refund_payment_references(reference,source) VALUES(?,?)")){s.setString(1,reference.trim());s.setString(2,source);s.executeUpdate();}
        try(PreparedStatement s=c.prepareStatement("UPDATE refund_records SET status='PAID',reference=?,note=CONCAT(note,' / ',?),paid_at=?,paid_by=? WHERE source=? AND status='PENDING'")){s.setString(1,reference.trim());s.setString(2,note.trim());s.setObject(3,LocalDateTime.now());s.setInt(4,actor.getId());s.setString(5,source);if(s.executeUpdate()!=1)throw new IllegalStateException("This refund is no longer pending.");}
    }
    public static void recordOrderRefund(String source,String reference,String note,User actor){
        admin(actor);if(source==null||!source.startsWith("ORDER:"))throw new IllegalArgumentException("Use return review to record returned-item refunds.");
        if(database==null){try{pay(null,source,reference,note,actor);}catch(SQLException e){throw new IllegalStateException(e);}return;}
        database.transaction(c->{pay(c,source,reference,note,actor);repository.JdbcActivityRepository.record(c,actor,"Refund recorded",source+": "+reference);return null;});
    }
    public static double totalPaid(int orderId,List<RefundRecord> records){return records.stream().filter(r->r.orderId()==orderId&&r.status().equals("PAID")).mapToDouble(RefundRecord::amount).sum();}
    public static String outcome(Order o,List<RefundRecord> records){if(totalPaid(o.getId(),records)>=o.getTotal()-.001&&o.getTotal()>0)return "Refunded";boolean pending=records.stream().anyMatch(r->r.orderId()==o.getId()&&r.status().equals("PENDING"));return o.getStatusLabel()+(pending?" / refund pending":"");}
}
