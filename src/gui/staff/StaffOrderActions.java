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
                sorter.setRowFilter(query.isEmpty() ? null : RowFilter.regexFilter("(?i)" + java.util.regex.Pattern.quote(query)));
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

    static String nextAction(Order order) {
        switch (order.getStatus()) {
            case CONFIRMED: return "Start preparing";
            case PREPARING: return "Mark ready";
            case READY_FOR_PICKUP: return "Complete order";
            default: return "Completed";
        }
    }

    static void advance(Component parent, Order order, Runnable refresh) {
        OrderStatus expected = order.getStatus();
        if (StaffStyles.confirm(parent, nextAction(order) + " for Q-"
                + String.format("%03d", order.getQueueNumber()) + "?", "Update order",
                JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        try {
            StoreService.getInstance().advanceOrder(order.getId(), expected);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            StaffStyles.showMessage(parent, ex.getMessage(), "Order update", JOptionPane.WARNING_MESSAGE);
        }
        refresh.run();
    }

    static void showDetails(Component parent, Order order, Runnable refresh) {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(parent),
                "Order Q-" + String.format("%03d", order.getQueueNumber()), Dialog.ModalityType.APPLICATION_MODAL);
        JPanel content = new JPanel(new BorderLayout(0, 20));
        content.setBackground(StaffStyles.PAPER);
        content.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        content.add(orderSummary(order), BorderLayout.NORTH);
        DefaultTableModel items = new DefaultTableModel(new String[]{"Product", "Quantity", "Unit price", "Subtotal"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        for (CartItem item : order.getItems()) items.addRow(new Object[]{item.getProduct().getName(),
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
        actions.add(StaffStyles.label(String.format("Total: ₱%,.2f", order.getTotal()), 17, true, StaffStyles.FOREST));
        if (order.getStatus() != OrderStatus.COMPLETED) {
            JButton advance = StaffStyles.button(nextAction(order));
            advance.addActionListener(e -> { advance(dialog, order, refresh); dialog.dispose(); });
            actions.add(advance);
        }
        JButton close = StaffStyles.lightButton("Close");
        close.addActionListener(e -> dialog.dispose());
        actions.add(close);
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
        heading.add(StaffStyles.label(String.format("Order Q-%03d", order.getQueueNumber()), 24, true, StaffStyles.FOREST));
        JLabel status = StaffStyles.label(order.getStatus().getLabel(), 13, true,
                StaffQueuePresentation.statusColor(order.getStatus().getLabel()));
        status.setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));
        heading.add(status, BorderLayout.EAST);
        summary.add(heading, BorderLayout.NORTH);
        CheckoutDetails details = order.getCheckoutDetails();
        JPanel facts = new JPanel(new GridLayout(0, 2, 24, 14));
        facts.setOpaque(false);
        facts.add(summaryFact("CUSTOMER", order.getCustomerName()));
        facts.add(summaryFact("CONTACT", details.getContactNumber()));
        facts.add(summaryFact("EMAIL", details.getEmail()));
        java.time.format.DateTimeFormatter format = java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy / h:mm a");
        facts.add(summaryFact("PLACED", order.getPlacedAt().format(format)));
        facts.add(summaryFact("FULFILLMENT", details.getFulfillmentMethod()));
        facts.add(summaryFact("PAYMENT", details.getPaymentMethod()));
        facts.add(summaryFact("DELIVERY ADDRESS", details.getAddress()));
        facts.add(summaryFact("ORDER NOTES", details.getNotes()));
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
