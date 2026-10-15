package gui.customer;
import java.awt.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import model.*;
import gui.components.*;

public final class CustomerOrderDetails {
    private CustomerOrderDetails() { }
    public static void show(Component parent,User user,Order order){
        if(order.getCustomerId()!=user.getId())throw new IllegalStateException("This order belongs to another customer.");
        JDialog dialog=new JDialog(SwingUtilities.getWindowAncestor(parent),"Order "+order.getControlNumber(),Dialog.ModalityType.APPLICATION_MODAL);
        JPanel content=new JPanel(new BorderLayout(0,14));content.setBorder(BorderFactory.createEmptyBorder(20,20,20,20));
        java.time.format.DateTimeFormatter date=java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy / h:mm a");
        CheckoutDetails d=order.getCheckoutDetails();String facts="Control number: "+order.getControlNumber()+"\nPlaced: "+order.getPlacedAt().format(date)+"\nCompleted: "+(order.getCompletedAt()==null?"Pending":order.getCompletedAt().format(date))+"\nCustomer: "+order.getCustomerName()+" / "+d.getContactNumber()+"\nFulfillment: "+d.getFulfillmentMethod()+" / "+d.getDeliveryPoint().getLabel()+"\nAddress: "+d.getAddress()+"\nPayment: "+d.getPaymentMethod()+"\nNotes: "+d.getNotes()+"\nCourier confirmation: "+(d.isPickup()?"Not required for pickup":order.getCourierReference())+"\nTotal: "+String.format("₱%,.2f",order.getTotal());
        java.util.List<ReturnRequest> requests=service.ReturnsService.requests(user);
        java.util.List<RefundRecord> refunds=service.RefundService.records(user);
        facts+="\nOrder outcome: "+(order.getStatus()==OrderStatus.COMPLETED?ReturnPresentation.history(order,requests):service.RefundService.outcome(order,refunds));
        if(order.getStatus()==OrderStatus.CANCELLED)facts+="\nCancellation: "+order.getDeliveryIssueResponse();
        for(RefundRecord refund:refunds)if(refund.orderId()==order.getId()&&refund.source().startsWith("ORDER:"))facts+="\nRefund: "+refund.status()+" / "+String.format("₱%,.2f",refund.amount())+" / "+refund.reference()+" / "+refund.note();
        for(ReturnRequest request:requests)if(request.orderId()==order.getId())facts+="\nReturn #"+request.id()+": "+ReturnPresentation.status(request)+" / refund "+(request.status().equals("REFUNDED")?String.format("₱%,.2f",request.refundAmount()):"not issued")+" / "+request.refundReference();
        if(order.getCourierConfirmedAt()!=null)facts+="\nDelivery recorded: "+order.getCourierConfirmedAt().format(date);
        if(order.getDeliveredBy()!=null)facts+="\nDelivery recorded by: User #"+order.getDeliveredBy();
        if(order.getReceivedBy()!=null)facts+="\nReceipt acknowledged by: User #"+order.getReceivedBy();
        if(!order.getDeliveryIssue().isBlank())facts+="\nDelivery issue reported: "+order.getDeliveryIssue();
        content.add(summary(order,requests,refunds),BorderLayout.NORTH);
        DefaultTableModel model=new DefaultTableModel(new String[]{"Product","Size","Qty","Unit price","Subtotal","Picture"},0){public boolean isCellEditable(int r,int c){return false;}};
        for(CartItem item:order.getItems())model.addRow(new Object[]{item.getProduct().getName(),item.getProduct().getSize(),item.getQuantity(),String.format("₱%,.2f",item.getProduct().getPrice()),String.format("₱%,.2f",item.getSubtotal()),item.getProduct().getImagePath()});
        JTable table=new JTable(model);table.setRowHeight(64);table.getColumnModel().getColumn(5).setCellRenderer((t,value,selected,focus,r,c)->new JLabel(ProductImages.load(String.valueOf(value),48,48)));table.putClientProperty("report.details",facts);content.add(new ModernScrollPane(table));
        JPanel actions=new JPanel(new FlowLayout(FlowLayout.RIGHT));JButton print=new JButton("Preview / print details"),close=new JButton("Close");print.addActionListener(e->gui.admin.AdminTableReport.open(content,table,"Order "+order.getControlNumber(),user));close.addActionListener(e->dialog.dispose());actions.add(print);actions.add(close);content.add(actions,BorderLayout.SOUTH);
        dialog.setContentPane(content);AppTheme.apply(content);
        Rectangle screen=dialog.getGraphicsConfiguration().getBounds();Insets in=Toolkit.getDefaultToolkit().getScreenInsets(dialog.getGraphicsConfiguration());
        dialog.setSize(Math.min(1000,screen.width-in.left-in.right-32),Math.min(820,screen.height-in.top-in.bottom-32));
        dialog.setResizable(true);dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);dialog.setLocationRelativeTo(parent);dialog.setVisible(true);
    }
    static JPanel summary(Order order,java.util.List<ReturnRequest> requests,java.util.List<RefundRecord> refunds){
        CheckoutDetails details=order.getCheckoutDetails();
        var date=java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy • h:mm a");
        String outcome=order.getStatus()==OrderStatus.COMPLETED?ReturnPresentation.history(order,requests):service.RefundService.outcome(order,refunds);
        JPanel summary=new JPanel(new BorderLayout(0,14));summary.setOpaque(false);
        summary.add(DetailCards.heading("Order "+order.getControlNumber(),outcome),BorderLayout.NORTH);
        JPanel body=DetailCards.column();
        JPanel overview=DetailCards.card("Order overview");
        overview.add(DetailCards.grid(DetailCards.field("Order total",String.format("₱%,.2f",order.getTotal())),DetailCards.field("Placed",order.getPlacedAt().format(date)),
                DetailCards.field("Completed",order.getCompletedAt()==null?"Pending":order.getCompletedAt().format(date)),DetailCards.field("Payment status",order.getPaymentStatus().name().replace('_',' '))));
        DetailCards.add(body,overview);
        JPanel customer=DetailCards.card("Customer");JPanel customerFields=DetailCards.column();
        DetailCards.add(customerFields,DetailCards.field("Name",order.getCustomerName()));
        DetailCards.add(customerFields,DetailCards.field("Contact",details.getContactNumber()));
        DetailCards.add(customerFields,DetailCards.field("Email",details.getEmail()));customer.add(customerFields);
        JPanel delivery=DetailCards.card("Delivery & receipt");JPanel deliveryFields=DetailCards.column();
        DetailCards.add(deliveryFields,DetailCards.field("Method / delivery point",details.getFulfillmentMethod()+" / "+details.getDeliveryPoint().getLabel()));
        DetailCards.add(deliveryFields,DetailCards.field("Address",details.getAddress()));
        DetailCards.add(deliveryFields,DetailCards.field("Courier confirmation",details.isPickup()?"Not required for pickup":order.getCourierReference()));
        if(order.getCourierConfirmedAt()!=null)DetailCards.add(deliveryFields,DetailCards.field("Delivery recorded",order.getCourierConfirmedAt().format(date)));
        if(order.getDeliveredBy()!=null)DetailCards.add(deliveryFields,DetailCards.field("Recorded by","User #"+order.getDeliveredBy()));
        if(order.getReceivedBy()!=null)DetailCards.add(deliveryFields,DetailCards.field("Receipt acknowledged by","User #"+order.getReceivedBy()));
        delivery.add(deliveryFields);DetailCards.add(body,DetailCards.grid(customer,delivery));
        DetailCards.add(body,DetailCards.note("Payment method",details.getPaymentMethod()));
        DetailCards.add(body,DetailCards.note("Order notes",details.getNotes()));
        if(order.getStatus()==OrderStatus.CANCELLED)DetailCards.add(body,DetailCards.note("Cancellation",order.getDeliveryIssueResponse()));
        if(!order.getDeliveryIssue().isBlank())DetailCards.add(body,DetailCards.note("Delivery issue reported",order.getDeliveryIssue()));
        for(RefundRecord refund:refunds)if(refund.orderId()==order.getId()&&refund.source().startsWith("ORDER:")){
            JPanel card=DetailCards.card("Order refund • "+refund.status());card.add(DetailCards.grid(DetailCards.field("Amount",String.format("₱%,.2f",refund.amount())),DetailCards.field("Reference",refund.reference()),DetailCards.field("Payment note",refund.note())));DetailCards.add(body,card);
        }
        for(ReturnRequest request:requests)if(request.orderId()==order.getId()){
            JPanel card=DetailCards.card("Return request #"+request.id());card.add(DetailCards.grid(DetailCards.field("Progress",ReturnPresentation.status(request)),
                    DetailCards.field("Refund paid",request.status().equals("REFUNDED")?String.format("₱%,.2f",request.refundAmount()):"Not issued"),DetailCards.field("Reference",request.refundReference())));DetailCards.add(body,card);
        }
        summary.add(DetailCards.scroll(body,900,290));return summary;
    }
}
