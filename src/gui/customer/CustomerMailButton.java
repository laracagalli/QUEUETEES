package gui.customer;

import java.awt.*;
import javax.swing.*;
import model.User;
import service.SecurityIncidents;

/** Envelope with a live unread badge, shared by every customer dashboard. */
public final class CustomerMailButton extends JButton {
    private final User user;
    private int unread;
    private boolean loading;
    private final Timer timer=new Timer(5000,e->{if(isShowing())refreshCount();});
    public CustomerMailButton(User user){
        this.user=user;putClientProperty("queuetees.preserveButtonStyle",true);
        setPreferredSize(new Dimension(64,44));setMinimumSize(getPreferredSize());setMaximumSize(getPreferredSize());
        setBorder(BorderFactory.createEmptyBorder());setContentAreaFilled(false);setOpaque(false);setFocusPainted(false);setRolloverEnabled(true);setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setUnreadCount(0);
        addActionListener(e->{AdminInboxPanel inbox=new AdminInboxPanel(user,this::setUnreadCount);
            JOptionPane.showMessageDialog(this,inbox,"Messages from admin",JOptionPane.PLAIN_MESSAGE);refreshCount();});
        refreshCount();
    }
    @Override public void addNotify(){super.addNotify();timer.start();}
    @Override public void removeNotify(){timer.stop();super.removeNotify();}
    public void setUnreadCount(int count){unread=Math.max(0,count);String label="Messages from admin • "+unread+" unread";setToolTipText(label);getAccessibleContext().setAccessibleName(label);repaint();}
    public int getUnreadCount(){return unread;}
    public void refreshCount(){
        if(loading)return;loading=true;
        new SwingWorker<Integer,Void>(){
            protected Integer doInBackground(){return SecurityIncidents.pendingWarnings(user.getId()).size();}
            protected void done(){try{setUnreadCount(get());}catch(Exception ex){setToolTipText("Messages from admin • unable to refresh; click to retry");}finally{loading=false;}}
        }.execute();
    }
    @Override protected void paintComponent(Graphics graphics){
        Graphics2D g=(Graphics2D)graphics.create();g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(getModel().isRollover() || getModel().isPressed()?new Color(217,226,209):new Color(235,239,229));g.fillRoundRect(2,3,54,38,14,14);
        g.setColor(gui.components.AppTheme.GREEN);g.setStroke(new BasicStroke(2));g.drawRoundRect(14,14,28,19,4,4);g.drawLine(15,15,28,25);g.drawLine(28,25,41,15);
        if(hasFocus()){g.setStroke(new BasicStroke(1));g.drawRoundRect(1,2,56,40,14,14);}
        if(unread>0){String text=unread>99?"99+":String.valueOf(unread);g.setFont(new Font("Segoe UI",Font.BOLD,11));int width=Math.max(20,g.getFontMetrics().stringWidth(text)+10);int x=getWidth()-width;
            g.setColor(new Color(174,45,45));g.fillRoundRect(x,0,width,20,20,20);g.setColor(Color.WHITE);g.drawString(text,x+(width-g.getFontMetrics().stringWidth(text))/2,14);}
        g.dispose();
    }
}
