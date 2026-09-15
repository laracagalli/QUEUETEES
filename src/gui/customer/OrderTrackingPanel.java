package gui.customer;

import gui.components.RoundedButton;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import model.*;
import service.StoreService;

/** Customer queue dashboard built from the signed-in customer's real order records. */
public final class OrderTrackingPanel extends JPanel {
    private static final Color GREEN = new Color(55, 70, 56), INK = new Color(28, 31, 27),
            MUTED = new Color(99, 106, 96), PAPER = new Color(252, 252, 247), LINE = new Color(218, 220, 209);
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("MMM d, yyyy / h:mm a");
    private final StoreService store = StoreService.getInstance();
    private final User user;
    private final JComboBox<Order> selector = new JComboBox<>();
    private final JPanel body = new JPanel();
    private final javax.swing.Timer timer = new javax.swing.Timer(2000, e -> { if (isShowing()) refresh(); });
    private Integer selectedId;
    private boolean refreshing;
    private String lastState = "";

    public OrderTrackingPanel(User user) {
        this.user = user;
        setOpaque(false);
        setLayout(new BorderLayout(0, 16));
        JPanel header = new JPanel(new BorderLayout(15, 0)); header.setOpaque(false);
        JPanel title = vertical();
        title.add(label("QUEUE TRACKING", 10, true, GREEN)); title.add(Box.createVerticalStrut(5));
        title.add(label("My queue", 26, true, INK)); title.add(Box.createVerticalStrut(5));
        title.add(label("Track your order and see its latest progress.", 12, false, MUTED));
        header.add(title);
        JPanel choice = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 12)); choice.setOpaque(false);
        choice.add(label("Your order", 12, true, GREEN));
        selector.setPreferredSize(new Dimension(225, 34));
        selector.setBackground(PAPER);
        selector.getAccessibleContext().setAccessibleName("Choose an order to track");
        selector.setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean selected, boolean focus) {
                super.getListCellRendererComponent(list, value, index, selected, focus);
                if (value instanceof Order) { Order o = (Order) value; setText(ticket(o) + " / " + o.getStatus().getLabel()); }
                else setText("No orders yet");
                return this;
            }
        });
        selector.addActionListener(e -> { if (!refreshing) {
            Order selected = (Order) selector.getSelectedItem(); selectedId = selected == null ? null : selected.getId();
            lastState = ""; refresh();
        }});
        choice.add(selector); header.add(choice, BorderLayout.EAST); add(header, BorderLayout.NORTH);
        body.setOpaque(false); body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        JScrollPane scroll = new JScrollPane(body); scroll.setBorder(null); scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false); scroll.getVerticalScrollBar().setUnitIncrement(24);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        add(scroll); refresh();
    }
    @Override public void addNotify() { super.addNotify(); timer.start(); }
    @Override public void removeNotify() { timer.stop(); super.removeNotify(); }

    public void refresh() {
        List<Order> orders = store.getOrdersForCustomer(user.getId());
        Order selected = orders.stream().filter(o -> selectedId != null && o.getId() == selectedId).findFirst().orElse(null);
        if (selected == null) selected = orders.stream().filter(o -> o.getStatus() != OrderStatus.COMPLETED).findFirst()
                .orElse(orders.isEmpty() ? null : orders.get(orders.size() - 1));
        selectedId = selected == null ? null : selected.getId();
        long ahead = selected == null ? 0 : ordersAhead(selected);
        String state = selectedId + ":" + ahead + ":" + orders.stream().map(o -> o.getId() + ":" + o.getStatus())
                .collect(java.util.stream.Collectors.joining("|"));
        if (state.equals(lastState)) return;
        lastState = state;
        refreshing = true;
        selector.removeAllItems(); for (Order o : orders) selector.addItem(o);
        selector.setSelectedItem(selected); selector.setEnabled(!orders.isEmpty()); refreshing = false;
        body.removeAll();
        if (selected == null) {
            JPanel empty = card(); empty.setLayout(new GridBagLayout());
            JPanel text = vertical(); text.add(label("Your queue ticket will appear here", 20, true, GREEN));
            text.add(Box.createVerticalStrut(12)); text.add(label("Complete checkout to follow your order from confirmation to completion.", 13, false, MUTED));
            empty.add(text); empty.setPreferredSize(new Dimension(900, 340)); body.add(empty);
        } else {
            body.add(summary(selected, ahead)); body.add(Box.createVerticalStrut(16));
            JPanel columns = new JPanel(new BorderLayout(16, 0)); columns.setOpaque(false);
            JPanel left = vertical(); left.add(progress(selected)); left.add(Box.createVerticalStrut(16));
            JPanel lower = new JPanel(new GridLayout(1, 2, 16, 0)); lower.setOpaque(false); lower.setAlignmentX(Component.LEFT_ALIGNMENT);
            lower.add(items(selected)); lower.add(updates(selected)); left.add(lower);
            columns.add(left); JPanel receipt = receipt(selected); receipt.setPreferredSize(new Dimension(320, 450));
            columns.add(receipt, BorderLayout.EAST); columns.setAlignmentX(Component.LEFT_ALIGNMENT);
            body.add(columns);
        }
        body.revalidate(); body.repaint();
    }
    long ordersAhead(Order order) {
        if (order.getStatus() != OrderStatus.CONFIRMED) return 0;
        return store.getActiveOrders().stream().filter(o -> o.getStatus() == OrderStatus.CONFIRMED
                && o.getQueueNumber() < order.getQueueNumber()).count();
    }
    private JPanel summary(Order order, long ahead) {
        JPanel card = card(); card.setLayout(new GridLayout(1, 4, 16, 0));
        JPanel ticket = vertical(); ticket.add(label("YOUR QUEUE NUMBER", 11, true, GREEN)); ticket.add(Box.createVerticalStrut(9));
        ticket.add(label(ticket(order), 36, true, GREEN)); ticket.add(Box.createVerticalStrut(8));
        ticket.add(label("Placed " + order.getPlacedAt().format(DateTimeFormatter.ofPattern("MMM d / h:mm a")), 11, false, MUTED));
        card.add(ticket);
        card.add(metric("CURRENT STAGE", order.getStatus().getLabel(), statusDescription(order)));
        card.add(metric("WAITING ORDERS AHEAD", order.getStatus() == OrderStatus.CONFIRMED ? String.valueOf(ahead) : "—",
                order.getStatus() == OrderStatus.CONFIRMED ? (ahead == 0 ? "You are next to start preparation." : "Before your order starts preparation.") : "Your order is no longer waiting to start."));
        card.add(metric("FULFILLMENT", order.getCheckoutDetails().getFulfillmentMethod(), "This page updates automatically."));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 170)); card.setPreferredSize(new Dimension(1000, 160));
        return card;
    }
    private JPanel metric(String caption, String value, String hint) {
        JPanel panel = vertical(); panel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, LINE), new EmptyBorder(0, 16, 0, 0)));
        panel.add(label(caption, 10, true, GREEN)); panel.add(Box.createVerticalStrut(12));
        panel.add(label(value, 20, true, INK)); panel.add(Box.createVerticalStrut(9)); panel.add(copy(hint)); return panel;
    }
    private JPanel progress(Order order) {
        JPanel panel = card(); panel.setLayout(new BorderLayout(0, 14)); panel.add(label("ORDER PROGRESS", 12, true, GREEN), BorderLayout.NORTH);
        JPanel steps = new JPanel(new GridLayout(1, 4, 10, 0)); steps.setOpaque(false);
        OrderStatus[] statuses = OrderStatus.values();
        for (int i = 0; i < statuses.length; i++) {
            boolean reached = i <= order.getStatus().ordinal();
            JPanel step = vertical(); JLabel number = label(String.valueOf(i + 1), 19, true, reached ? Color.WHITE : MUTED);
            number.setOpaque(true); number.setBackground(reached ? GREEN : new Color(230, 234, 225));
            number.setHorizontalAlignment(SwingConstants.CENTER); number.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
            step.add(number); step.add(Box.createVerticalStrut(8)); step.add(label(statuses[i].getLabel(), 11, true, reached ? GREEN : MUTED));
            step.add(Box.createVerticalStrut(4)); step.add(label(i == order.getStatus().ordinal() ? "Current stage" : reached ? "Done" : "Upcoming", 10, false, MUTED)); steps.add(step);
        }
        panel.add(steps); panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 160)); panel.setPreferredSize(new Dimension(650, 150)); return panel;
    }
    private JPanel items(Order order) {
        JPanel card = card(); card.setLayout(new BorderLayout(0, 12)); card.add(label("ORDER SUMMARY", 12, true, GREEN), BorderLayout.NORTH);
        JPanel rows = vertical();
        for (CartItem item : order.getItems()) {
            JPanel row = new JPanel(new BorderLayout(10, 0)); row.setOpaque(false); row.setAlignmentX(Component.LEFT_ALIGNMENT); row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90)); row.setBorder(new EmptyBorder(8, 0, 10, 0));
            java.net.URL url = getClass().getResource(item.getProduct().getImagePath());
            if (url != null) { JLabel photo = new JLabel(new ImageIcon(new ImageIcon(url).getImage().getScaledInstance(48, 54, Image.SCALE_SMOOTH))); row.add(photo, BorderLayout.WEST); }
            JPanel info = vertical(); info.add(copy(item.getProduct().getName())); info.add(label("Qty " + item.getQuantity() + " / " + money(item.getSubtotal()), 11, false, MUTED)); row.add(info);
            rows.add(row);
        }
        JScrollPane scroll = new JScrollPane(rows); scroll.setBorder(null); scroll.getViewport().setBackground(PAPER);
        scroll.setPreferredSize(new Dimension(220, 200)); scroll.getVerticalScrollBar().setUnitIncrement(18); card.add(scroll);
        card.add(label("Total  " + money(order.getTotal()), 18, true, GREEN), BorderLayout.SOUTH); return card;
    }
    private JPanel updates(Order order) {
        JPanel card = card(); card.setLayout(new BorderLayout(0, 14)); card.add(label("ORDER UPDATES", 12, true, GREEN), BorderLayout.NORTH);
        JPanel text = vertical(); text.add(label(order.getStatus().getLabel(), 16, true, GREEN)); text.add(Box.createVerticalStrut(8));
        text.add(copy(statusDescription(order))); text.add(Box.createVerticalStrut(20));
        if (order.getCompletedAt() != null) { text.add(label("Completed", 12, true, INK)); text.add(copy(order.getCompletedAt().format(DATE))); text.add(Box.createVerticalStrut(16)); }
        text.add(label("Order received", 12, true, INK)); text.add(copy(order.getPlacedAt().format(DATE))); text.add(Box.createVerticalStrut(18));
        text.add(copy("Keep this page open for status changes from staff.")); card.add(text); return card;
    }
    private JPanel receipt(Order order) {
        CheckoutDetails d = order.getCheckoutDetails(); JPanel card = card(); card.setLayout(new BorderLayout(0, 14));
        card.add(label("RECEIPT / INFORMATION", 12, true, GREEN), BorderLayout.NORTH);
        JPanel rows = vertical();
        detail(rows, "Order number", ticket(order)); detail(rows, "Order date", order.getPlacedAt().format(DATE));
        detail(rows, "Payment method", d.getPaymentMethod()); detail(rows, "Customer", order.getCustomerName());
        detail(rows, "Contact", d.getContactNumber()); detail(rows, "Fulfillment", d.getFulfillmentMethod());
        if ("Delivery".equals(d.getFulfillmentMethod())) detail(rows, "Delivery address", d.getAddress());
        if (d.getNotes() != null && !d.getNotes().isBlank()) detail(rows, "Order notes", d.getNotes());
        rows.add(Box.createVerticalStrut(8)); rows.add(copy("Keep your queue number handy when contacting the store."));
        card.add(rows);
        RoundedButton copy = new RoundedButton("Copy queue number", INK, Color.WHITE); copy.setFont(new Font("Segoe UI", Font.BOLD, 12));
        copy.setHoverColor(new Color(65, 65, 65)); copy.setPreferredSize(new Dimension(200, 38));
        copy.addActionListener(e -> { try { Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new java.awt.datatransfer.StringSelection(ticket(order)), null); copy.setText("Queue number copied"); }
            catch (RuntimeException ex) { JOptionPane.showMessageDialog(this, "Your queue number is " + ticket(order), "Queue number", JOptionPane.INFORMATION_MESSAGE); }});
        card.add(copy, BorderLayout.SOUTH); return card;
    }
    private void detail(JPanel panel, String heading, String value) {
        panel.add(label(heading, 10, true, MUTED)); panel.add(Box.createVerticalStrut(3)); panel.add(copy(value)); panel.add(Box.createVerticalStrut(10));
    }
    private static String statusDescription(Order o) {
        switch (o.getStatus()) {
            case CONFIRMED: return "Your order is confirmed and waiting for preparation.";
            case PREPARING: return "Staff are preparing your order.";
            case READY_FOR_PICKUP: return "Delivery".equals(o.getCheckoutDetails().getFulfillmentMethod()) ? "Your order is ready for delivery fulfillment." : "Your order is ready for pickup.";
            default: return "Your order has been completed. Thank you!";
        }
    }
    private static String ticket(Order o) { return String.format("Q-%03d", o.getQueueNumber()); }
    private static String money(double amount) { return String.format("₱%,.2f", amount); }
    private static JLabel label(String text, int size, boolean bold, Color color) {
        JLabel label = new JLabel(text); label.putClientProperty("html.disable", true);
        label.setFont(new Font("Segoe UI", bold ? Font.BOLD : Font.PLAIN, size)); label.setForeground(color); return label;
    }
    private static JTextArea copy(String text) {
        JTextArea area = new JTextArea(text == null ? "—" : text); area.setEditable(false); area.setFocusable(false);
        area.setCursor(Cursor.getDefaultCursor()); area.setOpaque(false); area.setLineWrap(true); area.setWrapStyleWord(true);
        area.setFont(new Font("Segoe UI", Font.PLAIN, 12)); area.setForeground(MUTED);
        int lines = Math.max(1, (area.getText().length() + 31) / 32);
        area.setRows(lines); area.setAlignmentX(Component.LEFT_ALIGNMENT);
        area.setMinimumSize(new Dimension(0, lines * 18));
        area.setPreferredSize(new Dimension(180, lines * 18));
        area.setMaximumSize(new Dimension(Integer.MAX_VALUE, lines * 18));
        return area;
    }
    private static JPanel vertical() { JPanel p = new JPanel(); p.setOpaque(false); p.setAlignmentX(Component.LEFT_ALIGNMENT); p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS)); return p; }
    private static JPanel card() {
        JPanel p = new JPanel() {
            @Override protected void paintComponent(Graphics g) { Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); g2.setColor(PAPER);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 22, 22); g2.setColor(LINE);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 22, 22); g2.dispose(); super.paintComponent(g); }
        }; p.setOpaque(false); p.setBorder(new EmptyBorder(20, 20, 20, 20)); p.setAlignmentX(Component.LEFT_ALIGNMENT); return p;
    }
}
