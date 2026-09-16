package gui.auth;

import backend.EmailService;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.*;
import model.*;
import repository.InMemoryUserRepository;
import service.AuthService;

/** Uses an injected sender: no real emails or credentials are needed. */
public class EmailAuthPresentationTest {
    static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    static java.util.List<Component> components(Container root) {
        java.util.List<Component> result = new java.util.ArrayList<>();
        for (Component c : root.getComponents()) { result.add(c);if(c instanceof Container) result.addAll(components((Container)c)); }
        return result;
    }
    static JButton button(JFrame frame, String text) {
        return (JButton)components(frame.getContentPane()).stream().filter(c -> c instanceof JButton && text.equals(((JButton)c).getText())).findFirst().orElseThrow();
    }
    static void await(java.util.function.BooleanSupplier condition) throws Exception {
        long deadline=System.currentTimeMillis()+5000;
        while(System.currentTimeMillis()<deadline) {
            boolean[] ready={false};SwingUtilities.invokeAndWait(()->ready[0]=condition.getAsBoolean());
            if(ready[0])return;Thread.sleep(50);
        }
        throw new AssertionError("Email state did not settle");
    }
    static void render(JFrame frame, String name, int width, int height) throws Exception {
        frame.setSize(width,height);frame.validate();
        Container root=frame.getContentPane();
        BufferedImage image=new BufferedImage(root.getWidth(),root.getHeight(),BufferedImage.TYPE_INT_RGB);
        Graphics2D g=image.createGraphics();root.printAll(g);g.dispose();
        new java.io.File("bin/review").mkdirs();
        javax.imageio.ImageIO.write(image,"png",new java.io.File("bin/review/"+name+".png"));
        JButton verify=button(frame,"Verify Email");
        Point center=SwingUtilities.convertPoint(verify,verify.getWidth()/2,0,root);
        for(Component c:components(root)) {
            if(c instanceof JLabel && "Check your inbox".equals(((JLabel)c).getText())) {
                Point heading=SwingUtilities.convertPoint(c,c.getWidth()/2,0,root);
                check(Math.abs(center.x-heading.x)<3,"Heading and actions must share a center");
            }
            if(c instanceof JTextField) {
                Point point=SwingUtilities.convertPoint(c,0,0,root);
                check(c.getWidth()>=50 && c.getHeight()>=54 && point.x>=0 && point.x+c.getWidth()<=root.getWidth(),"Code field fits");
            }
        }
    }
    public static void main(String[] args) throws Exception {
        check(EmailService.deliveryFailureMessage(new java.util.concurrent.ExecutionException(new EmailService.EmailConfigurationException())).contains("configure"),"Configuration diagnosis");
        check(EmailService.deliveryFailureMessage(new jakarta.mail.AuthenticationFailedException("private server detail")).contains("could not sign in"),"Authentication diagnosis");
        check(!EmailService.deliveryFailureMessage(new jakarta.mail.MessagingException("private server detail")).contains("private"),"No raw server details");
        check(EmailService.deliveryFailureMessage(new java.net.SocketTimeoutException()).contains("connection"),"Connection diagnosis");
        AtomicInteger sends=new AtomicInteger();AtomicReference<EmailAuthFrame> ref=new AtomicReference<>();
        AuthService auth=new AuthService(new InMemoryUserRepository());
        User user=new User(1,"customer","customer@example.com","",UserRole.CUSTOMER,false,AccountStatus.ACTIVE);
        try {
            SwingUtilities.invokeAndWait(()->{
                EmailAuthFrame frame=new EmailAuthFrame(auth,user,(email,code)->{
                    if(sends.incrementAndGet()==1)throw new EmailService.EmailConfigurationException();
                    check(code.matches("[0-9]{6}"),"Six-digit generated code");
                });
                frame.addNotify();frame.validate();ref.set(frame);
            });
            await(()->components(ref.get().getContentPane()).stream().anyMatch(c->c instanceof JButton && "Try sending again".equals(((JButton)c).getText())));
            SwingUtilities.invokeAndWait(()->{try {
                EmailAuthFrame frame=ref.get();
                check(!button(frame,"Verify Email").isEnabled(),"Cannot verify a failed send");
                render(frame,"email-auth-error",1100,733);
                render(frame,"email-auth-small",900,650);
                button(frame,"Try sending again").doClick();
                check(!button(frame,"Verify Email").isEnabled(),"Cannot verify while sending");
            }catch(Exception ex){throw new RuntimeException(ex);}});
            await(()->button(ref.get(),"Verify Email").isEnabled());
            SwingUtilities.invokeAndWait(()->{try {
                check(sends.get()==2,"Retry sends once");
                render(ref.get(),"email-auth-ready",1100,733);
            }catch(Exception ex){throw new RuntimeException(ex);}});
            System.out.println("PASS: email failure diagnosis, failed-send blocking, retry recovery and aligned layouts at both window sizes");
        } finally { SwingUtilities.invokeAndWait(()->{if(ref.get()!=null)ref.get().dispose();}); }
    }
}
