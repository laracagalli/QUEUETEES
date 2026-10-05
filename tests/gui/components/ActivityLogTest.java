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
    static JButton button(Container root, String text) {
        for (Component child : root.getComponents()) {
            if (child instanceof JButton candidate && text.equals(candidate.getText())) return candidate;
            if (child instanceof Container nested) { JButton found = button(nested,text); if (found != null) return found; }
        }
        return null;
    }
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
        for (String action : new String[]{"UI command", "Search changed", "Table sorted", "Filter changed", "Date filter changed"})
            ActivityLogger.record(admin,action,"Unnecessary interaction");
        check(ActivityLogger.getEntries().size()==before,"UI interactions are excluded at the logger");
        auth.suspendUser(admin,staff.getId(),true);
        check(ActivityLogger.getEntries().size()==before+1&&ActivityLogger.getEntries().get(0).action().equals("Account suspended"),"Successful account change recorded once");
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
            JPanel userCounts=(JPanel)field(panel,"userCounts");
            check(((JLabel)userCounts.getComponent(0)).getText().equals("staff-audit (STAFF): 1 activity record(s)"),"Filtered per-user count");
            ActivityLogger.record(admin,"Stock adjusted","Test shirt: 4 → 8 (+4)"); panel.refresh();
            check(table.getRowCount()==1&&role.getSelectedItem().equals("STAFF"),"Refresh preserves filters");
            search.setText("missing"); check(table.getRowCount()==0,"Empty matches supported");
            search.setText(""); role.setSelectedIndex(0); action.setSelectedIndex(0);
            render(panel,"activity-log",1120,700);
            var root=new JPanel(); var command=new JButton("View details"); root.add(command);
            ActivityTracking.track(root,staff); ActivityTracking.track(root,staff);
            int count=ActivityLogger.getEntries().size(); command.doClick();
            check(ActivityLogger.getEntries().size()==count,"Viewing details does not create activity");
            check(ActivityLogger.getActor()==staff,"Dashboard still sets the operational actor");
            var adminDashboard=new gui.admin.AdminDashboardPanel(auth,admin);
            java.util.Map<?,?> adminNavigation=(java.util.Map<?,?>)field(adminDashboard,"navigation");
            var overview=(ActivityLogPanel)field(adminDashboard,"recentActivityPanel");
            JTable overviewTable=(JTable)field(overview,"table");
            check(overviewTable.getColumnModel().getColumn(0).getHeaderValue().equals("Username"),"Overview retains username-first columns");
            JComboBox<?> overviewRole=(JComboBox<?>)field(overview,"role"),overviewAction=(JComboBox<?>)field(overview,"action");
            JTextField overviewSearch=(JTextField)field(overview,"search");
            overviewRole.setSelectedItem("STAFF"); overviewAction.setSelectedItem("Order updated"); overviewSearch.setText("q-001");
            check(overviewTable.getRowCount()==1,"Overview combines case-insensitive search, role and action");
            ((JButton)adminNavigation.get("activity")).doClick();
            ((JButton)adminNavigation.get("overview")).doClick();
            check(overviewTable.getRowCount()==1&&overviewSearch.getText().equals("q-001"),"Overview preserves filters after navigation");
            overviewSearch.setText("absent");
            check(overviewTable.getRowCount()==0,"Overview supports no matches");
            overviewSearch.setText(""); overviewRole.setSelectedIndex(0); overviewAction.setSelectedIndex(0);
            overview.refresh();
            JPanel overviewCounts=(JPanel)field(overview,"userCounts");
            int counted=0;
            for(Component component:overviewCounts.getComponents()) if(component instanceof JLabel label) {
                String text=label.getText();
                counted+=Integer.parseInt(text.substring(text.lastIndexOf(": ")+2,text.indexOf(" activity record(s)")));
            }
            check(counted==overviewTable.getRowCount(),"Individual counts sum to all matching records");
            check(ActivityLogger.getEntries().size()==count,"Overview filtering and navigation do not create activity");
            overviewTable.getRowSorter().toggleSortOrder(1);
            check(ActivityLogger.getEntries().size()==count,"Table sorting does not create activity");
            JLabel overallCount=(JLabel)field(overview,"count");
            check(!overallCount.getFont().isBold(),"Overall count uses regular font");
            check(((java.awt.BorderLayout)overallCount.getParent().getLayout()).getConstraints(overallCount).equals(java.awt.BorderLayout.SOUTH),"Overall count appears below per-user labels");
            overviewTable.getRowSorter().setSortKeys(null);
            render(adminDashboard,"activity-overview",1440,820);
            ((JButton)adminNavigation.get("activity")).doClick();
            render(adminDashboard,"activity-admin",1440,820);
            var staffDashboard=new gui.staff.StaffDashboardPanel(auth,staff);
            java.util.Map<?,?> staffNavigation=(java.util.Map<?,?>)field(staffDashboard,"navigation");
            ((JButton)staffNavigation.get("queue")).doClick();
            render(staffDashboard,"activity-staff",1440,820);
            check(ActivityLogger.getEntries().size()==count,"Staff navigation does not create activity");
            JButton print=(JButton)field(panel,"print");
            search.setText("no matching record"); check(!print.isEnabled(),"Empty activity cannot print");
            search.setText("Q-001"); role.setSelectedItem("STAFF"); action.setSelectedItem("Order updated");
            check(print.isEnabled(),"Matching activity can print");
            Component originalHeader=((BorderLayout)panel.getLayout()).getLayoutComponent(BorderLayout.NORTH);
            print.doClick();
            check("adminReportPreview".equals(panel.getComponent(0).getName()),"Print activity opens report preview");
            render(panel,"activity-print-preview",1120,700);
            check(button(panel,"Print report")!=null,"Preview provides system print action");
            button(panel,"Back to table").doClick();
            check(((BorderLayout)panel.getLayout()).getLayoutComponent(BorderLayout.NORTH)==originalHeader,"Back restores original header placement");
            check(table.getRowCount()==1&&search.getText().equals("Q-001"),"Back preserves activity filters");
            render(panel,"activity-print-return",1120,700);
            System.out.println("PASS: meaningful activity, ignored UI interactions, per-user counts, overview/log filters and footer layout");
        }catch(Exception ex){throw new RuntimeException(ex);}});
    }
}
