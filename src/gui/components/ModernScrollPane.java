package gui.components;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import javax.swing.*;
/** Clips table headers, viewports and scrollbars to the same rounded outline. */
public class ModernScrollPane extends JScrollPane {
    public ModernScrollPane(){super();}
    public ModernScrollPane(Component view){super(view);}
    public ModernScrollPane(Component view,int vertical,int horizontal){super(view,vertical,horizontal);}
    @Override public void paint(Graphics graphics){Graphics2D g=(Graphics2D)graphics.create();g.clip(new RoundRectangle2D.Float(0,0,getWidth(),getHeight(),16,16));super.paint(g);g.dispose();}
}
