package gui.components;

import java.awt.event.*;
import javax.swing.*;
import model.*;
import service.*;

/** Deliver admin notices in the user's own window, including across app instances. */
public final class AccountNotices {
    private AccountNotices() { }
    public static void attach(JFrame frame,AuthService auth,User user) {
        Timer timer=new Timer(5000,null);
        boolean[] busy={false};
        timer.addActionListener(e->{
            if(!frame.isShowing() || busy[0])return;
            busy[0]=true;
            new SwingWorker<Snapshot,Void>() {
                protected Snapshot doInBackground(){
                    AccountStatus status=auth.currentAccountStatus(user);
                    return new Snapshot(status,status==AccountStatus.ACTIVE ? (user.getRole()==UserRole.CUSTOMER ? SecurityIncidents.pendingPopupWarnings(user.getId()) : SecurityIncidents.pendingWarnings(user.getId())) : java.util.List.of());
                }
                protected void done(){
                    try{
                        Snapshot snapshot=get();
                        if(!frame.isShowing())return;
                        if(snapshot.status()!=AccountStatus.ACTIVE){
                            timer.stop();user.setStatus(snapshot.status());
                            // Close this session before showing the notice; login remains blocked.
                            frame.setVisible(false);
                            String message=snapshot.status()==AccountStatus.BANNED ? SecurityIncidents.PERMANENT_SUSPENSION_MESSAGE
                                    : snapshot.status()==AccountStatus.SUSPENDED ? "This account is suspended." : "Your account no longer has access. Please contact the administrator.";
                            JOptionPane.showMessageDialog(null,message,"Account suspended",JOptionPane.WARNING_MESSAGE);
                            frame.dispose();new gui.auth.LoginFrame(auth).setVisible(true);return;
                        }
                        if(!snapshot.warnings().isEmpty()){
                            JOptionPane.showMessageDialog(frame,SecurityIncidents.WARNING_MESSAGE,"Account usage warning",JOptionPane.WARNING_MESSAGE);
                            new SwingWorker<Void,Void>() {
                                protected Void doInBackground(){for(int id:snapshot.warnings()){
                                    if(user.getRole()==UserRole.CUSTOMER)SecurityIncidents.acknowledgePopup(user.getId(),id);
                                    else SecurityIncidents.acknowledgeWarning(user.getId(),id);
                                }return null;}
                                protected void done(){try{get();}catch(Exception ex){/* Keep unread notices for a later attempt. */}busy[0]=false;}
                            }.execute();return;
                        }
                    }catch(Exception ex){/* A connection interruption must not close an active session. */}
                    finally{if(!frame.isShowing())timer.stop();}
                    busy[0]=false;
                }
            }.execute();
        });
        frame.addWindowListener(new WindowAdapter(){
            public void windowOpened(WindowEvent e){timer.setInitialDelay(0);timer.start();}
            public void windowClosed(WindowEvent e){timer.stop();}
        });
    }
    private record Snapshot(AccountStatus status,java.util.List<Integer> warnings) { }
}
