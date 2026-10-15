package service;
import model.*;
import java.sql.*;
/** Re-check operator access, including suspensions from another app session. */
public final class OrderAccess {
    private OrderAccess() { }
    public static void memory(User actor,Order order,boolean collection) {
        // Memory mode also supports isolated domain tests without a signed-in UI.
        if(actor==null)return;
        if(actor.getStatus()!=AccountStatus.ACTIVE || actor.getRole()==UserRole.CUSTOMER
                && (!collection || actor.getId()!=order.getCustomerId()))
            throw new IllegalStateException("An active authorized account is required for this order action.");
    }
    public static void database(Connection c,User actor,Order order,boolean collection)throws SQLException {
        if(actor==null)throw new IllegalStateException("Sign in before updating an order.");
        memory(actor,order,collection);
        try(PreparedStatement s=c.prepareStatement("SELECT role,status FROM users WHERE id=?")){
            s.setInt(1,actor.getId());try(ResultSet r=s.executeQuery()){
                if(!r.next()||!r.getString("role").equals(actor.getRole().name())||!r.getString("status").equals("ACTIVE"))
                    throw new IllegalStateException("Your account no longer has access to update orders.");
            }
        }
    }
}
