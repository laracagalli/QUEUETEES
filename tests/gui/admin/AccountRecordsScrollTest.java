package gui.admin;
import java.awt.*;
import javax.swing.*;
import model.*;
import repository.*;
import service.*;
public final class AccountRecordsScrollTest {
 static void layout(Container root){root.doLayout();for(Component c:root.getComponents())if(c instanceof Container p)layout(p);}
 public static void main(String[] args)throws Exception{
  SwingUtilities.invokeAndWait(()->{try{
   var repo=new InMemoryUserRepository();var admin=new User(1,"admin","admin@example.com","hash",UserRole.ADMIN,true,AccountStatus.ACTIVE);repo.save(admin);
   for(int i=0;i<10;i++)repo.save(new User(i+2,i==9?"jmpogi":"customer"+i,"customer"+i+"@example.com","hash",UserRole.CUSTOMER,true,AccountStatus.ACTIVE));
   User flagged=repo.findByEmailOrUsername("jmpogi").orElseThrow();SecurityIncidents.flag(flagged,"Five consecutive failed sign-in attempts for this account.");
   var incident=SecurityIncidents.incidents().get(0);SecurityIncidents.sendWarning(admin,incident.id());SecurityIncidents.resolve(admin,incident.id(),"Warning sent and incident reviewed");
   var panel=new CustomerManagementPanel(new AuthService(repo),admin);gui.components.AppTheme.apply(panel);panel.setSize(1120,700);layout(panel);layout(panel);
   var f=CustomerManagementPanel.class.getDeclaredField("table");f.setAccessible(true);JTable table=(JTable)f.get(panel);
   JScrollPane scroll=(JScrollPane)SwingUtilities.getAncestorOfClass(JScrollPane.class,table);
   scroll.getVerticalScrollBar().setValue(scroll.getVerticalScrollBar().getMaximum());layout(panel);
   Rectangle last=table.getCellRect(9,0,true);Rectangle visible=scroll.getViewport().getViewRect();
   if(!visible.contains(last))throw new AssertionError("Last account is hidden behind pagination footer");
   if(!"jmpogi".equals(table.getValueAt(9,0)) || !"ACTIVE".equals(table.getValueAt(9,5)))throw new AssertionError("Flagged account must remain visible with ACTIVE status");
   table.setRowSelectionInterval(9,9);
   var image=new java.awt.image.BufferedImage(1120,700,java.awt.image.BufferedImage.TYPE_INT_RGB);var graphics=image.createGraphics();panel.paint(graphics);graphics.dispose();
   new java.io.File("bin/review").mkdirs();javax.imageio.ImageIO.write(image,"png",new java.io.File("bin/review/account-records-last-row.png"));
   System.out.println("PASS: jmpogi stays reachable at the bottom of account records");
  }catch(Exception ex){throw new RuntimeException(ex);}});
 }
}
