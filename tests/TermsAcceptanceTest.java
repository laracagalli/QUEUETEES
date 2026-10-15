import model.*;
import repository.*;
import service.*;
import gui.auth.TermsPanel;
import javax.swing.*;
import java.awt.*;

public final class TermsAcceptanceTest {
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    private static void blocked(Runnable action){try{action.run();}catch(IllegalArgumentException|IllegalStateException expected){return;}throw new AssertionError("Invalid acceptance succeeded");}
    private static void layout(Container root){root.doLayout();for(Component c:root.getComponents())if(c instanceof Container container)layout(container);}
    private static <T> T find(Container root,Class<T> type){for(Component c:root.getComponents()){if(type.isInstance(c))return type.cast(c);if(c instanceof Container container){T found=find(container,type);if(found!=null)return found;}}return null;}
    private static JButton button(Container root){for(Component c:root.getComponents()){if(c instanceof JButton b && "Agree & Continue".equals(b.getText()))return b;if(c instanceof Container container){JButton found=button(container);if(found!=null)return found;}}return null;}
    public static void main(String[] args)throws Exception{
        InMemoryUserRepository users=new InMemoryUserRepository();AuthService auth=new AuthService(users);
        User customer=new User(1,"customer","customer@example.com",PasswordUtil.hashPassword("Customer123!"),UserRole.CUSTOMER,false,AccountStatus.ACTIVE);users.save(customer);
        blocked(()->auth.acceptTerms(customer,false));check(!auth.hasAcceptedTerms(customer),"Unchecked acceptance is not saved");
        SwingUtilities.invokeAndWait(()->{try{
            int[] navigations={0};TermsPanel panel=new TermsPanel(auth,customer,()->navigations[0]++,()->{});
            JCheckBox agreement=find(panel,JCheckBox.class);JButton next=button(panel);
            check(!agreement.isSelected() && !next.isEnabled(),"Agreement starts unchecked and Continue disabled");
            next.doClick();check(navigations[0]==0,"Unchecked Continue cannot navigate");
            for(var listener:next.getActionListeners())listener.actionPerformed(new java.awt.event.ActionEvent(next,0,"test"));
            check(navigations[0]==0,"Unchecked listener cannot bypass gate");
            agreement.doClick();check(next.isEnabled(),"Checking agreement enables Continue");
            agreement.doClick();check(!next.isEnabled(),"Unchecking disables Continue again");
            panel.setSize(890,700);layout(panel);
            var image=new java.awt.image.BufferedImage(890,700,java.awt.image.BufferedImage.TYPE_INT_RGB);var graphics=image.createGraphics();panel.printAll(graphics);graphics.dispose();
            new java.io.File("bin/review").mkdirs();javax.imageio.ImageIO.write(image,"png",new java.io.File("bin/review/terms-conditions.png"));
            agreement.doClick();next.doClick();check(navigations[0]==1 && auth.hasAcceptedTerms(customer),"Continue saves agreement before navigation");
        }catch(Exception e){throw new RuntimeException(e);}});
        check(!customer.isEmailVerified(),"Terms acceptance does not bypass email verification");
        var accepted=customer.getTermsAcceptedAt();auth.acceptTerms(customer,true);check(accepted.equals(customer.getTermsAcceptedAt()),"Repeated acceptance does not overwrite timestamp");
        customer.setTermsAcceptance("old-version",accepted);check(!auth.hasAcceptedTerms(customer),"New version requires fresh acceptance");
        customer.setStatus(AccountStatus.BANNED);blocked(()->auth.acceptTerms(customer,true));
        User staff=new User(2,"staff","staff@example.com",PasswordUtil.hashPassword("Staff123!"),UserRole.STAFF,false,AccountStatus.PENDING_APPROVAL);users.save(staff);
        SwingUtilities.invokeAndWait(()->{
            int[] navigations={0};TermsPanel panel=new TermsPanel(auth,staff,()->navigations[0]++,()->{});
            check(find(panel,JTextArea.class).getText().equals(TermsPanel.STAFF_RULES),"Staff sees staff responsibilities rather than customer shopping rules");
            JCheckBox agreement=find(panel,JCheckBox.class);JButton next=button(panel);
            check(agreement.getText().contains("Staff") && !next.isEnabled(),"Staff agreement is explicitly labeled and unchecked");
            agreement.doClick();next.doClick();check(navigations[0]==1 && auth.hasAcceptedTerms(staff),"Staff acceptance is saved before continuing");
            check(staff.getStatus()==AccountStatus.PENDING_APPROVAL && !staff.isEmailVerified(),"Staff acceptance does not grant approval or email verification");
        });
        System.out.println("PASS: checkbox gate, guarded navigation, saved acceptance, version checks and suspended account rejection");
    }
}
