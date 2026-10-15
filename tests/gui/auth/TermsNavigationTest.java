package gui.auth;

import java.awt.*;
import javax.swing.*;
import model.*;
import repository.InMemoryUserRepository;
import service.*;

/** Local navigation only: no database writes or verification emails. */
public final class TermsNavigationTest {
    private static void check(boolean value,String message) {
        if(!value)throw new AssertionError(message);
    }
    private static JButton button(Container root,String text) {
        for(Component child:root.getComponents()) {
            if(child instanceof JButton b && text.equals(b.getText()))return b;
            if(child instanceof Container c) { JButton found=button(c,text);if(found!=null)return found; }
        }
        return null;
    }
    private static Object field(Object target,String name)throws Exception {
        var field=target.getClass().getDeclaredField(name);field.setAccessible(true);return field.get(target);
    }
    private static <T extends Window> T visible(Class<T> type) {
        for(Window window:Window.getWindows())if(type.isInstance(window)&&window.isVisible())return type.cast(window);
        throw new AssertionError("Missing window: "+type.getSimpleName());
    }
    public static void main(String[] args)throws Exception {
        try {
            SwingUtilities.invokeAndWait(()->{try {
                for(UserRole role:new UserRole[]{UserRole.CUSTOMER,UserRole.STAFF}) {
                    InMemoryUserRepository users=new InMemoryUserRepository();AuthService auth=new AuthService(users);
                    String name=role==UserRole.STAFF?"stafftest":"customertest";
                    User user=new User(1,name,name+"@example.com",PasswordUtil.hashPassword("Example123!"),role,false,
                            role==UserRole.STAFF?AccountStatus.PENDING_APPROVAL:AccountStatus.ACTIVE,
                            "Test Person","123 Test Street, Barangay One, Pasay City, Metro Manila","9123456789","Female",java.time.LocalDate.of(2000,1,1));
                    user.setTermsRequired(true);users.save(user);
                    char[] transfer="Example123!".toCharArray();
                    TermsFrame terms=new TermsFrame(auth,user,()->{throw new AssertionError("Acceptance was bypassed");},transfer);
                    check(transfer[0]=='\0',"Temporary password transfer is cleared");
                    terms.setVisible(true);
                    for(int attempt=0;attempt<2;attempt++) {
                        button(terms.getContentPane(),"Back to signup").doClick();
                        SignupFrame signup=visible(SignupFrame.class);
                        check(name.equals(((JTextField)field(signup,"username")).getText()),"Registered username is preserved for "+role);
                        check(user.getEmail().equals(((JTextField)field(signup,"email")).getText()),"Email is retained");
                        check(user.getAddress().equals(((JTextField)field(signup,"address")).getText()),"Address is retained");
                        check("9123456789".equals(((JTextField)field(signup,"contactnum")).getText()),"Contact is retained");
                        check(((com.toedter.calendar.JDateChooser)field(signup,"birthday")).getDate()!=null && ((JRadioButton)field(signup,"female")).isSelected(),"Birthday and gender are retained");
                        JPasswordField password=(JPasswordField)field(signup,"pass");
                        check("Example123!".equals(new String(password.getPassword())) && password.getEchoChar()!=0,"Password is preserved and hidden for "+role);
                        JTextField contact=(JTextField)field(signup,"contactnum");
                        for(int first=0;first<=8;first++) {
                            contact.selectAll();contact.replaceSelection(first+"123456789");
                            check("9123456789".equals(contact.getText()),"Invalid contact prefix is rejected without losing previous input");
                        }
                        String updatedName=attempt==0?"Updated Person":"Changed Person";
                        ((JTextField)field(signup,"fullname")).setText(updatedName);
                        ((JButton)field(signup,"signupbtn")).doClick();
                        terms=visible(TermsFrame.class);
                        check(!auth.hasAcceptedTerms(user)&&!user.isEmailVerified(),"Returning to signup does not accept terms or verify email");
                        check(users.findAll().size()==1,"Resuming does not create a duplicate account");
                        check(users.findAll().get(0).getFullName().equals(updatedName),"Edits update the existing unfinished registration");
                    }
                    terms.dispose();
                }
                LoginFrame longEmailLogin=new LoginFrame(new AuthService(new InMemoryUserRepository()));
                JTextField identifier=(JTextField)field(longEmailLogin,"username");
                String longEmail="customer.with.a.long.email.address@example.com";
                identifier.setText(longEmail);
                check(longEmail.equals(identifier.getText()),"Login accepts email addresses longer than 36 characters");
                longEmailLogin.dispose();
            }catch(Exception ex){throw new RuntimeException(ex);} });
            System.out.println("PASS: customer/staff Terms > signup > Terms retains fields, supports edits, prevents duplicate accounts and blocks contact prefixes 0–8");
        } finally {
            SwingUtilities.invokeAndWait(()->{for(Window window:Window.getWindows())window.dispose();});
        }
        System.exit(0);
    }
}
