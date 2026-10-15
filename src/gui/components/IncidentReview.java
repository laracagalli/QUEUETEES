package gui.components;

import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.*;
import model.*;
import service.SecurityIncidents;

public final class IncidentReview {
    private IncidentReview() { }
    public static void show(Component parent,User admin) {
        try{JOptionPane.showMessageDialog(parent,createPanel(admin),"Usage warnings",JOptionPane.PLAIN_MESSAGE);}
        catch(IllegalStateException ex){JOptionPane.showMessageDialog(parent,ex.getMessage(),"Unable to load warnings",JOptionPane.WARNING_MESSAGE);}
    }
    public static JPanel createPanel(User admin) {
        return new ReviewPanel(admin,SecurityIncidents.incidents(),SecurityIncidents.warnedIncidentIds());
    }
    private static final class ReviewPanel extends JPanel {
        private final java.util.List<SecurityIncidents.Incident> rows;
        private final Set<Integer> warned;
        private final DefaultTableModel model=new DefaultTableModel(new String[]{"User","Observed event","Date","Review status","Resolution","User warning"},0){public boolean isCellEditable(int r,int c){return false;}};
        private final JTable table=new JTable(model);
        private final JTextArea details=new JTextArea("Select a warning above to read the full observed event and review notes.");
        private final JLabel detailTitle=new JLabel("Warning details");
        private final JButton send=ActionButtons.create("Send warning",ActionButtons.GREEN),resolve=ActionButtons.create("Mark reviewed",ActionButtons.NEUTRAL);
        private final User admin;
        private static final DateTimeFormatter DATE=DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        ReviewPanel(User admin,java.util.List<SecurityIncidents.Incident> rows,Set<Integer> warned) {
            super(new BorderLayout(0,14));this.admin=admin;this.rows=rows;this.warned=new HashSet<>(warned);
            setBackground(AppTheme.PAPER);setPreferredSize(new Dimension(1120,590));
            JTextArea intro=new JTextArea("These observations require review; they are not automatic findings of malicious or illegal behavior.\nSelect a row to read the full details. Reporting an issue does not remove the user's account.");
            styleText(intro);intro.setRows(2);intro.setPreferredSize(new Dimension(900,42));add(intro,BorderLayout.NORTH);
            for(var r:rows)model.addRow(new Object[]{r.username(),r.reason(),DATE.format(r.reportedAt()),r.resolved()?"Reviewed":"Needs review",r.resolution().isBlank()?"—":r.resolution(),warned.contains(r.id())?"Sent":"Not sent"});
            table.setFont(new Font("Segoe UI",Font.PLAIN,13));table.setRowHeight(52);table.setAutoCreateRowSorter(true);
            table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);table.setFillsViewportHeight(true);
            table.setBackground(AppTheme.PAPER);table.setForeground(AppTheme.GREEN);
            table.setSelectionBackground(new Color(219,232,209));table.setSelectionForeground(new Color(25,45,29));
            table.setShowVerticalLines(false);table.setGridColor(AppTheme.LINE);table.getTableHeader().setReorderingAllowed(false);
            table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
            int[] widths={115,335,175,140,205,130};
            for(int c=0;c<widths.length;c++){table.getColumnModel().getColumn(c).setPreferredWidth(widths[c]);table.getColumnModel().getColumn(c).setMinWidth(c==1?180:widths[c]);}
            table.setDefaultRenderer(Object.class,new DefaultTableCellRenderer(){
                public Component getTableCellRendererComponent(JTable t,Object value,boolean selected,boolean focus,int row,int col){
                    super.getTableCellRendererComponent(t,value,selected,focus,row,col);
                    setHorizontalAlignment(SwingConstants.LEFT);setVerticalAlignment(SwingConstants.CENTER);
                    setBorder(new EmptyBorder(10,14,10,14));setFont(t.getFont());
                    setBackground(selected?t.getSelectionBackground():row%2==0?AppTheme.PAPER:new Color(245,248,240));
                    setForeground(selected?t.getSelectionForeground():AppTheme.GREEN);setToolTipText(String.valueOf(value));return this;
                }
            });
            AppTheme.styleHeader(table);
            ModernScrollPane scroll=new ModernScrollPane(table);scroll.setBorder(BorderFactory.createEmptyBorder());scroll.getViewport().setBackground(AppTheme.PAPER);add(scroll);
            styleText(details);details.setRows(5);details.getAccessibleContext().setAccessibleName("Full selected warning details");
            JScrollPane detailScroll=new JScrollPane(details);detailScroll.setPreferredSize(new Dimension(600,130));
            detailTitle.setFont(new Font("Segoe UI",Font.BOLD,13));
            JPanel detailPanel=new JPanel(new BorderLayout(0,8));detailPanel.setOpaque(false);detailPanel.add(detailTitle,BorderLayout.NORTH);detailPanel.add(detailScroll);
            JPanel actions=new JPanel(new WrappingFlowLayout());actions.setOpaque(false);actions.add(send);actions.add(resolve);
            JPanel bottom=new JPanel(new BorderLayout(0,10));bottom.setOpaque(false);bottom.add(detailPanel);bottom.add(actions,BorderLayout.SOUTH);add(bottom,BorderLayout.SOUTH);
            send.setEnabled(false);resolve.setEnabled(false);
            table.getSelectionModel().addListSelectionListener(e->{if(!e.getValueIsAdjusting())updateSelection();});
            send.addActionListener(e->sendWarning());resolve.addActionListener(e->resolveWarning());
        }
        private static void styleText(JTextArea text){
            text.setEditable(false);text.setLineWrap(true);text.setWrapStyleWord(true);text.setFont(new Font("Segoe UI",Font.PLAIN,13));
            text.setForeground(AppTheme.GREEN);text.setBackground(AppTheme.PAPER);text.setBorder(new EmptyBorder(0,0,0,0));
        }
        private int selected(){return table.getSelectedRow()<0?-1:table.convertRowIndexToModel(table.getSelectedRow());}
        private void updateSelection(){
            int index=selected();
            if(index<0){send.setEnabled(false);resolve.setEnabled(false);detailTitle.setText("Warning details");details.setText("Select a warning above to read the full observed event and review notes.");return;}
            var r=rows.get(index);boolean open="Needs review".equals(model.getValueAt(index,3));
            boolean canReview=admin!=null && admin.getRole()==UserRole.ADMIN && admin.getStatus()==AccountStatus.ACTIVE;
            send.setEnabled(canReview && open && r.userId()>0 && !warned.contains(r.id()));resolve.setEnabled(canReview && open);
            detailTitle.setText("Warning details • "+r.username());
            details.setText("User: "+r.username()+"\nDate: "+DATE.format(r.reportedAt())+"\nObserved event: "+r.reason()+"\nReview notes: "+model.getValueAt(index,4));details.setCaretPosition(0);
        }
        private void sendWarning(){
            int index=selected();if(index<0)return;var r=rows.get(index);
            JTextArea preview=new JTextArea("Send this warning to "+r.username()+"?\n\nObserved event: "+r.reason()+"\n\n"+SecurityIncidents.WARNING_MESSAGE);
            styleText(preview);preview.setColumns(55);preview.setRows(7);
            if(JOptionPane.showConfirmDialog(this,new JScrollPane(preview),"Send account warning",JOptionPane.YES_NO_OPTION,JOptionPane.WARNING_MESSAGE)!=JOptionPane.YES_OPTION)return;
            try{SecurityIncidents.sendWarning(admin,r.id());warned.add(r.id());model.setValueAt("Sent",index,5);updateSelection();
                JOptionPane.showMessageDialog(this,"Warning saved. The user will see it while signed in or on their next successful sign-in.","Warning sent",JOptionPane.INFORMATION_MESSAGE);
            }catch(IllegalArgumentException|IllegalStateException ex){JOptionPane.showMessageDialog(this,ex.getMessage(),"Unable to send warning",JOptionPane.WARNING_MESSAGE);}
        }
        private void resolveWarning(){
            int index=selected();if(index<0)return;var r=rows.get(index);
            String note=JOptionPane.showInputDialog(this,"Review result / explanation:");if(note==null)return;
            try{SecurityIncidents.resolve(admin,r.id(),note);model.setValueAt("Reviewed",index,3);model.setValueAt(note.trim(),index,4);updateSelection();}
            catch(IllegalArgumentException|IllegalStateException ex){JOptionPane.showMessageDialog(this,ex.getMessage(),"Unable to mark reviewed",JOptionPane.WARNING_MESSAGE);}
        }
    }
}
