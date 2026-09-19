package gui.components;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import javax.swing.*;
import javax.swing.border.AbstractBorder;
import javax.swing.plaf.basic.*;
import javax.swing.table.*;
import javax.swing.text.JTextComponent;

/** Shared styling applied to existing and dynamically created Swing controls. */
public final class AppTheme {
    public static final Color PAPER=new Color(252,252,247), GREEN=new Color(55,70,56), LINE=new Color(210,219,203);
    private static boolean installed;
    private AppTheme(){}
    public static void install(){
        if(installed)return;installed=true;
        UIManager.put("OptionPane.background",PAPER);UIManager.put("Panel.background",PAPER);
        UIManager.put("OptionPane.messageForeground",GREEN);
        UIManager.put("OptionPane.messageFont",new Font("Segoe UI",Font.PLAIN,13));
        UIManager.put("OptionPane.buttonFont",new Font("Segoe UI",Font.BOLD,12));
        UIManager.put("OptionPane.border",BorderFactory.createEmptyBorder(22,24,18,24));
        UIManager.put("OptionPane.minimumSize",new Dimension(390,150));
        for(String type:new String[]{"information","question","warning","error"})UIManager.put("OptionPane."+type+"Icon",new NoticeIcon(type));
        Toolkit.getDefaultToolkit().addAWTEventListener(event->{
            if(event instanceof ContainerEvent && event.getID()==ContainerEvent.COMPONENT_ADDED){
                Component child=((ContainerEvent)event).getChild();SwingUtilities.invokeLater(()->apply(child));
            }else if(event instanceof WindowEvent && event.getID()==WindowEvent.WINDOW_OPENED)apply(((WindowEvent)event).getWindow());
        },AWTEvent.CONTAINER_EVENT_MASK|AWTEvent.WINDOW_EVENT_MASK);
    }
    public static void apply(Component component){
        if(component instanceof JComponent){
            JComponent c=(JComponent)component;
            if(!Boolean.TRUE.equals(c.getClientProperty("queuetees.themed"))){
                c.putClientProperty("queuetees.themed",true);
                if(c instanceof JTextComponent){
                    JTextComponent text=(JTextComponent)c;
                    if(text.isEditable() || text instanceof JTextField){
                        text.setBackground(text.isEditable()?Color.WHITE:new Color(239,242,233));text.setForeground(GREEN);text.setCaretColor(GREEN);text.setSelectionColor(new Color(219,232,209));text.setDisabledTextColor(new Color(120,129,114));
                        text.setOpaque(false);
                        if(text instanceof JPasswordField)text.setUI(new BasicPasswordFieldUI(){protected void paintSafely(Graphics g){paintField(g,getComponent());super.paintSafely(g);}});
                        else if(text instanceof JTextField)text.setUI(new BasicTextFieldUI(){protected void paintSafely(Graphics g){paintField(g,getComponent());super.paintSafely(g);}});
                        else if(text instanceof JTextArea)text.setUI(new BasicTextAreaUI(){protected void paintSafely(Graphics g){paintField(g,getComponent());super.paintSafely(g);}});
                        text.setBorder(new FieldBorder());
                        if(text instanceof JTextArea && SwingUtilities.getAncestorOfClass(JScrollPane.class,text)!=null){
                            text.setUI(new BasicTextAreaUI());
                            text.setOpaque(true);
                            text.setBorder(BorderFactory.createEmptyBorder(2,4,2,4));
                        }
                        if(SwingUtilities.getAncestorOfClass(JSpinner.class,text)!=null)text.setBorder(BorderFactory.createEmptyBorder(2,7,2,7));
                        text.addFocusListener(new FocusAdapter(){public void focusGained(FocusEvent e){repaintField(text);}public void focusLost(FocusEvent e){repaintField(text);}});
                    }
                }
                if(c instanceof JSpinner){JSpinner spinner=(JSpinner)c;spinner.setUI(new DateSpinnerUI());spinner.getEditor().setOpaque(false);spinner.setBorder(new FieldBorder(){public Insets getBorderInsets(Component c){return new Insets(1,3,1,3);}public Insets getBorderInsets(Component c,Insets i){i.set(1,3,1,3);return i;}});spinner.setOpaque(false);}
                if(c instanceof JScrollBar){JScrollBar bar=(JScrollBar)c;bar.setUI(new ScrollUI());bar.setPreferredSize(new Dimension(10,10));bar.setUnitIncrement(20);}
                if(c instanceof JTable){JTable table=(JTable)c;table.setShowVerticalLines(false);table.setGridColor(new Color(228,233,222));table.setIntercellSpacing(new Dimension(0,1));styleHeader(table);}
                if(c instanceof JScrollPane){
                    JScrollPane scroll=(JScrollPane)c;scroll.setBorder(BorderFactory.createEmptyBorder());
                    Component view=scroll.getViewport().getView();
                    if(view instanceof JTable)scroll.setColumnHeaderView(((JTable)view).getTableHeader());
                    if(view instanceof JTextArea && ((JTextArea)view).isEditable()){
                        scroll.setBorder(new FieldBorder(){
                            public Insets getBorderInsets(Component c){return new Insets(4,8,4,8);}
                            public Insets getBorderInsets(Component c,Insets i){i.set(4,8,4,8);return i;}
                        });
                        scroll.setBackground(Color.WHITE);scroll.getViewport().setBackground(Color.WHITE);
                        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
                    }
                }
                if(c instanceof JButton && !Boolean.TRUE.equals(c.getClientProperty("queuetees.preserveButtonStyle")) && !(c instanceof RoundedButton) && !(c instanceof OutlineButton) && !Boolean.TRUE.equals(c.getClientProperty("queuetees.internalArrow"))){
                    JButton b=(JButton)c;
                    if(!b.getClass().getSimpleName().endsWith("NavButton") && !insideCalendar(b) && !(b.getParent() instanceof JComboBox) && !(b.getParent() instanceof JScrollBar) && !(b.getParent() instanceof JSpinner)){
                        b.setUI(new PillUI());b.setOpaque(false);b.setContentAreaFilled(false);b.setBorderPainted(false);b.setBorder(BorderFactory.createEmptyBorder(9,18,9,18));b.setForeground(Color.WHITE);b.setBackground(new Color(28,31,27));b.setRolloverEnabled(true);b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                    }
                }
                if(c instanceof com.toedter.calendar.JDateChooser){c.setOpaque(false);}
                if(c instanceof JButton && insideCalendar(c)){JButton b=(JButton)c;b.setUI(new BasicButtonUI());b.setBackground(PAPER);b.setForeground(GREEN);b.setBorder(BorderFactory.createEmptyBorder(4,6,4,6));}
                if(c instanceof JOptionPane){JOptionPane pane=(JOptionPane)c;pane.setBackground(PAPER);}
                if(c instanceof JCheckBox || c instanceof JRadioButton){AbstractButton b=(AbstractButton)c;b.setOpaque(false);b.setForeground(GREEN);b.setIcon(new ChoiceIcon(b,false));b.setSelectedIcon(new ChoiceIcon(b,true));}
            }
        }
        if(component instanceof Container)for(Component child:((Container)component).getComponents())apply(child);
    }
    public static void styleHeader(JTable table){
        JTableHeader header=table.getTableHeader();header.setOpaque(true);header.setBackground(new Color(232,238,226));header.setBorder(BorderFactory.createEmptyBorder());header.setPreferredSize(new Dimension(100,44));
        header.setDefaultRenderer(new DefaultTableCellRenderer(){
            public Component getTableCellRendererComponent(JTable t,Object value,boolean selected,boolean focus,int row,int column){
                super.getTableCellRendererComponent(t,value,selected,focus,row,column);setFont(header.getFont().deriveFont(Font.BOLD,12f));setBackground(new Color(232,238,226));setForeground(GREEN);setHorizontalAlignment(LEFT);setBorder(BorderFactory.createEmptyBorder(10,14,10,14));
                String suffix="";if(t.getRowSorter()!=null)for(RowSorter.SortKey key:t.getRowSorter().getSortKeys())if(key.getColumn()==t.convertColumnIndexToModel(column)){suffix=key.getSortOrder()==SortOrder.ASCENDING?"  ↑":key.getSortOrder()==SortOrder.DESCENDING?"  ↓":"";break;}
                setText(String.valueOf(value)+suffix);return this;
            }
        });
    }
    private static void repaintField(JTextComponent text){
        text.repaint();
        Container scroll=SwingUtilities.getAncestorOfClass(JScrollPane.class,text);
        if(scroll!=null)scroll.repaint();
    }
    private static boolean fieldFocused(Component c){
        if(c instanceof JScrollPane){Component view=((JScrollPane)c).getViewport().getView();return view!=null && view.hasFocus();}
        return c.hasFocus();
    }
    private static boolean insideCalendar(Component c){for(Component p=c;p!=null;p=p.getParent())if(p.getClass().getName().startsWith("com.toedter."))return true;return false;}
    private static void paintField(Graphics graphics,JTextComponent c){Graphics2D g=(Graphics2D)graphics.create();g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);g.setColor(c.getBackground());g.fillRoundRect(1,1,c.getWidth()-3,c.getHeight()-3,14,14);g.dispose();}
    private static class DateSpinnerUI extends BasicSpinnerUI{
        protected Component createNextButton(){JButton b=arrow(true);installNextButtonListeners(b);return b;}
        protected Component createPreviousButton(){JButton b=arrow(false);installPreviousButtonListeners(b);return b;}
        private JButton arrow(boolean up){JButton b=new JButton(){protected void paintComponent(Graphics graphics){Graphics2D g=(Graphics2D)graphics.create();g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);g.setColor(isEnabled()?GREEN:new Color(170,177,163));g.setStroke(new BasicStroke(1.5f));int x=getWidth()/2,y=getHeight()/2;g.drawLine(x-4,y+(up?2:-2),x,y+(up?-2:2));g.drawLine(x,y+(up?-2:2),x+4,y+(up?2:-2));g.dispose();}};b.putClientProperty("queuetees.internalArrow",true);b.setPreferredSize(new Dimension(22,14));b.setBorder(BorderFactory.createEmptyBorder());b.setOpaque(false);b.setContentAreaFilled(false);return b;}
    }
    private static class FieldBorder extends AbstractBorder{
        public Insets getBorderInsets(Component c){return new Insets(7,12,7,12);}
        public Insets getBorderInsets(Component c,Insets i){i.set(7,12,7,12);return i;}
        public void paintBorder(Component c,Graphics graphics,int x,int y,int width,int height){Graphics2D g=(Graphics2D)graphics.create();g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);Shape round=new RoundRectangle2D.Float(x+1,y+1,width-3,height-3,14,14);g.setColor(fieldFocused(c)?GREEN:LINE);g.setStroke(new BasicStroke(fieldFocused(c)?1.6f:1f));g.draw(round);g.dispose();}
    }
    private static class ScrollUI extends BasicScrollBarUI{
        protected JButton createDecreaseButton(int orientation){return zero();}protected JButton createIncreaseButton(int orientation){return zero();}
        private JButton zero(){JButton b=new JButton();b.putClientProperty("queuetees.internalArrow",true);b.setPreferredSize(new Dimension(0,0));b.setMinimumSize(new Dimension(0,0));b.setMaximumSize(new Dimension(0,0));return b;}
        protected void paintTrack(Graphics g,JComponent c,Rectangle r){g.setColor(PAPER);g.fillRect(r.x,r.y,r.width,r.height);}
        protected void paintThumb(Graphics graphics,JComponent c,Rectangle r){if(r.isEmpty()||!scrollbar.isEnabled())return;Graphics2D g=(Graphics2D)graphics.create();g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);g.setColor(isThumbRollover()?GREEN:new Color(156,172,148));g.fillRoundRect(r.x+2,r.y+2,Math.max(4,r.width-4),Math.max(4,r.height-4),8,8);g.dispose();}
    }
    private static class PillUI extends BasicButtonUI{
        public void paint(Graphics graphics,JComponent c){AbstractButton b=(AbstractButton)c;Graphics2D g=(Graphics2D)graphics.create();g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);g.setColor(!b.isEnabled()?new Color(158,166,153):b.getModel().isRollover()?GREEN:b.getBackground());g.fillRoundRect(0,0,c.getWidth(),c.getHeight(),24,24);if(b.hasFocus()){g.setColor(new Color(192,208,181));g.drawRoundRect(3,3,c.getWidth()-7,c.getHeight()-7,20,20);}g.dispose();super.paint(graphics,c);}
    }
    private static class ChoiceIcon implements Icon{
        final AbstractButton button;final boolean selected;ChoiceIcon(AbstractButton b,boolean s){button=b;selected=s;}public int getIconWidth(){return 18;}public int getIconHeight(){return 18;}
        public void paintIcon(Component c,Graphics graphics,int x,int y){Graphics2D g=(Graphics2D)graphics.create();g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);g.setColor(selected?GREEN:Color.WHITE);if(button instanceof JRadioButton)g.fillOval(x+1,y+1,16,16);else g.fillRoundRect(x+1,y+1,16,16,6,6);g.setColor(GREEN);if(button instanceof JRadioButton)g.drawOval(x+1,y+1,16,16);else g.drawRoundRect(x+1,y+1,16,16,6,6);if(selected){g.setColor(Color.WHITE);if(button instanceof JRadioButton)g.fillOval(x+6,y+6,6,6);else{g.setStroke(new BasicStroke(2));g.drawLine(x+4,y+9,x+7,y+12);g.drawLine(x+7,y+12,x+14,y+5);}}g.dispose();}
    }
    private static class NoticeIcon implements Icon{final String type;NoticeIcon(String t){type=t;}public int getIconWidth(){return 36;}public int getIconHeight(){return 36;}public void paintIcon(Component c,Graphics graphics,int x,int y){Graphics2D g=(Graphics2D)graphics.create();g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);g.setColor(new Color(230,237,223));g.fillOval(x,y,34,34);g.setColor(GREEN);g.setFont(new Font("Segoe UI",Font.BOLD,21));g.drawString(type.equals("question")?"?":type.equals("information")?"i":"!",x+12,y+25);g.dispose();}}
}
