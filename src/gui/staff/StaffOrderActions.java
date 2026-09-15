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
        JPanel content = new JPanel(new BorderLayout(12, 12));
        content.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
        CheckoutDetails details = order.getCheckoutDetails();
        JTextArea summary = new JTextArea("Customer: " + order.getCustomerName()
                + "\nEmail: " + details.getEmail() + "\nContact: " + details.getContactNumber()
                + "\nFulfillment: " + details.getFulfillmentMethod() + "\nAddress: " + details.getAddress()
                + "\nPayment method (simulated): " + details.getPaymentMethod() + "\nNotes: " + details.getNotes()
                + "\nStatus: " + order.getStatus().getLabel() + "\nPlaced: " + order.getPlacedAt()
                + (order.getCompletedAt() == null ? "" : "\nCompleted: " + order.getCompletedAt()));
        StaffStyles.readOnly(summary);
        summary.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        summary.setBackground(StaffStyles.PAPER);
        summary.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        summary.setLineWrap(true);
        summary.setWrapStyleWord(true);
        JScrollPane summaryScroll = new JScrollPane(summary);
        summaryScroll.setPreferredSize(new Dimension(600, 230));
        content.add(summaryScroll, BorderLayout.NORTH);
        DefaultTableModel items = new DefaultTableModel(new String[]{"Product", "Quantity", "Unit price", "Subtotal"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        for (CartItem item : order.getItems()) items.addRow(new Object[]{item.getProduct().getName(),
                item.getQuantity(), String.format("₱%,.2f", item.getProduct().getPrice()), String.format("₱%,.2f", item.getSubtotal())});
        JTable table = new JTable(items);
        table.setRowHeight(30);
        content.add(new JScrollPane(table), BorderLayout.CENTER);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actions.add(new JLabel(String.format("Total: ₱%,.2f", order.getTotal())));
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
        dialog.setSize(680, 570);
        dialog.setLocationRelativeTo(parent);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dialog.setVisible(true);
    }
}
