package gui.staff;

import java.awt.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import model.CartItem;
import model.Order;
import model.OrderStatus;
import service.StoreService;

/** Staff page for inspecting and updating a selected order. */
public final class OrderDetailsPanel extends JPanel {
    private final java.util.List<Order> rowOrders = new java.util.ArrayList<>();
    private final StoreService store = StoreService.getInstance();
    private final DefaultTableModel model = new DefaultTableModel(
            new String[]{"Waiting #", "Queue no.", "Customer", "Product", "Qty", "Payment", "Status"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
        @Override public Class<?> getColumnClass(int column) { return column == 0 ? Integer.class : Object.class; }
    };
    private final JTable table = StaffQueuePresentation.table(model, "No active orders yet", "Customer orders will appear here after checkout.");
    private final JLabel message = Ui.label("", 11, Font.PLAIN, Ui.MUTED);

    private final JPanel detailBody = new DetailContent();
    private static final class DetailContent extends JPanel implements Scrollable {
        public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        public int getScrollableUnitIncrement(Rectangle r, int orientation, int direction) { return 22; }
        public int getScrollableBlockIncrement(Rectangle r, int orientation, int direction) { return Math.max(22, r.height - 22); }
        public boolean getScrollableTracksViewportWidth() { return true; }
        public boolean getScrollableTracksViewportHeight() { return false; }
    }
    private final JButton advance = StaffStyles.button("Select an order");
    private boolean refreshing;
    private String lastState = "";

    public OrderDetailsPanel() {
        setOpaque(false);
        setLayout(new BorderLayout(0, 16));
        JPanel header = Ui.verticalBox();
        Ui.addLeft(header, Ui.label("ORDERS", 10, Font.BOLD, Ui.FOREST));
        header.add(Box.createVerticalStrut(4));
        Ui.addLeft(header, Ui.label("Order details", 25, Font.BOLD, Ui.INK));
        header.add(Box.createVerticalStrut(5));
        Ui.addLeft(header, Ui.label("Review queued orders and prepare items in first-come, first-served order.", 11, Font.PLAIN, Ui.MUTED));
        header.add(Box.createVerticalStrut(16)); header.add(StaffOrderActions.search(table));
        add(header, BorderLayout.NORTH);
        JPanel list = createTableCard(); JPanel detail = createDetailsCard();
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, list, detail);
        split.setOpaque(false); split.setBorder(null); split.setDividerSize(14); split.setResizeWeight(0.56);
        list.setMinimumSize(new Dimension(350, 300)); detail.setMinimumSize(new Dimension(340, 300));
        list.setPreferredSize(new Dimension(580, 500)); detail.setPreferredSize(new Dimension(430, 500));
        split.setContinuousLayout(true); add(split);
        refresh();
    }

    private JPanel createTableCard() {
        JPanel card = Ui.card(Ui.PAPER, 22, true); card.setLayout(new BorderLayout(0, 12));
        card.setBorder(BorderFactory.createEmptyBorder(16, 12, 12, 12));
        Ui.styleTable(table); StaffQueuePresentation.style(table, 6, true);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
        int[] widths = {55, 65, 100, 135, 35, 125, 145};
        table.getColumnModel().getColumn(0).setMinWidth(55);
        table.getColumnModel().getColumn(0).setMaxWidth(70);
        for (int i = 0; i < widths.length; i++) table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        table.removeColumn(table.getColumnModel().getColumn(5)); // Payment is shown in full in the details pane.
        JScrollPane scroll = new JScrollPane(table); scroll.setColumnHeaderView(table.getTableHeader()); scroll.setBorder(null);
        scroll.getViewport().setBackground(Ui.PAPER); card.add(scroll);
        JPanel heading = Ui.verticalBox();
        Ui.addLeft(heading, Ui.label("Orders in queue", 16, Font.BOLD, Ui.INK));
        heading.add(Box.createVerticalStrut(8)); Ui.addLeft(heading, message);
        card.add(heading, BorderLayout.NORTH);
        JLabel hint = Ui.label("Select an item row to review the entire order.", 11, Font.PLAIN, Ui.MUTED);
        card.add(hint, BorderLayout.SOUTH);
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!refreshing && !e.getValueIsAdjusting()) showSelectedDetails();
        });
        return card;
    }

    private JPanel createDetailsCard() {
        JPanel card = Ui.card(Ui.PAPER, 22, true); card.setLayout(new BorderLayout(0, 14));
        card.setBorder(BorderFactory.createEmptyBorder(18, 18, 16, 18));
        detailBody.setOpaque(false); detailBody.setLayout(new BoxLayout(detailBody, BoxLayout.Y_AXIS));
        JScrollPane scroll = new JScrollPane(detailBody); scroll.setBorder(null); scroll.getViewport().setBackground(Ui.PAPER);
        scroll.getVerticalScrollBar().setUnitIncrement(22); card.add(scroll);
        JPanel actions = new JPanel(new GridLayout(1, 2, 10, 0)); actions.setOpaque(false);
        advance.addActionListener(e -> { Order order = selectedOrder(); if (order != null) StaffOrderActions.advance(this, order, this::refresh); });
        actions.add(advance);
        actions.add(StaffOrderActions.detailsButton(this, table, this::selectedOrder, this::refresh));
        card.add(actions, BorderLayout.SOUTH); return card;
    }

    private Order selectedOrder() {
        int row = table.getSelectedRow();
        return row < 0 ? null : rowOrders.get(table.convertRowIndexToModel(row));
    }

    private void showSelectedDetails() {
        Order order = selectedOrder(); detailBody.removeAll();
        Integer position = order == null ? null : StaffQueuePresentation.waitingPositions(store.getActiveOrders()).get(order.getId());
        boolean eligible = order != null && (order.getStatus() != OrderStatus.CONFIRMED || Integer.valueOf(1).equals(position));
        advance.setEnabled(eligible); advance.setText(order == null ? "Select an order" : StaffOrderActions.nextAction(order));
        advance.setToolTipText(eligible ? "Update the selected order" : "Select the first waiting order to start preparation.");
        if (order == null) {
            detailBody.add(Ui.label("Order details", 19, Font.BOLD, Ui.INK)); detailBody.add(Box.createVerticalStrut(16));
            detailBody.add(text("Select an order on the left to review its items and preparation details.", false));
        } else {
            JPanel title = new JPanel(new BorderLayout(10, 0)); title.setOpaque(false); title.setAlignmentX(Component.LEFT_ALIGNMENT);
            title.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
            title.add(Ui.label(String.format("Q-%03d", order.getQueueNumber()), 26, Font.BOLD, Ui.INK));
            title.add(Ui.label(position == null ? order.getStatus().getLabel() : "Waiting #" + position, 12, Font.BOLD,
                    StaffQueuePresentation.statusColor(order.getStatus().getLabel())), BorderLayout.EAST);
            detailBody.add(title); separator();
            JPanel facts = new JPanel(new GridLayout(1, 2, 16, 0)); facts.setOpaque(false); facts.setAlignmentX(Component.LEFT_ALIGNMENT);
            JPanel customer = Ui.verticalBox(), payment = Ui.verticalBox();
            fact(customer, "Customer name", order.getCustomerName());
            fact(customer, "Contact number", order.getCheckoutDetails().getContactNumber());
            fact(customer, "Order time", order.getPlacedAt().format(java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy / h:mm a")));
            fact(payment, "Payment method", order.getCheckoutDetails().getPaymentMethod());
            fact(payment, "Queue position", position == null ? "Already in progress" : "Waiting #" + position);
            fact(payment, "Fulfillment", order.getCheckoutDetails().getFulfillmentMethod());
            facts.add(customer); facts.add(payment); facts.setMaximumSize(new Dimension(Integer.MAX_VALUE, 175)); detailBody.add(facts);
            separator();
            detailBody.add(Ui.label("Order items / " + order.getItemCount() + " items", 15, Font.BOLD, Ui.INK));
            detailBody.add(Box.createVerticalStrut(10));
            for (CartItem item : order.getItems()) {
                JPanel row = new JPanel(new BorderLayout(12, 0)); row.setOpaque(false); row.setAlignmentX(Component.LEFT_ALIGNMENT);
                row.setBorder(BorderFactory.createEmptyBorder(6, 0, 8, 0));
                java.net.URL url = getClass().getResource(item.getProduct().getImagePath());
                if (url != null) row.add(new JLabel(new ImageIcon(new ImageIcon(url).getImage().getScaledInstance(48, 55, Image.SCALE_SMOOTH))), BorderLayout.WEST);
                row.add(text(item.getProduct().getName() + "\nQty " + item.getQuantity() + " / Unit " + String.format("₱%,.2f", item.getProduct().getPrice()), true));
                row.add(Ui.label(String.format("₱%,.2f", item.getSubtotal()), 12, Font.BOLD, Ui.INK), BorderLayout.EAST);
                row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 85)); detailBody.add(row);
            }
            detailBody.add(Box.createVerticalStrut(10)); detailBody.add(Ui.label(String.format("Total: ₱%,.2f", order.getTotal()), 16, Font.BOLD, Ui.FOREST));
            separator();
            fact(detailBody, "Preparation notes", order.getCheckoutDetails().getNotes() == null || order.getCheckoutDetails().getNotes().isBlank() ? "No notes provided." : order.getCheckoutDetails().getNotes());
            detailBody.add(Ui.label("Order status", 12, Font.BOLD, Ui.INK)); detailBody.add(Box.createVerticalStrut(10));
            JPanel stages = new JPanel(new GridLayout(1, 4, 6, 0)); stages.setOpaque(false); stages.setAlignmentX(Component.LEFT_ALIGNMENT);
            for (OrderStatus status : OrderStatus.values()) {
                JPanel stage = Ui.verticalBox(); boolean reached = status.ordinal() <= order.getStatus().ordinal();
                stage.add(Ui.label(reached ? "●" : "○", 22, Font.BOLD, reached ? Ui.FOREST : Ui.MUTED));
                stage.add(text(status.getLabel(), status == order.getStatus())); stages.add(stage);
            }
            stages.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90)); detailBody.add(stages);
        }
        detailBody.revalidate(); detailBody.repaint();
    }

    private void separator() {
        detailBody.add(Box.createVerticalStrut(14));
        JSeparator line = new JSeparator(); line.setAlignmentX(Component.LEFT_ALIGNMENT); line.setForeground(Ui.LINE); line.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        detailBody.add(line); detailBody.add(Box.createVerticalStrut(14));
    }
    private void fact(JPanel panel, String caption, String value) {
        panel.add(Ui.label(caption, 10, Font.PLAIN, Ui.MUTED)); panel.add(Box.createVerticalStrut(4));
        panel.add(text(value, true)); panel.add(Box.createVerticalStrut(12));
    }
    private JTextArea text(String value, boolean bold) {
        JTextArea area = new JTextArea(value == null ? "—" : value); StaffStyles.readOnly(area);
        area.setOpaque(false); area.setLineWrap(true); area.setWrapStyleWord(true); area.setFont(Ui.font(12, bold ? Font.BOLD : Font.PLAIN));
        area.setForeground(bold ? Ui.INK : Ui.MUTED); area.setAlignmentX(Component.LEFT_ALIGNMENT);
        int height = Math.max(2, (area.getText().length() + 23) / 24) * 17;
        area.setMinimumSize(new Dimension(0, height)); area.setPreferredSize(new Dimension(150, height));
        area.setMaximumSize(new Dimension(Integer.MAX_VALUE, height)); return area;
    }

    public void refresh() {
        String state = store.getActiveOrders().stream().map(o -> o.getId() + ":" + o.getStatus()).collect(java.util.stream.Collectors.joining("|"));
        if (state.equals(lastState) && !lastState.isEmpty()) return;
        lastState = state;
        refreshing = true;
        int selectedRow = table.getSelectedRow();
        Order selected = selectedRow < 0 ? null : rowOrders.get(table.convertRowIndexToModel(selectedRow));
        model.setRowCount(0);
        rowOrders.clear();
        java.util.List<Order> active = store.getActiveOrders();
        java.util.Map<Integer, Integer> positions = StaffQueuePresentation.waitingPositions(active);
        for (Order order : active) {
            for (CartItem item : order.getItems()) {
                rowOrders.add(order);
                model.addRow(new Object[]{positions.get(order.getId()), "Q-" + String.format("%03d", order.getQueueNumber()), order.getCustomerName(),
                        item.getProduct().getName(), item.getQuantity(), order.getCheckoutDetails().getPaymentMethod(),
                        order.getStatus().getLabel()});
            }
        }
        if (selected != null) {
            for (int i = 0; i < rowOrders.size(); i++) {
                if (rowOrders.get(i).getId() == selected.getId()) {
                    int view = table.convertRowIndexToView(i);
                    if (view >= 0) {
                        table.setRowSelectionInterval(view, view);
                        break;
                    }
                }
            }
        }
        if (table.getSelectedRow() < 0 && table.getRowCount() > 0) table.setRowSelectionInterval(0, 0);
        refreshing = false;
        message.setText(active.size() + " orders / " + StaffQueuePresentation.nextMessage(active));
        showSelectedDetails();
    }

    /** Styling owned by this panel so the screen can be configured independently. */
    private static final class Ui {
        static final Color INK = new Color(28, 31, 27);
        static final Color MUTED = new Color(99, 106, 96);
        static final Color FOREST = new Color(55, 70, 56);
        static final Color SAGE = new Color(145, 155, 145);
        static final Color CREAM = new Color(241, 241, 232);
        static final Color PAPER = new Color(252, 252, 247);
        static final Color LINE = new Color(218, 220, 209);

        static Font font(int size, int style) { return new Font("Segoe UI", style, size);
        }
        static JLabel label(String value, int size, int style, Color color) {
            JLabel label = new JLabel(value);
            label.setFont(font(size, style));
            label.setForeground(color);
            return label;
        }
        static void addLeft(JPanel parent, JComponent child) { child.setAlignmentX(Component.LEFT_ALIGNMENT);
            parent.add(child);
        }
        static JPanel verticalBox() {
            JPanel panel = new JPanel();
            panel.setOpaque(false);
            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

            return panel;
        }
        static JPanel card(Color color, int radius, boolean outlined) {
            JPanel panel = new JPanel() {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(color);
                    g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);
                    if (outlined) { g2.setColor(LINE);
                        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);
                    }
                    g2.dispose();
                    super.paintComponent(g);
                }
            };

            panel.setOpaque(false);
            panel.setAlignmentX(Component.LEFT_ALIGNMENT);
            return panel;
        }
        static JPanel metricCard(String value, String caption) {
            JPanel panel = card(PAPER, 20, true);
            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
            panel.setBorder(new javax.swing.border.EmptyBorder(17, 19, 15, 19));
            panel.add(label(value, 24, Font.BOLD, FOREST));
            panel.add(Box.createVerticalStrut(6));
            panel.add(label(caption, 11, Font.PLAIN, MUTED));

            return panel;
        }
        static JPanel emptyState(String title, String message) {
            JPanel panel = card(PAPER, 22, true);
            panel.setLayout(new GridBagLayout());
            JPanel center = verticalBox();
            JLabel icon = label("○", 29, Font.PLAIN, SAGE);
            icon.setAlignmentX(Component.CENTER_ALIGNMENT);
            center.add(icon);
            center.add(Box.createVerticalStrut(6));
            JLabel heading = label(title, 15, Font.BOLD, INK);
            heading.setAlignmentX(Component.CENTER_ALIGNMENT);
            center.add(heading);
            center.add(Box.createVerticalStrut(6));

            JLabel detail = label(message, 11, Font.PLAIN, MUTED);
            detail.setAlignmentX(Component.CENTER_ALIGNMENT);
            center.add(detail);
            panel.add(center);
            panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
            panel.setPreferredSize(new Dimension(1000, 390));
            return panel;
        }
        static JPanel toolbar(String placeholder, String action) {
            JPanel toolbar = new JPanel(new BorderLayout(12, 0));
            toolbar.setOpaque(false);
            toolbar.setAlignmentX(Component.LEFT_ALIGNMENT);
            toolbar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
            JTextField search = new JTextField(placeholder);
            search.setFont(font(11, Font.PLAIN));
            search.setForeground(MUTED);
            search.setBackground(PAPER);
            search.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(LINE), new javax.swing.border.EmptyBorder(0, 13, 0, 13)));
            toolbar.add(search);
            if (action != null) { gui.components.RoundedButton button = primaryButton(action);
                button.setPreferredSize(new Dimension(140, 40));
                toolbar.add(button, BorderLayout.EAST);
                }
            return toolbar;
        }
        static JPanel tableCard(String[] columns, String emptyMessage, String action) {
            JPanel panel = card(PAPER, 22, true);
            panel.setLayout(new BorderLayout());
            panel.setBorder(new javax.swing.border.EmptyBorder(0, 0, action == null ? 0 : 12, 0));
            javax.swing.table.DefaultTableModel model = new javax.swing.table.DefaultTableModel(columns, 0) {
                @Override public boolean isCellEditable(int row, int column) { return false;
                }
            };
            JTable table = new JTable(model);
            styleTable(table);
            JScrollPane scroll = new JScrollPane(table);
        scroll.setColumnHeaderView(table.getTableHeader());

            scroll.setBorder(null);
            scroll.getViewport().setBackground(PAPER);
            panel.add(scroll);
            JLabel empty = label(emptyMessage, 11, Font.PLAIN, MUTED);
            empty.setHorizontalAlignment(SwingConstants.CENTER);
            empty.setBorder(new javax.swing.border.EmptyBorder(12, 8, 12, 8));
            panel.add(empty, BorderLayout.NORTH);

            if (action != null) {
                gui.components.RoundedButton button = primaryButton(action);
                button.setEnabled(false);
                JPanel wrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
                wrap.setOpaque(false);
                wrap.add(button);
                panel.add(wrap, BorderLayout.SOUTH);
            }
            panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
            panel.setPreferredSize(new Dimension(1000, 520));
            return panel;
        }
        static JPanel productCard(String imagePath) {
            JPanel panel = card(PAPER, 22, true);
            panel.setLayout(new BorderLayout());
            JLabel image = new JLabel(imagePath == null ? "Catalog data will appear here" : "", SwingConstants.CENTER);
            image.setOpaque(true);
            image.setBackground(new Color(229, 230, 220));
            image.setForeground(MUTED);
            image.setFont(font(10, Font.PLAIN));
            if (imagePath != null) {
                java.net.URL url = Ui.class.getResource(imagePath);
                if (url != null) image.setIcon(new ImageIcon(new ImageIcon(url).getImage().getScaledInstance(260, 245, Image.SCALE_SMOOTH)));
            }
            panel.add(image);
            JPanel caption = verticalBox();
            caption.setBorder(new javax.swing.border.EmptyBorder(12, 15, 13, 15));
            caption.add(label("Catalog item", 13, Font.BOLD, INK));
            caption.add(Box.createVerticalStrut(4));
            caption.add(label("Details load from the product catalog", 9, Font.PLAIN, MUTED));
            panel.add(caption, BorderLayout.SOUTH);
            return panel;
        }
        static gui.components.RoundedButton primaryButton(String title) {
            return StaffStyles.button(title);
        }
        static gui.components.RoundedButton lightButton(String title) {
            return StaffStyles.lightButton(title);
        }
        static NavButton navButton(String title) { return new NavButton(title);
        }
        static void confirmLogout(Component parent, service.AuthService authService) {
            int choice = StaffStyles.confirm(parent, "Log out of QueueTees?", "Confirm Log Out", JOptionPane.YES_NO_OPTION);
            if (choice == JOptionPane.YES_OPTION) {
                Window window = SwingUtilities.getWindowAncestor(parent);
                if (window != null) window.dispose();
                new gui.auth.LoginFrame(authService).setVisible(true);
            }
        }
        static void styleTable(JTable table) {
            table.setFont(font(11, Font.PLAIN));
            table.setForeground(INK);
            table.setBackground(PAPER);
            table.setSelectionBackground(new Color(222, 229, 217));
            table.setRowHeight(38);
            table.setShowGrid(true);
            table.setGridColor(LINE);
            table.setIntercellSpacing(new Dimension(1, 1));
            table.setFillsViewportHeight(true);
            javax.swing.table.JTableHeader header = table.getTableHeader();
            header.setFont(font(10, Font.BOLD));
            header.setForeground(MUTED);
            header.setBackground(new Color(238, 239, 230));
            header.setPreferredSize(new Dimension(0, 38));
            header.setReorderingAllowed(false);
            javax.swing.table.DefaultTableCellRenderer renderer = new javax.swing.table.DefaultTableCellRenderer();
            renderer.setBorder(new javax.swing.border.EmptyBorder(0, 12, 0, 12));
            table.setDefaultRenderer(Object.class, renderer);
        }
        static final class NavButton extends JButton {
            private boolean selected, hovered;
            NavButton(String title) {
                super(title);
                setFont(font(12, Font.PLAIN));
                setForeground(new Color(224, 230, 219));
                setHorizontalAlignment(LEFT);
                setBorder(new javax.swing.border.EmptyBorder(0, 13, 0, 13));
                setOpaque(false);
                setContentAreaFilled(false);
                setBorderPainted(false);
                setFocusPainted(false);
                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                addMouseListener(new java.awt.event.MouseAdapter() {
                    @Override public void mouseEntered(java.awt.event.MouseEvent e) { hovered = true;
                        repaint();
                    }
                    @Override public void mouseExited(java.awt.event.MouseEvent e) { hovered = false;
                        repaint();
                    }
                });
            }
            void setSelectedState(boolean value) {
                selected = value;
                setFont(font(12, value ? Font.BOLD : Font.PLAIN));
                setForeground(value ? Color.WHITE : new Color(224, 230, 219));
                repaint();
            }
            @Override protected void paintComponent(Graphics g) {
                if (selected || hovered) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setColor(new Color(255, 255, 255, selected ? 34 : 20));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                    g2.dispose();
                }
                super.paintComponent(g);
            }
        }
    }

}
