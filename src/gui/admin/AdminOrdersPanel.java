package gui.admin;
import java.awt.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import model.*;
import service.StoreService;

/** Read-only queue monitoring and completed-order sales, using actual session data. */
class AdminOrdersPanel extends JPanel {
    final DefaultTableModel model=new DefaultTableModel(new String[]{"Queue no.","Customer","Placed","Items","Total","Status"},0){public boolean isCellEditable(int r,int c){return false;}};
    final JTable table=new JTable(model);
    final boolean sales;
    private final JPanel metrics=new JPanel(new BorderLayout());
    private final JLabel footer=AdminUi.label("",11,false);
    AdminOrdersPanel(boolean sales,User user){
        this.sales=sales;setOpaque(false);setLayout(new BoxLayout(this,BoxLayout.Y_AXIS));
        add(AdminUi.label(sales?"REPORTS":"ORDERS",10,true));add(Box.createVerticalStrut(4));add(AdminUi.label(sales?"Sales reports":"Order queue monitoring",25,true));add(Box.createVerticalStrut(5));add(AdminUi.label(sales?"Completed order totals for the current session.":"Monitor confirmed orders as they move through the queue.",11,false));add(Box.createVerticalStrut(18));
        metrics.setOpaque(false);metrics.setAlignmentX(0);metrics.setMaximumSize(new Dimension(Integer.MAX_VALUE,104));add(metrics);add(Box.createVerticalStrut(16));
        AdminUi.style(table);add(AdminUi.filters(table,"All statuses","Confirmed","Preparing","Ready for pickup","Completed"));add(Box.createVerticalStrut(16));
        JPanel card=AdminUi.tableCard(table,sales?"Completed sales":"Live order queue");JPanel bottom=new JPanel(new BorderLayout());bottom.setOpaque(false);bottom.add(footer);JPanel actions=new JPanel(new FlowLayout(FlowLayout.RIGHT,10,0));actions.setOpaque(false);JButton refresh=AdminUi.button("Refresh");refresh.addActionListener(e->refresh());actions.add(refresh);
        if(sales){JButton print=AdminUi.button("Preview / print report");print.addActionListener(e->AdminTableReport.open(this,table,"Sales report",user));actions.add(print);}bottom.add(actions,BorderLayout.EAST);card.add(bottom,BorderLayout.SOUTH);add(card);refresh();
    }
    void refresh(){model.setRowCount(0);java.util.List<Order> all=StoreService.getInstance().getOrders();java.util.List<Order> rows=sales?StoreService.getInstance().getCompletedOrders():all;double total=0;for(Order o:rows){total+=o.getTotal();model.addRow(new Object[]{String.format("Q-%03d",o.getQueueNumber()),o.getCustomerName(),o.getPlacedAt().format(java.time.format.DateTimeFormatter.ofPattern("MMM d, HH:mm")),o.getItemCount(),String.format("₱%,.2f",o.getTotal()),o.getStatus().getLabel()});}metrics.removeAll();if(sales)metrics.add(AdminUi.metrics(String.format("₱%,.2f",total),"Completed order value",String.valueOf(rows.size()),"Completed orders",String.format("₱%,.2f",rows.isEmpty()?0:total/rows.size()),"Average order value"));else metrics.add(AdminUi.metrics(String.valueOf(all.stream().filter(o->o.getStatus()==OrderStatus.CONFIRMED).count()),"Waiting",String.valueOf(all.stream().filter(o->o.getStatus()==OrderStatus.PREPARING).count()),"Preparing",String.valueOf(all.stream().filter(o->o.getStatus()==OrderStatus.READY_FOR_PICKUP).count()),"Ready for pickup"));footer.setText(rows.size()+" orders | Current session");metrics.revalidate();metrics.repaint();}
}
