package gui.auth;

import javax.swing.*;
import model.User;
import service.AuthService;

public final class TermsFrame extends JFrame {
    private final char[] returnPassword;
    public TermsFrame(AuthService auth,User user,Runnable next) {
        this(auth,user,next,new char[0]);
    }
    public TermsFrame(AuthService auth,User user,Runnable next,char[] password) {
        returnPassword=password.clone();
        java.util.Arrays.fill(password,'\0');
        setTitle("QueueTees • "+(user.getRole()==model.UserRole.STAFF?"Staff":"Customer")+" Terms & Conditions");gui.components.AppTheme.applyWindowIcon(this);
        setDefaultCloseOperation(EXIT_ON_CLOSE);setMinimumSize(new java.awt.Dimension(760,600));setSize(950,760);setLocationRelativeTo(null);
        setContentPane(new TermsPanel(auth,user,()->{dispose();next.run();},()->{
            SignupFrame signup=new SignupFrame(auth,user,returnPassword);
            dispose();signup.setVisible(true);
        }));
    }
    @Override public void dispose() {
        if(returnPassword!=null)java.util.Arrays.fill(returnPassword,'\0');
        super.dispose();
    }
    public static void proceed(AuthService auth,User user,Runnable next) {
        proceed(auth,user,next,new char[0]);
    }
    public static void proceed(AuthService auth,User user,Runnable next,char[] password) {
        if(auth.hasAcceptedTerms(user)){
            java.util.Arrays.fill(password,'\0');next.run();
        } else new TermsFrame(auth,user,next,password).setVisible(true);
    }
}
