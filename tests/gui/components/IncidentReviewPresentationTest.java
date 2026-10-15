package gui.components;

import java.awt.*;
import java.awt.image.BufferedImage;
import javax.swing.*;
import javax.swing.table.*;
import model.*;
import repository.*;
import service.*;

public final class IncidentReviewPresentationTest {
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    private static void layout(Container c){c.doLayout();for(Component child:c.getComponents())if(child instanceof Container container)layout(container);}
    private static <T> T find(Container c,Class<T> type){for(Component child:c.getComponents()){if(type.isInstance(child))return type.cast(child);if(child instanceof Container container){T found=find(container,type);if(found!=null)return found;}}return null;}
    private static JTextArea details(Container c){for(Component child:c.getComponents()){if(child instanceof JTextArea a && "Full selected warning details".equals(a.getAccessibleContext().getAccessibleName()))return a;if(child instanceof Container container){var found=details(container);if(found!=null)return found;}}return null;}
    public static void main(String[] args)throws Exception{
        InMemoryUserRepository users=new InMemoryUserRepository();AuthService auth=new AuthService(users);
        User admin=new User(1,"admin","admin@example.com","hash",UserRole.ADMIN,true,AccountStatus.ACTIVE);
        User customer=new User(2,"testcustomer","customer@example.com","hash",UserRole.CUSTOMER,true,AccountStatus.ACTIVE);users.save(admin);users.save(customer);
        String event="Five consecutive failed sign-in attempts for this account. This long explanation should remain available in full when the administrator selects the warning.";
        SecurityIncidents.flag(customer,event);
        var incident=SecurityIncidents.incidents().get(0);
        SecurityIncidents.flag(customer,"Second incident to verify sorting and selection.");
        SwingUtilities.invokeAndWait(()->{try{
            JPanel panel=IncidentReview.createPanel(admin);AppTheme.apply(panel);
            JTable table=find(panel,JTable.class);table.getRowSorter().toggleSortOrder(1);table.getRowSorter().toggleSortOrder(1);
            for(int i=0;i<table.getRowCount();i++)if(event.equals(table.getValueAt(i,1)))table.setRowSelectionInterval(i,i);
            check(details(panel).getText().contains(event),"Clicking a sorted warning shows its full message");
            check(String.valueOf(table.getValueAt(table.getSelectedRow(),2)).matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}"),"Date is formatted for display");
            JLabel cell=(JLabel)table.prepareRenderer(table.getCellRenderer(table.getSelectedRow(),0),table.getSelectedRow(),0);
            check(cell.getForeground().equals(table.getSelectionForeground()) && !cell.getForeground().equals(cell.getBackground()),"Selected warning text stays readable");
            JLabel header=(JLabel)table.getTableHeader().getDefaultRenderer().getTableCellRendererComponent(table,"User",false,false,-1,0);
            check(cell.getInsets().left==header.getInsets().left,"Header and row text have matching left padding");
            panel.setSize(1120,590);layout(panel);
            check(table.getParent().getHeight()>100,"Warning table remains visible below the introductory message");
            check(details(panel).getParent().getHeight()>80,"Full details remain visible beneath the warning table");
            BufferedImage image=new BufferedImage(1120,590,BufferedImage.TYPE_INT_RGB);Graphics2D g=image.createGraphics();panel.paint(g);g.dispose();
            new java.io.File("bin/review").mkdirs();javax.imageio.ImageIO.write(image,"png",new java.io.File("bin/review/usage-warning-selected.png"));
        }catch(Exception ex){throw new RuntimeException(ex);}});
        SecurityIncidents.sendWarning(admin,incident.id());SecurityIncidents.resolve(admin,incident.id(),"Test incident reviewed");
        check(auth.getManagedUsers(admin).stream().anyMatch(u->u.getId()==customer.getId()),"Sending and reviewing a warning preserves the account record");
        System.out.println("PASS: sorted warning details, date formatting, selected-row contrast, header alignment and account retention");
    }
}
