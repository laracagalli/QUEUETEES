package gui.components;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import model.User;

/** Read-only account details from the user already signed in. */
public final class ProfileCard extends JPanel {
    private static final Color GREEN = new Color(55,70,56);
    public ProfileCard(User user) {
        setOpaque(false);
        setAlignmentX(LEFT_ALIGNMENT);
        setLayout(new BorderLayout(0,24));
        setBorder(new EmptyBorder(26,28,26,28));
        setMaximumSize(new Dimension(Integer.MAX_VALUE,340));
        setPreferredSize(new Dimension(800,340));
        if (user == null) {
            add(label("No account is signed in. Please return to login.",13,false));
            return;
        }
        JPanel heading = new JPanel(new BorderLayout(18,0));heading.setOpaque(false);
        String username = user.getUsername();
        JLabel avatar = label(username == null || username.isBlank() ? "?" : username.substring(0,1).toUpperCase(java.util.Locale.ROOT),24,true);
        avatar.setHorizontalAlignment(SwingConstants.CENTER);avatar.setOpaque(true);avatar.setBackground(new Color(228,235,221));avatar.setPreferredSize(new Dimension(62,62));
        heading.add(avatar,BorderLayout.WEST);
        JPanel identity = new JPanel(new GridLayout(2,1,0,6));identity.setOpaque(false);
        identity.add(label(username,20,true));identity.add(label(friendly(user.getRole().name())+" account",11,false));heading.add(identity);
        add(heading,BorderLayout.NORTH);
        JPanel details = new JPanel(new GridLayout(3,2,30,18));details.setOpaque(false);
        field(details,"Username",username);field(details,"Email address",user.getEmail());
        field(details,"Role",friendly(user.getRole().name()));field(details,"Account status",friendly(user.getStatus().name()));
        field(details,"Email verification",user.isEmailVerified()?"Verified":"Not verified");field(details,"Account ID",String.valueOf(user.getId()));
        add(details);
    }
    private static void field(JPanel parent,String name,String value) {
        JPanel cell=new JPanel(new GridLayout(2,1,0,5));cell.setOpaque(false);
        JLabel title=label(name,11,false);title.setForeground(new Color(99,106,96));cell.add(title);
        JLabel content=label(value,12,true);content.setToolTipText(value);cell.add(content);parent.add(cell);
    }
    private static JLabel label(String value,int size,boolean bold) {
        JLabel label=new JLabel(value);label.putClientProperty("html.disable",true);label.setFont(new Font("Fira Code",bold?Font.BOLD:Font.PLAIN,size));label.setForeground(GREEN);return label;
    }
    private static String friendly(String value) {String s=value.toLowerCase(java.util.Locale.ROOT).replace('_',' ');return Character.toUpperCase(s.charAt(0))+s.substring(1);}
    @Override protected void paintComponent(Graphics graphics) {
        Graphics2D g=(Graphics2D)graphics.create();g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(252,252,247));g.fillRoundRect(0,0,getWidth()-1,getHeight()-1,22,22);
        g.setColor(new Color(218,220,209));g.drawRoundRect(0,0,getWidth()-1,getHeight()-1,22,22);g.dispose();super.paintComponent(graphics);
    }
}
