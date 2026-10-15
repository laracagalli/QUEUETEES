package gui.components;
import java.awt.*;
import javax.swing.*;
/** Consistent action meaning and readable disabled states. */
public final class ActionButtons {
    public static final Color GREEN=new Color(35,105,62),RED=new Color(174,45,45),BLUE=new Color(36,88,149),AMBER=new Color(139,88,15),NEUTRAL=new Color(60,70,65);
    private ActionButtons(){}
    public static RoundedButton create(String text,Color color){RoundedButton b=new RoundedButton(text,color,Color.WHITE);b.setFont(new Font("Segoe UI",Font.BOLD,13));b.setPreferredSize(new Dimension(b.getFontMetrics(b.getFont()).stringWidth(text)+36,40));return b;}
}
