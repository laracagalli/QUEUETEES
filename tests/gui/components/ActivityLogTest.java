package gui.components;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import javax.swing.*;
import model.*;
import service.*;
import repository.InMemoryUserRepository;

public class ActivityLogTest {
    static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
    static Object field(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name); field.setAccessible(true); return field.get(target);
    }
    static void layout(Container c) { c.doLayout(); for(Component child:c.getComponents()) if(child instanceof Container)layout((Container)child); }
    static void render(JComponent panel, String name, int width, int height) throws Exception {
        AppTheme.apply(panel); panel.setSize(width,height); layout(panel);
        BufferedImage image=new BufferedImage(width,height,BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics=image.createGraphics(); graphics.setColor(new Color(230,234,224)); graphics.fillRect(0,0,width,height);
        panel.printAll(graphics); graphics.dispose();
        javax.imageio.ImageIO.write(image,"png",new java.io.File("bin/review/"+name+".png"));
    }
    public static void main(String[] args) throws Exception {
        StoreService.getInstance();
        var repository=new InMemoryUserRepository();
        var admin=new User(700,"admin-audit","admin@example.com",PasswordUtil.hashPassword("Admin123!"),UserRole.ADMIN,true,AccountStatus.ACTIVE);
        var staff=new User(701,"staff-audit","staff@example.com",PasswordUtil.hashPassword("Staff123!"),UserRole.STAFF,true,AccountStatus.ACTIVE);
        var customer=new User(702,"customer","customer@example.com","unused",UserRole.CUSTOMER,true,AccountStatus.ACTIVE);
        repository.save(admin); repository.save(staff); repository.save(customer);
        var auth=new AuthService(repository);
        auth.login("admin-audit","Admin123!".toCharArray());
        check(ActivityLogger.getEntries().get(0).action().equals("Logged in"),"Login recorded with identity");
        auth.changePassword(admin,"Admin123!".toCharArray(),"Changed123!".toCharArray(),"Changed123!".toCharArray());
        check(ActivityLogger.getEntries().get(0).action().equals("Password changed"),"Password success recorded");
        int before=ActivityLogger.getEntries().size();
        ActivityLogger.record(customer,"UI command","Customer action");
        check(ActivityLogger.getEntries().size()==before,"Customer events excluded from staff/admin activity");
        ActivityLogger.record(staff,"Order updated","Q-001: Confirmed → Preparing");
        check(ActivityLogger.getEntries().stream().noneMatch(e->e.details().contains("Changed123!")),"Passwords absent from log");
        var snapshot=ActivityLogger.getEntries();
        try{snapshot.clear();throw new AssertionError("Log snapshot mutable");}catch(UnsupportedOperationException expected){}
        SwingUtilities.invokeAndWait(()->{try{
            AppTheme.install();
            var panel=new ActivityLogPanel();
            JTable table=(JTable)field(panel,"table");
            JComboBox<?> role=(JComboBox<?>)field(panel,"role"),action=(JComboBox<?>)field(panel,"action");
            JTextField search=(JTextField)field(panel,"search");
            role.setSelectedItem("STAFF"); action.setSelectedItem("Order updated"); search.setText("Q-001");
            check(table.getRowCount()==1,"Role, action and text filters combine");
            ActivityLogger.record(admin,"Stock adjusted","Test shirt: 4 → 8 (+4)"); panel.refresh();
            check(table.getRowCount()==1&&role.getSelectedItem().equals("STAFF"),"Refresh preserves filters");
            search.setText("missing"); check(table.getRowCount()==0,"Empty matches supported");
            search.setText(""); role.setSelectedIndex(0); action.setSelectedIndex(0);
            render(panel,"activity-log",1120,700);
            var root=new JPanel(); var command=new JButton("View details"); root.add(command);
            ActivityTracking.track(root,staff); ActivityTracking.track(root,staff);
            int count=ActivityLogger.getEntries().size(); command.doClick();
            check(ActivityLogger.getEntries().size()==count+1,"Commands captured once even after repeat tracking");
            check(ActivityLogger.getEntries().get(0).username().equals("staff-audit"),"Correct command actor");
            var adminDashboard=new gui.admin.AdminDashboardPanel(auth,admin);
            java.util.Map<?,?> adminNavigation=(java.util.Map<?,?>)field(adminDashboard,"navigation");
            ((JButton)adminNavigation.get("activity")).doClick();
            render(adminDashboard,"activity-admin",1440,820);
            var staffDashboard=new gui.staff.StaffDashboardPanel(auth,staff);
            java.util.Map<?,?> staffNavigation=(java.util.Map<?,?>)field(staffDashboard,"navigation");
            ((JButton)staffNavigation.get("activity")).doClick();
            render(staffDashboard,"activity-staff",1440,820);
            check(ActivityLogger.getEntries().get(0).details().equals("Activity Log"),"Sidebar navigation audited");
            System.out.println("PASS: audit identities, safe password logging, immutable snapshots, filters, command tracking and both sidebars");
        }catch(Exception ex){throw new RuntimeException(ex);}});
    }
}
