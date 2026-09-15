package gui.staff;

import java.awt.*;
import javax.swing.*;
import model.Order;
import model.OrderStatus;
import service.StoreService;

/** Staff queue board with one column for each active fulfillment stage. */
public final class QueueStatusPanel extends JPanel {
    private final StoreService store = StoreService.getInstance();
    private final JPanel board = new JPanel(new GridLayout(1, 3, 16, 0));
    private final JLabel summary = StaffStyles.label("", 12, false, StaffStyles.MUTED);
    private String lastState;

    public QueueStatusPanel() {
        setOpaque(false);
        setLayout(new BorderLayout(0, 20));
        JPanel heading = new JPanel();
        heading.setOpaque(false);
        heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));
        heading.add(StaffStyles.label("LIVE OPERATIONS", 11, true, StaffStyles.FOREST));
        heading.add(Box.createVerticalStrut(5));
        heading.add(StaffStyles.label("Queue status", 27, true, StaffStyles.FOREST));
        heading.add(Box.createVerticalStrut(8));
        heading.add(summary);
        add(heading, BorderLayout.NORTH);
        board.setOpaque(false);
        add(board);
        refresh();
    }

    public void refresh() {
        java.util.List<Order> orders = store.getActiveOrders();
        String state = orders.stream().map(o -> o.getId() + ":" + o.getStatus())
                .collect(java.util.stream.Collectors.joining("|"));
        if (state.equals(lastState)) return;
        lastState = state;
        summary.setText(orders.isEmpty() ? "All caught up. New confirmed orders will appear here."
                : orders.size() + " active orders  |  Preparation starts in arrival order  |  Updates automatically");
        board.removeAll();
        board.add(lane("01", "Waiting", OrderStatus.CONFIRMED, orders, new Color(150, 112, 43)));
        board.add(lane("02", "Preparing", OrderStatus.PREPARING, orders, new Color(67, 104, 132)));
        board.add(lane("03", "Ready", OrderStatus.READY_FOR_PICKUP, orders, StaffStyles.FOREST));
        board.revalidate();
        board.repaint();
    }

    private JPanel lane(String step, String title, OrderStatus status, java.util.List<Order> orders, Color accent) {
        java.util.List<Order> matching = orders.stream().filter(o -> o.getStatus() == status)
                .collect(java.util.stream.Collectors.toList());
        JPanel lane = StaffStyles.card();
        lane.setLayout(new BorderLayout(0, 18));
        JPanel heading = new JPanel(new BorderLayout());
        heading.setOpaque(false);
        heading.add(StaffStyles.label(step + "  " + title, 14, true, accent));
        heading.add(StaffStyles.label(String.valueOf(matching.size()), 23, true, accent), BorderLayout.EAST);
        lane.add(heading, BorderLayout.NORTH);
        JPanel cards = new JPanel();
        cards.setOpaque(false);
        cards.setLayout(new BoxLayout(cards, BoxLayout.Y_AXIS));
        if (matching.isEmpty()) {
            cards.add(StaffStyles.label("No orders here", 14, true, StaffStyles.MUTED));
            cards.add(Box.createVerticalStrut(6));
            cards.add(StaffStyles.label(status == OrderStatus.CONFIRMED ? "New orders join this lane." : "Orders appear as staff update them.",
                    11, false, StaffStyles.MUTED));
        }
        for (int i = 0; i < matching.size(); i++) {
            Order order = matching.get(i);
            JPanel card = new JPanel();
            card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
            card.setBackground(new Color(242, 244, 237));
            card.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 3, 0, 0, accent),
                    BorderFactory.createEmptyBorder(12, 12, 12, 12)));
            card.setAlignmentX(Component.LEFT_ALIGNMENT);
            card.add(StaffStyles.label(String.format("Q-%03d", order.getQueueNumber())
                    + (status == OrderStatus.CONFIRMED && i == 0 ? "  /  NEXT" : ""), 20, true, accent));
            card.add(Box.createVerticalStrut(5));
            JLabel customer = StaffStyles.label(order.getCustomerName(), 13, true, StaffStyles.FOREST);
            customer.setToolTipText(order.getCustomerName());
            card.add(customer);
            card.add(Box.createVerticalStrut(4));
            card.add(StaffStyles.label(order.getItemCount() + " items  /  " + order.getCheckoutDetails().getFulfillmentMethod(), 12, false, StaffStyles.MUTED));
            card.add(StaffStyles.label("Placed " + order.getPlacedAt().format(java.time.format.DateTimeFormatter.ofPattern("MMM d, h:mm a")), 11, false, StaffStyles.MUTED));
            card.add(Box.createVerticalStrut(10));
            JButton details = StaffStyles.button("View order");
            details.addActionListener(e -> StaffOrderActions.showDetails(this, order, this::refresh));
            card.add(details);
            card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 175));
            cards.add(card);
            cards.add(Box.createVerticalStrut(12));
        }
        JScrollPane scroll = new JScrollPane(cards);
        scroll.setBorder(null);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getViewport().setBackground(StaffStyles.PAPER);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        lane.add(scroll);
        return lane;
    }

}
