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
        JPanel notice = rounded(new Color(235, 241, 229), StaffStyles.LINE, 18);
        notice.setLayout(new BorderLayout(12,0));
        notice.setBorder(BorderFactory.createEmptyBorder(12,16,12,16));
        notice.add(summary);
        notice.add(StaffStyles.label("Live queue",11,true,StaffStyles.FOREST),BorderLayout.EAST);
        notice.setAlignmentX(Component.LEFT_ALIGNMENT);
        notice.setMaximumSize(new Dimension(Integer.MAX_VALUE,44));
        heading.add(notice);
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
        board.add(lane("03", "Ready for pickup", OrderStatus.READY_FOR_PICKUP, orders, StaffStyles.FOREST));
        board.revalidate();
        board.repaint();
    }

    private JPanel lane(String step, String title, OrderStatus status, java.util.List<Order> orders, Color accent) {
        java.util.List<Order> matching = orders.stream().filter(o -> o.getStatus() == status)
                .collect(java.util.stream.Collectors.toList());
        matching.sort(java.util.Comparator.comparingInt(Order::getQueueNumber));
        JPanel lane = rounded(StaffStyles.PAPER, StaffStyles.LINE, 22);
        lane.setBorder(BorderFactory.createEmptyBorder(16,16,16,16));
        lane.setLayout(new BorderLayout(0, 18));
        JPanel heading = new JPanel(new BorderLayout(12,0));
        heading.setOpaque(false);
        JPanel titles = new JPanel(new GridLayout(2,1,0,5));titles.setOpaque(false);
        titles.add(StaffStyles.label(title,16,true,accent));
        titles.add(StaffStyles.label(status == OrderStatus.CONFIRMED ? "In arrival order" : status == OrderStatus.PREPARING ? "Being prepared" : "Awaiting collection",11,false,StaffStyles.MUTED));
        heading.add(titles);
        JPanel badge = rounded(tint(accent),tint(accent),16);badge.setLayout(new GridBagLayout());badge.setPreferredSize(new Dimension(48,48));
        badge.add(StaffStyles.label(String.valueOf(matching.size()),22,true,accent));heading.add(badge,BorderLayout.EAST);
        heading.setBorder(BorderFactory.createEmptyBorder(0,0,14,0));
        lane.add(heading, BorderLayout.NORTH);
        JPanel cards = new JPanel();
        cards.setOpaque(false);
        cards.setLayout(new BoxLayout(cards, BoxLayout.Y_AXIS));
        if (matching.isEmpty()) {
            JPanel empty = new JPanel(new GridBagLayout());empty.setOpaque(false);
            JPanel message = new JPanel();message.setOpaque(false);message.setLayout(new BoxLayout(message,BoxLayout.Y_AXIS));
            JPanel symbol = rounded(tint(accent),tint(accent),48);symbol.setLayout(new GridBagLayout());symbol.setMaximumSize(new Dimension(64,64));symbol.setPreferredSize(new Dimension(64,64));symbol.setAlignmentX(CENTER_ALIGNMENT);
            symbol.add(StaffStyles.label(status == OrderStatus.CONFIRMED ? "1" : status == OrderStatus.PREPARING ? "2" : "3",24,true,accent));message.add(symbol);message.add(Box.createVerticalStrut(18));
            JLabel titleLabel=StaffStyles.label(status == OrderStatus.CONFIRMED ? "No waiting orders" : status == OrderStatus.PREPARING ? "Nothing in preparation" : "No orders ready yet",14,true,StaffStyles.FOREST);titleLabel.setAlignmentX(CENTER_ALIGNMENT);message.add(titleLabel);message.add(Box.createVerticalStrut(8));
            JLabel hint=StaffStyles.label(status == OrderStatus.CONFIRMED ? "New orders will appear here." : status == OrderStatus.PREPARING ? "Started orders appear here." : "Ready orders appear here.",11,false,StaffStyles.MUTED);hint.setAlignmentX(CENTER_ALIGNMENT);message.add(hint);
            empty.add(message);lane.add(empty);return lane;
        }
        for (int i = 0; i < matching.size(); i++) {
            Order order = matching.get(i);
            JPanel card = rounded(Color.WHITE, new Color(221,227,216), 18);
            card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
            card.setBorder(BorderFactory.createEmptyBorder(16,16,16,16));
            card.setAlignmentX(Component.LEFT_ALIGNMENT);
            JPanel ticket = new JPanel(new BorderLayout(8,0));ticket.setOpaque(false);ticket.setAlignmentX(LEFT_ALIGNMENT);
            ticket.add(StaffStyles.label(String.format("Q-%03d",order.getQueueNumber()),22,true,accent));
            if(status==OrderStatus.CONFIRMED)ticket.add(StaffStyles.label("Waiting #"+(i+1),11,true,accent),BorderLayout.EAST);
            card.add(ticket);
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
            card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 205));
            cards.add(card);
            cards.add(Box.createVerticalStrut(12));
        }
        JScrollPane scroll = new gui.components.ModernScrollPane(cards);
        scroll.setBorder(null);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getViewport().setBackground(StaffStyles.PAPER);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        lane.add(scroll);
        return lane;
    }

    private static Color tint(Color color) {
        return new Color((color.getRed()+255*9)/10,(color.getGreen()+255*9)/10,(color.getBlue()+255*9)/10);
    }
    private static JPanel rounded(Color background, Color border, int radius) {
        JPanel panel=new JPanel(){
            @Override protected void paintComponent(Graphics graphics){
                Graphics2D g=(Graphics2D)graphics.create();g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(background);g.fillRoundRect(0,0,getWidth()-1,getHeight()-1,radius,radius);
                g.setColor(border);g.drawRoundRect(0,0,getWidth()-1,getHeight()-1,radius,radius);g.dispose();super.paintComponent(graphics);
            }
        };panel.setOpaque(false);return panel;
    }

}
