import java.awt.*;
import javax.swing.*;
import model.*;
public class ProfileDisplayTest {
    static String text(Component c){String s=c instanceof JLabel?String.valueOf(((JLabel)c).getText()):c instanceof javax.swing.text.JTextComponent?((javax.swing.text.JTextComponent)c).getText():"";if(c instanceof Container)for(Component child:((Container)c).getComponents())s+="\n"+text(child);return s;}
    public static void main(String[] args)throws Exception{SwingUtilities.invokeAndWait(()->{
        User user=new User(91,"profile-check","profile@example.com","secret-hash",UserRole.ADMIN,true,AccountStatus.ACTIVE);
        repository.InMemoryUserRepository repo=new repository.InMemoryUserRepository();repo.save(user);
        service.AuthService auth=new service.AuthService(repo);
        for(JPanel panel:new JPanel[]{new gui.admin.AdminDashboardPanel(auth,user),new gui.customer.CustomerDashboardPanel(auth,user),new gui.staff.StaffProfilePanel(user)}){
            String shown=text(panel);
            if(!shown.contains("profile-check")||!shown.contains("profile@example.com")||shown.contains("secret-hash")||shown.contains("authenticated user record"))throw new AssertionError("Profile not wired correctly");
        }
        System.out.println("PASS: signed-in profiles display actual identity without placeholders or password hashes");
    });}
}
