import model.*;
import repository.*;
import service.*;
import java.time.LocalDate;
import javax.swing.*;
import java.awt.*;

public class StaffRegistrationTest {
    static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
    static void blocked(Runnable action) {
        try { action.run(); throw new AssertionError("Expected rejection"); }
        catch (IllegalStateException expected) { }
    }
    static RegistrationResult register(AuthService auth, String username) {
        return auth.registerStaff(username + "@example.com", "Lara Staff", username,
                "Staff123!".toCharArray(), "Manila", "9123456789", "Female", LocalDate.of(2000, 1, 1));
    }
    static <T> T find(Container root, Class<T> type) {
        for (Component child : root.getComponents()) {
            if (type.isInstance(child)) return type.cast(child);
            if (child instanceof Container) { T result = find((Container) child, type); if (result != null) return result; }
        }
        return null;
    }
    static void layout(Container root) { root.doLayout(); for (Component c : root.getComponents()) if(c instanceof Container) layout((Container)c); }
    static void render(JComponent panel, String name, int width, int height) throws Exception {
        panel.setSize(width, height);layout(panel);
        java.awt.image.BufferedImage image = new java.awt.image.BufferedImage(width,height,java.awt.image.BufferedImage.TYPE_INT_RGB);
        Graphics2D g=image.createGraphics();g.setColor(new Color(241,241,232));g.fillRect(0,0,width,height);panel.printAll(g);g.dispose();
        new java.io.File("bin/review").mkdirs();
        javax.imageio.ImageIO.write(image,"png",new java.io.File("bin/review/"+name+".png"));
    }
    public static void main(String[] args) throws Exception {
        InMemoryUserRepository repo = new InMemoryUserRepository();
        AuthService auth = new AuthService(repo);
        User admin = new User(1,"admin","admin@example.com",PasswordUtil.hashPassword("Admin123!"),UserRole.ADMIN,true,AccountStatus.ACTIVE);
        repo.save(admin);
        RegistrationResult result = register(auth,"newstaff");
        check(result.isSuccess(),result.getMessage());
        User staff = result.getUser();
        check(staff.getRole()==UserRole.STAFF && staff.getStatus()==AccountStatus.PENDING_APPROVAL,"Pending staff role");
        check(staff.getFullName().equals("Lara Staff") && staff.getAddress().equals("Manila") && staff.getContactNumber().equals("9123456789") && staff.getBirthday().equals(LocalDate.of(2000,1,1)),"Application details retained");
        check(auth.login("newstaff","Staff123!".toCharArray()).getStatus()==AuthStatus.STAFF_NOT_APPROVED,"Pending login blocked");
        check(!register(auth,"newstaff").isSuccess(),"Duplicate rejected");
        blocked(()->auth.reviewStaff(staff,staff.getId(),true));
        blocked(()->auth.getStaffApplications(staff));
        User rejected = register(auth,"rejectedstaff").getUser();
        auth.reviewStaff(admin,rejected.getId(),false);
        check(auth.login(rejected.getEmail(),"Staff123!".toCharArray()).getStatus()==AuthStatus.ACCOUNT_REJECTED,"Rejected login blocked");
        blocked(()->auth.reviewStaff(admin,rejected.getId(),true));
        User customer=auth.registerCustomer("customer@example.com","Customer Name","customer","Customer123!".toCharArray(),"Manila","9123456789","Female",LocalDate.of(2000,1,1)).getUser();
        check(customer.getRole()==UserRole.CUSTOMER && !customer.isEmailVerified(),"Customer registration unchanged");
        check(auth.login("customer","Customer123!".toCharArray()).getStatus()==AuthStatus.EMAIL_NOT_VERIFIED,"Customer verification required");
        blocked(()->auth.reviewStaff(admin,customer.getId(),true));
        SwingUtilities.invokeAndWait(()->{try {
            gui.admin.StaffApprovalsPanel panel=new gui.admin.StaffApprovalsPanel(auth,admin);
            JTable table=find(panel,JTable.class);
            check(table.getRowCount()==2,"Only staff listed");
            table.getRowSorter().toggleSortOrder(0);
            JComboBox<?> filter=find(panel,JComboBox.class);filter.setSelectedItem("Pending");
            check(table.getRowCount()==1 && table.getValueAt(0,1).equals(staff.getEmail()),"Pending filter");
            table.setRowSelectionInterval(0,0);
            render(panel,"staff-applications",1120,700);
            auth.reviewStaff(admin,staff.getId(),true);panel.refresh();
            check(table.getRowCount()==0,"Approval removes pending row");
            filter.setSelectedItem("Approved");check(table.getRowCount()==1,"Approved filter");
            if (args.length>0 && args[0].equals("--frames")) {
                JFrame login=new gui.auth.LoginFrame(auth);
                render((JComponent)login.getContentPane(),"staff-login",1100,695);login.dispose();
                JFrame signup=new gui.auth.SignupFrame(auth,true);
                render((JComponent)signup.getContentPane(),"staff-signup",1100,695);signup.dispose();
            }
        } catch(Exception ex) {throw new RuntimeException(ex);} });
        check(auth.login("newstaff","Staff123!".toCharArray()).isSuccess(),"Approved staff login");
        blocked(()->auth.reviewStaff(admin,staff.getId(),false));
        System.out.println("PASS: staff registration, details, duplicates, approval/rejection, permissions, login and filtered admin table");
        System.exit(0);
    }
}
