import service.*;
import model.*;
import repository.*;
import java.time.*;
import java.util.concurrent.atomic.AtomicReference;
public class PasswordResetTest {
    static void fails(Runnable work) { try { work.run(); throw new AssertionError("Expected rejection"); } catch (IllegalArgumentException expected) {} }
    public static void main(String[] args) throws Exception {
        InMemoryUserRepository repo = new InMemoryUserRepository();
        User user = new User(1,"staff","staff@example.com",PasswordUtil.hashPassword("OldPass1!"),UserRole.STAFF,true,AccountStatus.ACTIVE);
        repo.save(user);
        AtomicReference<String> code = new AtomicReference<>();
        PasswordResetService reset = new PasswordResetService(repo,(email,value)->code.set(value));
        String token = reset.request(user.getEmail());
        fails(()->reset.reset(token,"NewPass1!".toCharArray(),"NewPass1!".toCharArray()));
        fails(()->reset.verify(token,"bad"));
        reset.verify(token,code.get());
        fails(()->reset.reset(token,"weak".toCharArray(),"weak".toCharArray()));
        reset.reset(token,"NewPass1!".toCharArray(),"NewPass1!".toCharArray());
        if (!PasswordUtil.verifyPassword("NewPass1!", user.getPasswordHash()) || PasswordUtil.verifyPassword("OldPass1!",user.getPasswordHash())) throw new AssertionError();
        fails(()->reset.verify(token,code.get()));
        PasswordResetService locked = new PasswordResetService(repo,(email,value)->code.set(value));
        String lockToken = locked.request(user.getEmail());
        for(int i=0;i<5;i++) fails(()->locked.verify(lockToken,"000000"));
        fails(()->locked.verify(lockToken,code.get()));
        final long[] now = {0};
        Clock clock = new Clock() { public ZoneId getZone(){return ZoneOffset.UTC;} public Clock withZone(ZoneId z){return this;} public Instant instant(){return Instant.ofEpochMilli(now[0]);} };
        PasswordResetService expired = new PasswordResetService(repo,(email,value)->code.set(value),clock);
        String old = expired.request(user.getEmail()); now[0]=600001;
        fails(()->expired.verify(old,code.get()));
        java.util.concurrent.atomic.AtomicInteger sends = new java.util.concurrent.atomic.AtomicInteger();
        PasswordResetService delivery = new PasswordResetService(repo,(email,value)-> {
            if(sends.getAndIncrement()==0) throw new Exception("delivery unavailable");
            code.set(value);
        });
        try { delivery.request(user.getEmail()); throw new AssertionError(); } catch(Exception expected) {}
        String retry = delivery.request(user.getEmail()); delivery.verify(retry,code.get());
        final gui.auth.ForgotPasswordPanel[] ui = new gui.auth.ForgotPasswordPanel[1];
        PasswordResetService uiService = new PasswordResetService(repo,(email,value)->code.set(value));
        javax.swing.SwingUtilities.invokeAndWait(()->{
            ui[0] = new gui.auth.ForgotPasswordPanel(uiService,()->{});
            ((javax.swing.JTextField)get(ui[0],"email")).setText(user.getEmail());
            ((javax.swing.JButton)get(ui[0],"action")).doClick();
        });
        waitStage(ui[0],1);
        javax.swing.SwingUtilities.invokeAndWait(()->{
            gui.components.VerificationCodeInput input = (gui.components.VerificationCodeInput)get(ui[0],"code");
            ((javax.swing.JTextField)input.getComponent(0)).setText(code.get());
        });
        javax.swing.SwingUtilities.invokeAndWait(()->{
            if(!((gui.components.VerificationCodeInput)get(ui[0],"code")).getText().equals(code.get())) throw new AssertionError("Paste failed");
            ui[0].setSize(1053,710);layout(ui[0]);
            java.awt.image.BufferedImage image = new java.awt.image.BufferedImage(1053,710,java.awt.image.BufferedImage.TYPE_INT_RGB);
            java.awt.Graphics2D g=image.createGraphics();ui[0].printAll(g);g.dispose();
            try {javax.imageio.ImageIO.write(image,"png",new java.io.File("bin/review/forgot-password-verify.png"));}catch(Exception ex){throw new RuntimeException(ex);}
            ((javax.swing.JButton)get(ui[0],"action")).doClick();
        });
        waitStage(ui[0],2);
        javax.swing.SwingUtilities.invokeAndWait(()->{
            gui.auth.ForgotPasswordPanel panel = new gui.auth.ForgotPasswordPanel(reset,()->{});
            panel.setSize(1080,720); layout(panel);
            java.awt.image.BufferedImage image = new java.awt.image.BufferedImage(1080,720,java.awt.image.BufferedImage.TYPE_INT_RGB);
            java.awt.Graphics2D g=image.createGraphics();panel.printAll(g);g.dispose();
            try { java.nio.file.Files.createDirectories(java.nio.file.Path.of("bin/review"));javax.imageio.ImageIO.write(image,"png",new java.io.File("bin/review/forgot-password.png")); }catch(Exception e){throw new RuntimeException(e);}
        });
        System.out.println("Password reset checks passed");
    }
    static Object get(Object target,String name) {try {java.lang.reflect.Field f=target.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(target);}catch(Exception e){throw new RuntimeException(e);}}
    static void waitStage(gui.auth.ForgotPasswordPanel ui,int expected) throws Exception {
        for(int i=0;i<100;i++) {final boolean[] ready={false};javax.swing.SwingUtilities.invokeAndWait(()->ready[0]=get(ui,"stage").equals(expected));if(ready[0])return;Thread.sleep(20);}
        throw new AssertionError("UI did not advance to " + expected);
    }
    static void layout(java.awt.Container c){c.doLayout();for(java.awt.Component child:c.getComponents())if(child instanceof java.awt.Container)layout((java.awt.Container)child);}
}
