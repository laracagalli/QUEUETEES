package gui.components;
import java.awt.*;
import javax.swing.*;
public final class ScrollableContent extends JPanel implements Scrollable {
    public ScrollableContent(Component view){super(new BorderLayout());setOpaque(false);add(view);}
    public Dimension getPreferredScrollableViewportSize(){return getPreferredSize();}
    public int getScrollableUnitIncrement(Rectangle r,int direction,int increment){return 24;}
    public int getScrollableBlockIncrement(Rectangle r,int direction,int increment){return Math.max(24,r.height-24);}
    public boolean getScrollableTracksViewportWidth(){return true;}
    public boolean getScrollableTracksViewportHeight(){return false;}
}
