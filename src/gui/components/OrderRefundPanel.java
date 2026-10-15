package gui.components;
import model.*;import service.*;import javax.swing.*;import javax.swing.table.DefaultTableModel;import java.awt.*;import java.util.*;

/** Lost-parcel and cancellation refunds; returned-item payments stay in return review. */
public final class OrderRefundPanel extends JPanel {
    private final User user;
    private final DefaultTableModel model=new DefaultTableModel(new String[]{"Order","Reason","Amount","Status","Payment reference"},0){public boolean isCellEditable(int r,int c){return false;}};
    private final JTable table=new JTable(model){@Override protected void paintComponent(Graphics g){super.paintComponent(g);if(getRowCount()==0){g.setColor(new Color(65,80,68));g.setFont(new Font("Segoe UI",Font.PLAIN,13));g.drawString("No cancelled or lost-order refunds. Returned-item refunds use the main Returns table.",20,45);}}};
    private java.util.List<RefundRecord> rows=java.util.List.of();
    public OrderRefundPanel(User user){
        super(new BorderLayout(0,12));this.user=user;setOpaque(false);
        JPanel heading=new JPanel(new GridLayout(2,1,0,6));heading.setOpaque(false);JLabel title=new JLabel("Cancellation / lost-delivery refunds");title.setFont(new Font("Segoe UI",Font.BOLD,22));heading.add(title);heading.add(new JLabel("Amounts come from recorded prototype payments. Admin records paid / simulated refunds; no money transfer."));add(heading,BorderLayout.NORTH);
        table.setRowHeight(44);table.setFont(new Font("Segoe UI",Font.PLAIN,13));table.setFillsViewportHeight(true);table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        ModernScrollPane scroll=new ModernScrollPane(table);scroll.setColumnHeaderView(table.getTableHeader());add(scroll);
        JButton refresh=ActionButtons.create("Refresh",ActionButtons.NEUTRAL),details=ActionButtons.create("View refund details",ActionButtons.BLUE),pay=ActionButtons.create(user.getRole()==UserRole.ADMIN?"Record refund payment":"Admin records refund",ActionButtons.BLUE);JPanel actions=new JPanel(new WrappingFlowLayout());actions.setOpaque(false);actions.add(refresh);actions.add(details);actions.add(pay);pay.setToolTipText("Only administrators can record payment for a pending refund.");add(actions,BorderLayout.SOUTH);
        refresh.addActionListener(e->refresh());details.addActionListener(e->{RefundRecord r=selected();if(r!=null)JOptionPane.showMessageDialog(this,"Reason: "+r.kind()+"\nAmount: "+String.format("₱%,.2f",r.amount())+"\nStatus: "+r.status()+"\nRequested: "+r.requestedAt()+"\nPaid: "+(r.paidAt()==null?"Not issued":r.paidAt())+"\nReference: "+r.reference()+"\nNotes: "+r.note(),"Refund details",JOptionPane.INFORMATION_MESSAGE);});
        pay.setEnabled(false);details.setEnabled(false);table.getSelectionModel().addListSelectionListener(e->{details.setEnabled(selected()!=null);pay.setEnabled(user.getRole()==UserRole.ADMIN&&selected()!=null&&selected().status().equals("PENDING"));});
        pay.addActionListener(e->{RefundRecord r=selected();if(r==null)return;JTextField reference=new JTextField();JTextArea note=new JTextArea(3,30);JPanel form=new JPanel(new GridLayout(0,1,0,6));form.add(new JLabel("Refund: "+String.format("₱%,.2f",r.amount())));form.add(new JLabel("Payment reference"));form.add(reference);form.add(new JLabel("Payment method / recipient and confirmation note"));form.add(new ModernScrollPane(note));if(JOptionPane.showConfirmDialog(this,form,"Record refund",JOptionPane.OK_CANCEL_OPTION)!=JOptionPane.OK_OPTION)return;try{RefundService.recordOrderRefund(r.source(),reference.getText(),note.getText(),user);refresh();}catch(RuntimeException ex){JOptionPane.showMessageDialog(this,ex.getMessage(),"Refund",JOptionPane.WARNING_MESSAGE);}});
        refresh();
    }
    private RefundRecord selected(){int row=table.getSelectedRow();return row<0?null:rows.get(table.convertRowIndexToModel(row));}
    private void refresh(){rows=RefundService.records(user).stream().filter(r->r.source().startsWith("ORDER:")).toList();model.setRowCount(0);java.util.List<Order> orders=StoreService.getInstance().getOrders();for(RefundRecord r:rows){String number=orders.stream().filter(o->o.getId()==r.orderId()).findFirst().orElseThrow().getControlNumber();model.addRow(new Object[]{number,r.kind(),String.format("₱%,.2f",r.amount()),r.status().equals("PAID")?"Refunded":"Refund pending",r.reference().isBlank()?"Not issued":r.reference()});}}
    public static void show(Component parent,User user){OrderRefundPanel panel=new OrderRefundPanel(user);panel.setPreferredSize(new Dimension(880,500));AppTheme.apply(panel);JOptionPane.showMessageDialog(parent,panel,"Order refunds",JOptionPane.PLAIN_MESSAGE);}
}
