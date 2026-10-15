package gui.admin;
import java.awt.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import model.*;
import service.StoreService;

// order queeue monitoring and sales report page for administrators
class AdminOrdersPanel extends JPanel {
    final DefaultTableModel model=new DefaultTableModel(new String[]{"Control no.","Customer","Placed","Items","Total","Delivery point","Method","Status"},0){public boolean isCellEditable(int r,int c){return false;}};
    final JTable table=new JTable(model);
    final boolean sales;
    private final User reportUser;
    private final JPanel metrics=new JPanel(new BorderLayout());
    private final JLabel footer=AdminUi.label("",11,false);
    AdminOrdersPanel(boolean sales,User user){
        this.sales=sales;
        this.reportUser=user;
        if(sales)table.putClientProperty("report.details","Basis: verified payments on completed orders minus recorded refunds for those same orders. Unverified payments are excluded. Cancellation and lost-delivery refunds are listed separately in Order refunds.");
        if(sales)model.setColumnIdentifiers(new String[]{"Control no.","Customer","Completed","Items","Gross sales","Refunds","Net sales","Status"});

        setOpaque(false);

        setLayout(new BoxLayout(this,BoxLayout.Y_AXIS));
        
        add(AdminUi.label(sales?"REPORTS":"ORDERS",10,true));
        add(Box.createVerticalStrut(4));
        add(AdminUi.label(sales?"Sales reports":"Order queue monitoring",25,true));
        add(Box.createVerticalStrut(5));
        add(AdminUi.label(sales?"All-time completed-order cohort: original sales less recorded refunds against those same orders.":"Monitor confirmed orders as they move through the queue.",11,false));
        add(Box.createVerticalStrut(18));
        metrics.setOpaque(false);
        metrics.setAlignmentX(0);
        metrics.setMaximumSize(new Dimension(Integer.MAX_VALUE,104));
        add(metrics);
        add(Box.createVerticalStrut(16));
        
        AdminUi.style(table);add(AdminUi.filters(table,"All statuses","Confirmed","Preparing","Ready for pickup","Ready for dispatch","Transferring to delivery point","At delivery point","Out for delivery","Delivered / awaiting acknowledgment","Completed","Cancelled","Cancelled / refund pending","Lost","Lost / refund pending","Refunded","Partially refunded","Return under review","Return approved","Returned / awaiting refund","Partially returned / awaiting refund"));
        add(Box.createVerticalStrut(16));
        JPanel card=AdminUi.tableCard(table,sales?"Completed sales":"Live order queue");
        JPanel bottom=new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        bottom.add(footer);

        JPanel actions=new JPanel(new FlowLayout(FlowLayout.RIGHT,10,0));
        actions.setOpaque(false);
        JButton refresh=AdminUi.button("Refresh");
        refresh.addActionListener(e->refresh());
        actions.add(refresh);
        if(!sales){JButton verify=AdminUi.button("Verify payment");verify.setEnabled(false);
            table.getSelectionModel().addListSelectionListener(e->verify.setEnabled(table.getSelectedRow()>=0));
            verify.addActionListener(e->{int row=table.getSelectedRow();if(row<0)return;String control=String.valueOf(table.getValueAt(row,0));
                StoreService.getInstance().getOrders().stream().filter(o->o.getControlNumber().equals(control)).findFirst().ifPresent(o->gui.components.PaymentReview.show(this,o,reportUser,this::refresh));
            });actions.add(verify);}

        if(sales){JButton print=AdminUi.button("Preview / print report");

        print.addActionListener(e->AdminTableReport.open(this,table,"Sales report",user));
        actions.add(print);}bottom.add(actions,BorderLayout.EAST);card.add(bottom,BorderLayout.SOUTH);add(card);refresh();
    }
    void refresh(){model.setRowCount(0);java.util.List<Order> all=StoreService.getInstance().getOrders();
        java.util.List<Order> rows=sales?all.stream().filter(o->o.getStatus()==OrderStatus.COMPLETED&&o.getPaymentStatus()==PaymentStatus.VERIFIED).toList():all;
        java.util.List<RefundRecord> refunds=reportUser==null?java.util.List.of():service.RefundService.records(reportUser);
        java.util.List<ReturnRequest> returns=reportUser==null?java.util.List.of():service.ReturnsService.requests(reportUser);
        for(Order o:rows){double received=sales?o.getPaidAmount():o.getTotal();double paidRefund=service.RefundService.totalPaid(o.getId(),refunds);
            if(sales)model.addRow(new Object[]{o.getControlNumber(),o.getCustomerName(),o.getCompletedAt().format(java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy HH:mm")),o.getItemCount(),String.format("₱%,.2f",received),String.format("₱%,.2f",paidRefund),String.format("₱%,.2f",received-paidRefund),ReturnPresentation.history(o,returns)+(o.getPaymentStatus()!=PaymentStatus.VERIFIED?" / payment unverified":"")});
            else model.addRow(new Object[]{o.getControlNumber(),o.getCustomerName(),o.getPlacedAt().format(java.time.format.DateTimeFormatter.ofPattern("MMM d, HH:mm")),o.getItemCount(),String.format("₱%,.2f",o.getTotal()),o.getCheckoutDetails().getDeliveryPoint().getLabel()+" • "+WarehouseDistance.label(o),o.getCheckoutDetails().getFulfillmentMethod()+" / "+o.getPaymentStatus(),service.RefundService.outcome(o,refunds)});}
            metrics.removeAll();
            if(sales){SalesSummary totals=SalesSummary.from(all,refunds);metrics.add(AdminUi.metrics(String.format("₱%,.2f",totals.grossSales()),"Gross sales",String.format("₱%,.2f",totals.recordedRefunds()),"Recorded refunds",String.format("₱%,.2f",totals.netSales()),"Net sales"));}
            else metrics.add(AdminUi.metrics(String.valueOf(all.stream().filter(o->o.getStatus()==OrderStatus.CONFIRMED).count()),"Waiting",String.valueOf(all.stream().filter(o->o.getStatus()==OrderStatus.PREPARING).count()),"Preparing",String.valueOf(all.stream().filter(o->!service.RefundService.terminal(o)&&o.getStatus()!=OrderStatus.CONFIRMED&&o.getStatus()!=OrderStatus.PREPARING).count()),"Ready / Delivery"));
            footer.setText(rows.size()+" orders | "+(sales?"All-time completed-order cohort; cancellation/loss refunds are listed separately in Order refunds":"All stored orders"));
            metrics.revalidate();
            metrics.repaint();}
}
