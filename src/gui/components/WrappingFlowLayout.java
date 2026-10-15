package gui.components;
import java.awt.*;
/** Flow rows reserve their full height when the window becomes narrower. */
public final class WrappingFlowLayout extends FlowLayout {
    public WrappingFlowLayout(){super(FlowLayout.LEFT,8,4);}
    @Override public Dimension preferredLayoutSize(Container target){synchronized(target.getTreeLock()){
        int available=target.getWidth();if(available<=0&&target.getParent()!=null)available=target.getParent().getWidth();if(available<=0)available=900;
        Insets in=target.getInsets();int limit=Math.max(1,available-in.left-in.right-2*getHgap());int x=0,rowHeight=0,height=2*getVgap(),width=0;
        for(Component c:target.getComponents())if(c.isVisible()){Dimension d=c.getPreferredSize();if(x>0&&x+getHgap()+d.width>limit){height+=rowHeight+getVgap();width=Math.max(width,x);x=0;rowHeight=0;}x+=(x==0?0:getHgap())+d.width;rowHeight=Math.max(rowHeight,d.height);}
        return new Dimension(Math.max(width,x)+in.left+in.right+2*getHgap(),height+rowHeight+in.top+in.bottom);
    }}
    @Override public Dimension minimumLayoutSize(Container target){Dimension d=preferredLayoutSize(target);d.width=0;return d;}
}
