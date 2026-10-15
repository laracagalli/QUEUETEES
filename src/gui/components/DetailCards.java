package gui.components;

import java.awt.*;
import javax.swing.*;

/** Shared readable cards for order and return details. Values remain plain text. */
public final class DetailCards {
    private DetailCards(){}
    public static JLabel label(String text,int size,boolean bold){
        JLabel label=new JLabel(text);label.putClientProperty("html.disable",true);
        label.setFont(new Font("Segoe UI",bold?Font.BOLD:Font.PLAIN,size));label.setForeground(AppTheme.GREEN);return label;
    }
    public static JPanel column(){JPanel panel=new JPanel();panel.setLayout(new BoxLayout(panel,BoxLayout.Y_AXIS));panel.setOpaque(false);return panel;}
    public static void add(JPanel column,JComponent child){child.setAlignmentX(Component.LEFT_ALIGNMENT);child.setMaximumSize(new Dimension(Integer.MAX_VALUE,child.getPreferredSize().height));column.add(child);column.add(Box.createVerticalStrut(12));}
    public static JPanel card(String title){
        JPanel card=new JPanel(new BorderLayout(0,10));card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(AppTheme.LINE),BorderFactory.createEmptyBorder(14,16,14,16)));
        card.add(label(title,13,true),BorderLayout.NORTH);return card;
    }
    public static JTextArea text(String value,int rows){
        JTextArea text=new JTextArea(value==null||value.isBlank()?"—":value,rows,22);
        text.setEditable(false);text.setOpaque(false);text.setLineWrap(true);text.setWrapStyleWord(true);
        text.setFont(new Font("Segoe UI",Font.PLAIN,14));text.setForeground(AppTheme.GREEN);text.setCaretPosition(0);return text;
    }
    public static JPanel field(String title,String value){
        JPanel field=new JPanel(new BorderLayout(0,4));field.setOpaque(false);
        JLabel caption=label(title.toUpperCase(java.util.Locale.ROOT),10,true);caption.setForeground(new Color(102,113,102));
        field.add(caption,BorderLayout.NORTH);field.add(text(value,Math.max(1,Math.min(8,(value==null?0:value.length()+43)/44))));return field;
    }
    public static JPanel grid(JComponent... cells){
        JPanel grid=new JPanel(new GridLayout(0,2,14,12));grid.setOpaque(false);for(JComponent cell:cells)grid.add(cell);return grid;
    }
    public static JPanel heading(String title,String status){
        JPanel panel=new JPanel(new BorderLayout(12,8));panel.setOpaque(false);panel.add(label(title,24,true));
        JLabel badge=label(status,12,true);badge.setOpaque(true);badge.setBackground(new Color(226,237,218));
        badge.setBorder(BorderFactory.createEmptyBorder(8,12,8,12));panel.add(badge,BorderLayout.EAST);return panel;
    }
    public static JPanel note(String title,String value){
        int lines=value==null?1:value.lines().mapToInt(line->Math.max(1,(line.length()+54)/55)).sum();
        int rows=Math.max(2,Math.min(6,lines));
        JPanel card=card(title);JTextArea text=text(value,rows);
        JScrollPane scroll=new ModernScrollPane(text,JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setPreferredSize(new Dimension(350,rows*20+4));scroll.setOpaque(false);scroll.getViewport().setOpaque(false);
        card.add(scroll);return card;
    }
    public static JScrollPane scroll(JPanel content,int width,int height){
        JPanel view=new WidthPanel();view.setOpaque(false);view.add(content,BorderLayout.NORTH);
        JScrollPane scroll=new ModernScrollPane(view,JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setPreferredSize(new Dimension(width,height));scroll.getVerticalScrollBar().setUnitIncrement(18);return scroll;
    }
    private static final class WidthPanel extends JPanel implements Scrollable {
        WidthPanel(){super(new BorderLayout());}
        public Dimension getPreferredScrollableViewportSize(){return getPreferredSize();}
        public int getScrollableUnitIncrement(Rectangle r,int orientation,int direction){return 18;}
        public int getScrollableBlockIncrement(Rectangle r,int orientation,int direction){return Math.max(18,r.height-18);}
        public boolean getScrollableTracksViewportWidth(){return true;}
        public boolean getScrollableTracksViewportHeight(){return false;}
    }
}
