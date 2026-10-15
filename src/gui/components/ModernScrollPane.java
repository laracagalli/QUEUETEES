package gui.components;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import javax.swing.*;
/** Clips table headers, viewports and scrollbars to the same rounded outline. */
public class ModernScrollPane extends JScrollPane {
    private JPanel paginationControls;
    private int paginationHeight(){return paginationControls==null?0:Math.max(48,paginationControls.getPreferredSize().height);}
    private Insets baseInsets(){return super.getInsets();}
    @Override public Insets getInsets(){Insets in=super.getInsets();in.bottom+=paginationHeight();return in;}
    @Override public Insets getInsets(Insets in){super.getInsets(in);in.bottom+=paginationHeight();return in;}
    public ModernScrollPane(){super();}
    public ModernScrollPane(Component view){super(view);paginate(view);}
    public ModernScrollPane(Component view,int vertical,int horizontal){super(view,vertical,horizontal);paginate(view);}
    private void paginate(Component view) {
        if(!(view instanceof JTable table))return;
        JPanel controls=TablePagination.install(table);if(controls==null)return;
        paginationControls=controls;
        setLayout(new ScrollPaneLayout(){
            @Override public void addLayoutComponent(String name,Component child){if(!"pagination".equals(name))super.addLayoutComponent(name,child);}
            @Override public void layoutContainer(Container parent){
                // Reserve the footer before calculating the viewport and scrollbar range.
                super.layoutContainer(parent);Insets in=baseInsets();int height=paginationHeight();
                controls.setBounds(in.left,Math.max(in.top,parent.getHeight()-in.bottom-height),Math.max(0,parent.getWidth()-in.left-in.right),height);
            }
        });
        add(controls,"pagination");
    }
    @Override public void paint(Graphics graphics){Graphics2D g=(Graphics2D)graphics.create();g.clip(new RoundRectangle2D.Float(0,0,getWidth(),getHeight(),16,16));super.paint(g);g.dispose();}
}
