import model.*;
import repository.*;
import service.*;

public final class UsageWarningTest {
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    private static void blocked(Runnable action){try{action.run();}catch(IllegalArgumentException|IllegalStateException expected){return;}throw new AssertionError("Unauthorized action succeeded");}
    public static void main(String[] args) throws Exception {
        InMemoryUserRepository users=new InMemoryUserRepository();
        AuthService auth=new AuthService(users);
        User admin=new User(1,"admin","admin@example.com",PasswordUtil.hashPassword("Admin123!"),UserRole.ADMIN,true,AccountStatus.ACTIVE);
        User customer=new User(2,"customer","customer@example.com",PasswordUtil.hashPassword("Customer123!"),UserRole.CUSTOMER,true,AccountStatus.ACTIVE);
        User other=new User(3,"other","other@example.com",PasswordUtil.hashPassword("Customer123!"),UserRole.CUSTOMER,true,AccountStatus.ACTIVE);
        users.save(admin);users.save(customer);users.save(other);
        SecurityIncidents.flag(customer,"Repeated misuse reviewed by administrator");
        var incident=SecurityIncidents.incidents().get(0);
        check(SecurityIncidents.pendingWarnings(customer.getId()).isEmpty(),"Flag alone does not notify user");
        blocked(()->SecurityIncidents.sendWarning(customer,incident.id()));
        SecurityIncidents.sendWarning(admin,incident.id());
        check(SecurityIncidents.pendingWarnings(customer.getId()).contains(incident.id()),"Warning queued for correct user");
        check(SecurityIncidents.pendingWarnings(other.getId()).isEmpty(),"Other users do not receive warning");
        blocked(()->SecurityIncidents.sendWarning(admin,incident.id()));
        SecurityIncidents.acknowledgeWarning(other.getId(),incident.id());
        check(!SecurityIncidents.pendingWarnings(customer.getId()).isEmpty(),"Another user cannot acknowledge warning");
        SecurityIncidents.resolve(admin,incident.id(),"Misuse reviewed and warning sent");
        check(!SecurityIncidents.pendingWarnings(customer.getId()).isEmpty(),"Reviewing incident preserves undelivered warning");
        SecurityIncidents.acknowledgeWarning(customer.getId(),incident.id());
        check(SecurityIncidents.pendingWarnings(customer.getId()).isEmpty(),"Acknowledged warnings do not repeat");
        blocked(()->auth.permanentlySuspendUser(other,customer.getId(),"Confirmed misuse"));
        blocked(()->auth.permanentlySuspendUser(admin,admin.getId(),"Confirmed misuse"));
        blocked(()->auth.permanentlySuspendUser(admin,customer.getId(),""));
        auth.permanentlySuspendUser(admin,customer.getId(),"Confirmed repeated misuse");
        check(auth.currentAccountStatus(customer)==AccountStatus.BANNED,"Current session sees permanent suspension");
        LoginResult result=auth.login("customer","Customer123!".toCharArray());
        check(result.getStatus()==AuthStatus.ACCOUNT_BANNED && result.getMessage().equals(SecurityIncidents.PERMANENT_SUSPENSION_MESSAGE),"Suspended login shows approved message");
        blocked(()->auth.suspendUser(admin,customer.getId(),false,"Restore account"));
        check(auth.login("customer","wrong".toCharArray()).getStatus()==AuthStatus.INVALID_CREDENTIALS,"Suspension details require correct password");
        javax.swing.SwingUtilities.invokeAndWait(()->{
            try{
                var panel=new gui.admin.CustomerManagementPanel(auth,admin);
                var field=panel.getClass().getDeclaredField("table");field.setAccessible(true);
                javax.swing.JTable table=(javax.swing.JTable)field.get(panel);
                for(int i=0;i<table.getRowCount();i++)if("customer".equals(table.getValueAt(i,0)))table.setRowSelectionInterval(i,i);
                var restoreField=panel.getClass().getDeclaredField("unsuspendBtn");restoreField.setAccessible(true);
                check(!((javax.swing.JButton)restoreField.get(panel)).isEnabled(),"Permanent suspension cannot be restored from admin page");
                javax.swing.JComboBox<?> filter=findFilter(panel);filter.setSelectedItem("PERMANENTLY SUSPENDED");
                check(table.getRowCount()==1 && "PERMANENTLY SUSPENDED".equals(table.getValueAt(0,5)),"Admin can find permanently suspended accounts using the status filter");
            }catch(Exception e){throw new RuntimeException(e);}
        });
        System.out.println("PASS: targeted warnings, delivery acknowledgment, admin authorization, permanent suspension and login message");
    }
    private static javax.swing.JComboBox<?> findFilter(java.awt.Container root){
        for(java.awt.Component c:root.getComponents()){
            if(c instanceof javax.swing.JComboBox<?> combo)return combo;
            if(c instanceof java.awt.Container container){var found=findFilter(container);if(found!=null)return found;}
        }return null;
    }
}
