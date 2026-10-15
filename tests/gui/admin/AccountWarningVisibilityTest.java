package gui.admin;

import javax.swing.*;
import model.*;
import repository.*;
import service.*;

public final class AccountWarningVisibilityTest {
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    public static void main(String[] args)throws Exception{
        InMemoryUserRepository users=new InMemoryUserRepository();AuthService auth=new AuthService(users);
        User admin=new User(1,"admin","admin@example.com","hash",UserRole.ADMIN,true,AccountStatus.ACTIVE);
        User customer=new User(2,"first","first@example.com","hash",UserRole.CUSTOMER,true,AccountStatus.ACTIVE);users.save(admin);users.save(customer);
        CustomerManagementPanel[] panel={null};JTable[] table={null};JPanel root=new JPanel();
        SwingUtilities.invokeAndWait(()->{try{
            panel[0]=new CustomerManagementPanel(auth,admin);root.add(panel[0]);root.addNotify();
            var field=CustomerManagementPanel.class.getDeclaredField("table");field.setAccessible(true);table[0]=(JTable)field.get(panel[0]);table[0].setRowSelectionInterval(0,0);
        }catch(Exception e){throw new RuntimeException(e);}});
        User newest=new User(3,"newcustomer","new@example.com","hash",UserRole.CUSTOMER,true,AccountStatus.ACTIVE,"New Customer","","","",null);users.save(newest);
        SecurityIncidents.flag(newest,"Five consecutive failed sign-in attempts for this account.");
        SwingUtilities.invokeAndWait(()->{try{var method=CustomerManagementPanel.class.getDeclaredMethod("refreshIfChanged");method.setAccessible(true);method.invoke(panel[0]);}catch(Exception e){throw new RuntimeException(e);}});
        long deadline=System.currentTimeMillis()+5000;boolean[] ready={false};
        while(System.currentTimeMillis()<deadline){SwingUtilities.invokeAndWait(()->ready[0]=table[0].getModel().getRowCount()==2);if(ready[0])break;Thread.sleep(30);}
        check(ready[0],"Automatic refresh includes newly registered account");
        SwingUtilities.invokeAndWait(()->{
            check("first".equals(table[0].getValueAt(table[0].getSelectedRow(),0)),"Automatic refresh preserves existing selection");
            check("New Customer (newcustomer)".equals(table[0].getModel().getValueAt(1,0)),"Account record exposes the username used in warnings");
            root.removeNotify();
        });
        check(SecurityIncidents.incidents().stream().anyMatch(i->i.userId()==newest.getId()),"New account appears in both account records and warnings");
        System.out.println("PASS: new flagged accounts remain visible, automatic refresh, selection preservation and username identity");
    }
}
