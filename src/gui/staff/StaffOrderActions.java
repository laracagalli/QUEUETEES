package gui.staff;

import java.awt.*;
import java.util.function.Supplier;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import model.*;
import service.StoreService;

/** Shared staff search and fulfillment actions. */
final class StaffOrderActions {
    private StaffOrderActions() { }

    static JPanel search(JTable table) {
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>((DefaultTableModel) table.getModel());
        table.setRowSorter(sorter);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JTextField input = new JTextField(24);
        input.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        input.setBackground(StaffStyles.PAPER);
        input.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(StaffStyles.LINE),
                BorderFactory.createEmptyBorder(7, 10, 7, 10)));
        input.putClientProperty("searchField", true);
        input.getAccessibleContext().setAccessibleName("Search orders");
        input.getDocument().addDocumentListener(new DocumentListener() {
            private void update() {
                String query = input.getText().trim();
                ((TableRowSorter<?>)table.getRowSorter()).setRowFilter(query.isEmpty() ? null : RowFilter.regexFilter("(?i)" + java.util.regex.Pattern.quote(query)));
            }
            public void insertUpdate(DocumentEvent e) { update(); }
            public void removeUpdate(DocumentEvent e) { update(); }
            public void changedUpdate(DocumentEvent e) { update(); }
        });
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bar.setOpaque(false);
        bar.add(new JLabel("Search orders:"));
        bar.add(input);
        JButton clear = StaffStyles.lightButton("Clear search");
        clear.addActionListener(e -> { input.setText(""); input.requestFocusInWindow(); });
        bar.add(clear);
        bar.setAlignmentX(Component.LEFT_ALIGNMENT);
        bar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        return bar;
    }

    static JButton detailsButton(Component parent, JTable table, Supplier<Order> selected, Runnable refresh) {
        JButton button = StaffStyles.button("View details");
        button.setEnabled(false);
        table.getSelectionModel().addListSelectionListener(e -> button.setEnabled(table.getSelectedRow() >= 0));
        button.addActionListener(e -> {
            Order order = selected.get();
            if (order != null) showDetails(parent, order, refresh);
        });
        return button;
    }
    static JButton shortcutButton(Component parent,Supplier<Order> selected,Runnable refresh){
        JButton button=StaffStyles.lightButton("Update stage…");button.addActionListener(e->updateStage(parent,selected.get(),refresh));return button;
    }
    private static void updateStage(Component parent,Order selected,Runnable refresh){
        if(selected==null){StaffStyles.showMessage(parent,"Select an order first.","Update stage",JOptionPane.INFORMATION_MESSAGE);return;}
        try {
            StoreService store=StoreService.getInstance();
            Order order=store.getOrders().stream().filter(o->o.getId()==selected.getId()).findFirst().orElseThrow(()->new IllegalStateException("Order was not found. Refresh the queue."));
            if(order.getStatus()==OrderStatus.CONFIRMED && !order.canStartPacking()){
                int answer=JOptionPane.showOptionDialog(parent,"This order is awaiting payment verification.\nCheck the actual payment before starting preparation.",
                        "Payment verification required",JOptionPane.DEFAULT_OPTION,JOptionPane.INFORMATION_MESSAGE,null,new String[]{"Verify payment","Cancel"},"Verify payment");
                if(answer==0){
                    boolean[] verified={false};
                    gui.components.PaymentReview.show(parent,order,service.ActivityLogger.getActor(),()->{verified[0]=true;refresh.run();});
                    if(verified[0])updateStage(parent,order,refresh);
                }
                return;
            }
            if(order.getStatus()==OrderStatus.CONFIRMED && !PackingPriority.isNext(store.getOrders(),order)){
                java.util.List<Order> waiting=PackingPriority.queue(store.getOrders());
                String next=waiting.isEmpty()?"Refresh the queue.":"Start "+waiting.get(0).getControlNumber()+" first ("+PackingPriority.pieces(waiting.get(0))+" pieces).";
                StaffStyles.showMessage(parent,"This order is waiting for its packing turn.\n"+next,"Packing priority",JOptionPane.INFORMATION_MESSAGE);return;
            }
            if(order.getStatus()==OrderStatus.OUT_FOR_DELIVERY || order.getCheckoutDetails().isPickup()&&order.getStatus()==OrderStatus.READY_FOR_PICKUP){
                advance(parent,order,refresh);return;
            }
            if(order.getStatus()==OrderStatus.DELIVERED){
                StaffStyles.showMessage(parent,"Delivery is recorded. The customer must select Order Received to complete this order.","Awaiting customer receipt",JOptionPane.INFORMATION_MESSAGE);return;
            }
            OrderStatus expected=order.getStatus();
            java.util.List<OrderStatus> targets=OrderWorkflow.targets(order);
            if(targets.isEmpty()){StaffStyles.showMessage(parent,"This order has no further staff stage updates. Current status: "+order.getStatusLabel(),"Update stage",JOptionPane.INFORMATION_MESSAGE);return;}
            JComboBox<OrderStatus> choice=new gui.components.RoundedComboBox<>(targets.toArray(OrderStatus[]::new));
            choice.setRenderer((list,value,index,selectedValue,focus)->new JLabel(value==null?"":value==OrderStatus.READY_FOR_PICKUP&&!order.getCheckoutDetails().isPickup()?"Ready for dispatch":value.getLabel()));
            JPanel form=stageForm(order,choice);
            if(JOptionPane.showConfirmDialog(parent,form,"Update "+order.getControlNumber(),JOptionPane.OK_CANCEL_OPTION)!=JOptionPane.OK_OPTION)return;
            store.advanceOrderTo(order.getId(),expected,(OrderStatus)choice.getSelectedItem());refresh.run();
        } catch(RuntimeException ex){
            StaffStyles.showMessage(parent,ex.getMessage(),"Update stage",JOptionPane.WARNING_MESSAGE);refresh.run();
        }
    }

    static JPanel stageForm(Order order,JComboBox<OrderStatus> choice){
        JPanel form=new JPanel(new BorderLayout(0,12));
        JPanel selection=new JPanel(new GridLayout(0,1,0,8));
        selection.add(new JLabel("Current stage: "+order.getStatusLabel()));
        selection.add(new JLabel("Confirm the next completed stage:"));selection.add(choice);
        form.add(selection,BorderLayout.NORTH);
        choice.setPreferredSize(new Dimension(440,42));
        form.add(new JLabel("Confirm each stage in order. The next available stage is shown above."),BorderLayout.SOUTH);
        gui.components.AppTheme.apply(form);return form;
    }

    static String nextAction(Order order) {
        switch (order.getStatus()) {
            case CONFIRMED: return "Start preparing";
            case PREPARING: return "Mark ready";
            case READY_FOR_PICKUP: return order.getCheckoutDetails().isPickup() ? "Confirm pickup collected"
                    : order.getCheckoutDetails().getDeliveryPoint() == DeliveryPoint.CENTRAL ? "Dispatch local delivery" : "Transfer to delivery point";
            case TRANSFERRING: return "Confirm arrival at delivery point";
            case AT_DELIVERY_POINT: return "Dispatch local delivery";
            case OUT_FOR_DELIVERY: return "Record courier confirmation";
            case DELIVERED: return "Awaiting customer acknowledgment";
            default: return "Completed";
        }
    }

    static void advance(Component parent, Order order, Runnable refresh) {
        OrderStatus expected = order.getStatus();
        if (StaffStyles.confirm(parent, nextAction(order) + " for "
                + order.getControlNumber() + "?", "Update order",
                JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        try {
            if(expected==OrderStatus.OUT_FOR_DELIVERY) {
                String reference=JOptionPane.showInputDialog(parent,"Courier delivery confirmation / proof reference:","Courier confirmation",JOptionPane.QUESTION_MESSAGE);
                if(reference==null)return;
                StoreService.getInstance().confirmCourierDelivery(order.getId(),reference,service.ActivityLogger.getActor());
            } else StoreService.getInstance().advanceOrder(order.getId(), expected);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            StaffStyles.showMessage(parent, ex.getMessage(), "Order update", JOptionPane.WARNING_MESSAGE);
        }
        refresh.run();
    }

    static void showDetails(Component parent, Order order, Runnable refresh) {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(parent),
                "Order " + order.getControlNumber(), Dialog.ModalityType.APPLICATION_MODAL);
        JPanel content = new JPanel(new BorderLayout(0, 20));
        content.setBackground(StaffStyles.PAPER);
        content.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        content.add(orderSummary(order), BorderLayout.NORTH);
        DefaultTableModel items = new DefaultTableModel(new String[]{"Product", "Quantity", "Unit price", "Subtotal"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        for (CartItem item : order.getItems()) items.addRow(new Object[]{item.getProduct().getDisplayName(),
                item.getQuantity(), String.format("₱%,.2f", item.getProduct().getPrice()), String.format("₱%,.2f", item.getSubtotal())});
        JTable table = new JTable(items);
        table.setRowHeight(42);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.setBackground(StaffStyles.PAPER);
        table.setDefaultRenderer(Object.class, new javax.swing.table.DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object value, boolean selected, boolean focus, int row, int column) {
                super.getTableCellRendererComponent(t, value, selected, focus, row, column);
                setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));
                return this;
            }
        });
        table.getColumnModel().getColumn(0).setPreferredWidth(300);
        JScrollPane itemScroll = new gui.components.ModernScrollPane(table);
        itemScroll.getViewport().setBackground(StaffStyles.PAPER);
        content.add(itemScroll, BorderLayout.CENTER);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actions.setOpaque(false);
        if(order.getPaymentStatus()!=PaymentStatus.VERIFIED){JButton verify=StaffStyles.lightButton("Verify payment");verify.addActionListener(e->gui.components.PaymentReview.show(content,order,service.ActivityLogger.getActor(),()->{dialog.dispose();refresh.run();}));actions.add(verify);}
        actions.add(StaffStyles.label(String.format("Total: ₱%,.2f", order.getTotal()), 17, true, StaffStyles.FOREST));
        if (!order.getCheckoutDetails().isPickup() && order.getCheckoutDetails().getDeliveryPoint() == DeliveryPoint.UNASSIGNED
                && order.getStatus() != OrderStatus.COMPLETED && order.getStatus() != OrderStatus.OUT_FOR_DELIVERY) {
            JButton review = StaffStyles.lightButton("Review delivery address");
            review.addActionListener(e -> {
                gui.components.DeliveryAddressPanel form = new gui.components.DeliveryAddressPanel(new JTextArea(2, 20), () -> {});
                if (JOptionPane.showConfirmDialog(dialog, form, "Assign address / delivery point", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
                try { StoreService.getInstance().assignDeliveryAddress(order.getId(), form.getAddress()); refresh.run(); dialog.dispose(); }
                catch (IllegalArgumentException | IllegalStateException ex) { StaffStyles.showMessage(dialog, ex.getMessage(), "Address review", JOptionPane.WARNING_MESSAGE); }
            });
            actions.add(review);
        }
        if (order.getStatus() != OrderStatus.COMPLETED && order.getStatus() != OrderStatus.DELIVERED) {
            JButton advance = StaffStyles.button(nextAction(order));
            advance.addActionListener(e -> { advance(dialog, order, refresh); dialog.dispose(); });
            actions.add(advance);
        }
        JButton close = StaffStyles.lightButton("Close");
        if(order.getStatus()==OrderStatus.DELIVERED && java.util.Set.of("OPEN","REVIEWING").contains(order.getDeliveryIssueStatus())){
            JButton review=StaffStyles.button("Review non-receipt report");
            review.addActionListener(e->{
                JComboBox<String> outcome=new JComboBox<>(new String[]{"Investigating","Arrange redelivery","Respond to customer","Confirm package lost"});
                JTextArea note=new JTextArea(4,32);note.setLineWrap(true);note.setWrapStyleWord(true);
                JPanel form=new JPanel(new BorderLayout(0,10));form.add(outcome,BorderLayout.NORTH);form.add(new gui.components.ModernScrollPane(note));
                if(JOptionPane.showConfirmDialog(dialog,form,"Customer response / delivery action",JOptionPane.OK_CANCEL_OPTION)!=JOptionPane.OK_OPTION)return;
                try{String state=new String[]{"REVIEWING","REDELIVERY","RESOLVED","LOST"}[outcome.getSelectedIndex()];StoreService.getInstance().reviewDeliveryIssue(order.getId(),state,note.getText(),service.ActivityLogger.getActor());refresh.run();dialog.dispose();}
                catch(IllegalArgumentException|IllegalStateException ex){StaffStyles.showMessage(dialog,ex.getMessage(),"Delivery report",JOptionPane.WARNING_MESSAGE);}
            });actions.add(review);
        }
        close.addActionListener(e -> dialog.dispose());
        actions.add(close);
        if(order.getStatus().ordinal()<OrderStatus.OUT_FOR_DELIVERY.ordinal())actions.add(shortcutButton(dialog,()->order,()->{refresh.run();dialog.dispose();}));
        content.add(actions, BorderLayout.SOUTH);
        dialog.setContentPane(content);
        gui.components.AppTheme.apply(content);
        dialog.setSize(800, 730);
        dialog.setMinimumSize(new Dimension(720, 650));
        dialog.setLocationRelativeTo(parent);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dialog.setVisible(true);
    }
    static JPanel orderSummary(Order order) {
        JPanel summary = new JPanel(new BorderLayout(0, 18));
        summary.setOpaque(false);
        JPanel heading = new JPanel(new BorderLayout(16, 0));
        heading.setOpaque(false);
        heading.add(StaffStyles.label("Order " + order.getControlNumber() + "", 24, true, StaffStyles.FOREST));
        JLabel status = StaffStyles.label(order.getStatusLabel(), 13, true,
                StaffQueuePresentation.statusColor(order.getStatusLabel()));
        status.setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));
        heading.add(status, BorderLayout.EAST);
        summary.add(heading, BorderLayout.NORTH);
        CheckoutDetails details = order.getCheckoutDetails();
        JPanel facts = new JPanel(new GridLayout(0, 2, 24, 14));
        facts.setOpaque(false);
        facts.add(summaryFact("CUSTOMER", order.getCustomerName()));
        facts.add(summaryFact("CONTROL NUMBER",order.getControlNumber()));
        facts.add(summaryFact("CONTACT", details.getContactNumber()));
        facts.add(summaryFact("EMAIL", details.getEmail()));
        java.time.format.DateTimeFormatter format = java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy / h:mm a");
        facts.add(summaryFact("PLACED", order.getPlacedAt().format(format)));
        facts.add(summaryFact("FULFILLMENT", details.getFulfillmentMethod()));
        facts.add(summaryFact("DELIVERY POINT", details.getDeliveryPoint().getLabel()));
        if(!order.getCourierReference().isBlank())facts.add(summaryFact("COURIER CONFIRMATION",order.getCourierReference()+" / "+order.getCourierConfirmedAt()));
        facts.add(summaryFact("PAYMENT", details.getPaymentMethod()));
        facts.add(summaryFact("DELIVERY ADDRESS", details.getAddress()));
        facts.add(summaryFact("ORDER NOTES", details.getNotes()));
        if(order.getDeliveredBy()!=null)facts.add(summaryFact("DELIVERY RECORDED BY","User #"+order.getDeliveredBy()));
        if(order.getReceivedBy()!=null)facts.add(summaryFact("RECEIPT ACKNOWLEDGED BY","User #"+order.getReceivedBy()));
        if(!order.getDeliveryIssue().isBlank())facts.add(summaryFact("NON-RECEIPT REPORT",order.getDeliveryIssue()));
        if(!order.getDeliveryIssue().isBlank())facts.add(summaryFact("REPORT PROGRESS",service.DeliveryIssueReview.label(order.getDeliveryIssueStatus())+" / "+order.getDeliveryIssueResponse()));
        if (order.getCompletedAt() != null) facts.add(summaryFact("COMPLETED", order.getCompletedAt().format(format)));
        JScrollPane scroll = new gui.components.ModernScrollPane(facts);
        scroll.setPreferredSize(new Dimension(700, 260));
        scroll.getViewport().setBackground(StaffStyles.PAPER);
        facts.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 18));
        summary.add(scroll, BorderLayout.CENTER);
        return summary;
    }

    private static JPanel summaryFact(String title, String value) {
        JPanel fact = new JPanel(new BorderLayout(0, 5));
        fact.setOpaque(false);
        fact.add(StaffStyles.label(title, 10, true, StaffStyles.MUTED), BorderLayout.NORTH);
        JTextArea text = new JTextArea(value == null || value.trim().isEmpty() ? "—" : value, 2, 24);
        StaffStyles.readOnly(text);
        text.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        text.setForeground(StaffStyles.FOREST);
        text.setOpaque(false);
        text.setLineWrap(true);
        text.setWrapStyleWord(true);
        JScrollPane valueScroll = new gui.components.ModernScrollPane(text,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        valueScroll.setPreferredSize(new Dimension(260, 42));
        valueScroll.getViewport().setBackground(StaffStyles.PAPER);
        fact.add(valueScroll);
        return fact;
    }

}
