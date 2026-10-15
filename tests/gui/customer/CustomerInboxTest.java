package gui.customer;
import java.awt.*;
import javax.swing.*;
import model.*;
import service.*;

public final class CustomerInboxTest {
    static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    static void layout(Container c){c.doLayout();for(Component child:c.getComponents())if(child instanceof Container container)layout(container);}
    static <T> T find(Container c,Class<T> type){for(Component child:c.getComponents()){if(type.isInstance(child))return type.cast(child);if(child instanceof Container container){T found=find(container,type);if(found!=null)return found;}}return null;}
    static void await(java.util.function.BooleanSupplier condition)throws Exception{long deadline=System.currentTimeMillis()+5000;while(System.currentTimeMillis()<deadline){boolean[] ready={false};SwingUtilities.invokeAndWait(()->ready[0]=condition.getAsBoolean());if(ready[0])return;Thread.sleep(30);}throw new AssertionError("Inbox state did not settle");}
    public static void main(String[] args)throws Exception{
        User admin=new User(1,"admin","admin@example.com","hash",UserRole.ADMIN,true,AccountStatus.ACTIVE);
        User user=new User(2,"customer","customer@example.com","hash",UserRole.CUSTOMER,true,AccountStatus.ACTIVE);
        User other=new User(3,"other","other@example.com","hash",UserRole.CUSTOMER,true,AccountStatus.ACTIVE);
        SecurityIncidents.flag(user,"First observed usage issue");SecurityIncidents.flag(user,"Second observed usage issue");
        var incidents=SecurityIncidents.incidents();for(var incident:incidents)SecurityIncidents.sendWarning(admin,incident.id());
        check(SecurityIncidents.warningMessages(other.getId()).isEmpty(),"Messages belong only to intended customer");
        SecurityIncidents.acknowledgePopup(user.getId(),incidents.get(0).id());
        check(SecurityIncidents.pendingWarnings(user.getId()).size()==2,"Closing popup does not consume inbox unread count");
        check(!SecurityIncidents.pendingPopupWarnings(user.getId()).contains(incidents.get(0).id()),"Dismissed popup does not repeat");
        SecurityIncidents.acknowledgeWarning(other.getId(),incidents.get(0).id());
        check(SecurityIncidents.pendingWarnings(user.getId()).size()==2,"Another customer cannot mark this message read");
        CustomerMailButton[] button={null};AdminInboxPanel[] inbox={null};JList<?>[] list={null};
        SwingUtilities.invokeAndWait(()->{button[0]=new CustomerMailButton(user);inbox[0]=new AdminInboxPanel(user,button[0]::setUnreadCount);list[0]=find(inbox[0],JList.class);});
        await(()->button[0].getUnreadCount()==2 && list[0].getModel().getSize()==2);
        SwingUtilities.invokeAndWait(()->list[0].setSelectedIndex(0));await(()->button[0].getUnreadCount()==1);
        SwingUtilities.invokeAndWait(()->{try{
            inbox[0].setSize(860,470);layout(inbox[0]);var image=new java.awt.image.BufferedImage(860,470,java.awt.image.BufferedImage.TYPE_INT_RGB);var g=image.createGraphics();inbox[0].paint(g);g.dispose();
            new java.io.File("bin/review").mkdirs();javax.imageio.ImageIO.write(image,"png",new java.io.File("bin/review/customer-admin-inbox.png"));
            button[0].setSize(64,44);var badge=new java.awt.image.BufferedImage(64,44,java.awt.image.BufferedImage.TYPE_INT_ARGB);var bg=badge.createGraphics();button[0].paint(bg);bg.dispose();javax.imageio.ImageIO.write(badge,"png",new java.io.File("bin/review/customer-mail-badge.png"));
            var repo=new repository.InMemoryUserRepository();repo.save(user);repo.save(admin);var dashboard=new CustomerDashboardPanel(new AuthService(repo),user);
            dashboard.setSize(1440,820);layout(dashboard);
            var header=new java.awt.image.BufferedImage(1440,100,java.awt.image.BufferedImage.TYPE_INT_RGB);var hg=header.createGraphics();dashboard.paint(hg);hg.dispose();javax.imageio.ImageIO.write(header,"png",new java.io.File("bin/review/customer-mail-header.png"));
            list[0].clearSelection();list[0].setSelectedIndex(0);
        }catch(Exception ex){throw new RuntimeException(ex);}});
        check(SecurityIncidents.pendingWarnings(user.getId()).size()==1,"Rereading a message does not decrement twice");
        SwingUtilities.invokeAndWait(()->list[0].setSelectedIndex(1));await(()->button[0].getUnreadCount()==0);
        for(var incident:incidents)SecurityIncidents.resolve(admin,incident.id(),"Reviewed and warning sent");
        check(SecurityIncidents.warningMessages(user.getId()).size()==2,"Read and reviewed messages remain permanently in history");
        check(SecurityIncidents.warningMessages(user.getId()).stream().allMatch(m->m.read() && m.message().equals(SecurityIncidents.WARNING_MESSAGE) && m.sentAt()!=null),"Messages retain body, date and read status");
        System.out.println("PASS: private inbox, unread badge 2→1→0, popup independence, duplicate-read protection and retained history");
    }
}
