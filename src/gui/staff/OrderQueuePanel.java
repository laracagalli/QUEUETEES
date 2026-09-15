package gui.staff;

import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import model.Order;
import model.OrderStatus;
import service.StoreService;

/** Staff page for processing the FCFS order queue. */
public final class OrderQueuePanel extends JPanel {
    private final StoreService store = StoreService.getInstance();
    private final DefaultTableModel model = new DefaultTableModel(new String[]{"Waiting #", "Queue no.", "Customer", "Placed", "Items", "Total", "Status"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
        @Override public Class<?> getColumnClass(int column) { return column == 0 ? Integer.class : Object.class; }
    };
    private final JTable table = StaffQueuePresentation.table(model, "No active orders yet", "Customer orders will appear here after checkout.");
    private final JLabel message = Ui.label("", 11, Font.PLAIN, Ui.MUTED);
    private List<Order> visibleOrders;
    private final gui.components.RoundedButton advance = Ui.primaryButton("Advance selected");
    private final JButton selectNext = StaffStyles.lightButton("Select next waiting");

    private final JPanel metrics = new JPanel(new GridLayout(1, 3, 14, 0));
    private final JPanel detailBody = new JPanel();
    private final JLabel featuredTicket = StaffStyles.label("", 23, true, Ui.FOREST);
    private final JLabel featuredInfo = StaffStyles.label("", 12, false, Ui.MUTED);
    private final JButton startNext = StaffStyles.button("Start preparing");
    private boolean refreshing;
    private String lastState = "";

    public OrderQueuePanel() {
        setOpaque(false);
        setLayout(new BorderLayout(0, 18));
        JPanel top = Ui.verticalBox();
        top.add(Ui.label("Order queue", 26, Font.BOLD, Ui.INK));
        top.add(Box.createVerticalStrut(5));
        top.add(Ui.label("First come, first served / Live order fulfillment", 12, Font.PLAIN, Ui.MUTED));
        top.add(Box.createVerticalStrut(18));
        metrics.setOpaque(false); metrics.setAlignmentX(Component.LEFT_ALIGNMENT);
        metrics.setPreferredSize(new Dimension(1000, 105)); metrics.setMaximumSize(new Dimension(Integer.MAX_VALUE, 105));
        top.add(metrics); add(top, BorderLayout.NORTH);
        JPanel left = createQueueCard();
        JPanel right = createDetailCard();
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right);
        split.setOpaque(false); split.setBorder(null); split.setDividerSize(14); split.setResizeWeight(0.72);
        left.setMinimumSize(new Dimension(420, 300)); right.setMinimumSize(new Dimension(265, 300));
        right.setPreferredSize(new Dimension(300, 450));
        split.setContinuousLayout(true); add(split);
        refresh();
    }

    private JPanel createQueueCard() {
        JPanel card = Ui.card(Ui.PAPER, 22, true);
        card.setLayout(new BorderLayout(0, 12));
        card.setBorder(BorderFactory.createEmptyBorder(16, 14, 12, 14));
        JPanel header = Ui.verticalBox();
        JPanel title = new JPanel(new BorderLayout()); title.setOpaque(false); title.setAlignmentX(Component.LEFT_ALIGNMENT);
        title.add(StaffStyles.label("Live order queue", 16, true, Ui.INK));
        title.add(StaffStyles.label("● Auto-refresh on", 11, false, Ui.FOREST), BorderLayout.EAST);
        header.add(title); header.add(Box.createVerticalStrut(14));
        JPanel featured = Ui.card(new Color(235, 241, 229), 16, true);
        featured.setLayout(new BorderLayout(12, 0)); featured.setBorder(BorderFactory.createEmptyBorder(13, 14, 13, 14));
        JPanel info = Ui.verticalBox();
        info.add(StaffStyles.label("NEXT TO PREPARE", 10, true, Ui.FOREST)); info.add(Box.createVerticalStrut(5));
        info.add(featuredTicket); info.add(Box.createVerticalStrut(4)); info.add(featuredInfo);
        featured.add(info); JPanel action = new JPanel(new GridBagLayout()); action.setOpaque(false); action.add(startNext);
        startNext.addActionListener(e -> {
            Order next = nextWaiting();
            if (next != null) StaffOrderActions.advance(this, next, this::refresh);
        });
        featured.add(action, BorderLayout.EAST); header.add(featured); header.add(Box.createVerticalStrut(8));
        header.add(StaffOrderActions.search(table));
        header.add(StaffStyles.label("Waiting # = preparation priority. Select a row for details.", 10, false, Ui.MUTED));
        card.add(header, BorderLayout.NORTH);
        Ui.styleTable(table); StaffQueuePresentation.style(table, 6, true);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
        int[] widths = {85, 80, 135, 120, 45, 95, 145};
        for (int i = 0; i < widths.length; i++) table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        table.removeColumn(table.getColumnModel().getColumn(5)); // Total remains in the selected-order summary.
        JScrollPane scroll = new JScrollPane(table); scroll.setColumnHeaderView(table.getTableHeader()); scroll.setBorder(null);
        scroll.getViewport().setBackground(Ui.PAPER); card.add(scroll);
        table.getSelectionModel().addListSelectionListener(e -> { if (!refreshing && !e.getValueIsAdjusting()) updateDetails(); });
        table.getRowSorter().addRowSorterListener(e -> SwingUtilities.invokeLater(() -> updateCount()));
        JPanel footer = new JPanel(new BorderLayout(8, 0)); footer.setOpaque(false);
        footer.add(message);
        selectNext.setText("Select next"); selectNext.setPreferredSize(new Dimension(125, 36));
        selectNext.addActionListener(e -> selectNextWaiting()); selectNext.setToolTipText("Clear search and select the first waiting order.");
        footer.add(selectNext, BorderLayout.EAST); card.add(footer, BorderLayout.SOUTH);
        return card;
    }

    private JPanel createDetailCard() {
        JPanel card = Ui.card(Ui.PAPER, 22, true); card.setLayout(new BorderLayout(0, 14));
        card.setBorder(BorderFactory.createEmptyBorder(18, 18, 16, 18));
        card.add(StaffStyles.label("Order details", 17, true, Ui.INK), BorderLayout.NORTH);
        detailBody.setOpaque(false); detailBody.setLayout(new BoxLayout(detailBody, BoxLayout.Y_AXIS));
        JScrollPane scroll = new JScrollPane(detailBody); scroll.setBorder(null); scroll.getViewport().setBackground(Ui.PAPER);
        scroll.getVerticalScrollBar().setUnitIncrement(20); card.add(scroll);
        JPanel actions = new JPanel(new GridLayout(2, 1, 0, 9)); actions.setOpaque(false);
        advance.addActionListener(e -> advanceSelected()); actions.add(advance);
        actions.add(StaffOrderActions.detailsButton(this, table, this::selectedOrder, this::refresh));
        card.add(actions, BorderLayout.SOUTH); return card;
    }

    private Order nextWaiting() {
        return store.getActiveOrders().stream().filter(o -> o.getStatus() == OrderStatus.CONFIRMED)
                .min(java.util.Comparator.comparingInt(Order::getQueueNumber)).orElse(null);
    }

    private void updateDetails() {
        Order order = selectedOrder(); detailBody.removeAll();
        boolean eligible = order != null && (order.getStatus() != OrderStatus.CONFIRMED
                || Integer.valueOf(1).equals(StaffQueuePresentation.waitingPositions(store.getActiveOrders()).get(order.getId())));
        advance.setEnabled(eligible);
        advance.setText(order == null ? "Select an order" : StaffOrderActions.nextAction(order));
        advance.setToolTipText(order != null && !eligible ? "Start Waiting #1 first." : "Update this order's status");
        if (order == null) {
            detailBody.add(detailText("Select an order from the queue to see its customer, items and fulfillment details.", false));
        } else {
            detailBody.add(StaffStyles.label(String.format("Q-%03d", order.getQueueNumber()), 26, true, Ui.INK));
            detailBody.add(Box.createVerticalStrut(8));
            detailBody.add(StaffStyles.label(order.getStatus().getLabel(), 12, true, StaffQueuePresentation.statusColor(order.getStatus().getLabel())));
            detailBody.add(Box.createVerticalStrut(12));
            detailBody.add(detailText(order.getCustomerName(), true));
            detailBody.add(detailText(order.getCheckoutDetails().getContactNumber(), false));
            detailBody.add(Box.createVerticalStrut(14));
            detailBody.add(StaffStyles.label("ORDER SUMMARY", 11, true, Ui.FOREST));
            detailBody.add(Box.createVerticalStrut(8));
            detailBody.add(detailText("Items: " + order.getItemCount() + "    Total: " + String.format("₱%,.2f", order.getTotal()), true));
            detailBody.add(detailText("Placed: " + order.getPlacedAt().format(DateTimeFormatter.ofPattern("MMM d, h:mm a")), false));
            detailBody.add(detailText("Fulfillment: " + order.getCheckoutDetails().getFulfillmentMethod(), false));
            detailBody.add(detailText("Payment: " + order.getCheckoutDetails().getPaymentMethod(), false));
            detailBody.add(Box.createVerticalStrut(14));
            for (model.CartItem item : order.getItems()) {
                JPanel row = new JPanel(new BorderLayout(10, 0)); row.setOpaque(false); row.setAlignmentX(Component.LEFT_ALIGNMENT);
                row.setBorder(BorderFactory.createEmptyBorder(6, 0, 8, 0));
                java.net.URL url = getClass().getResource(item.getProduct().getImagePath());
                if (url != null) row.add(new JLabel(new ImageIcon(new ImageIcon(url).getImage().getScaledInstance(42, 48, Image.SCALE_SMOOTH))), BorderLayout.WEST);
                row.add(detailText(item.getProduct().getName() + "\nQty " + item.getQuantity() + " / " + String.format("₱%,.2f", item.getSubtotal()), false));
                row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 85)); detailBody.add(row);
            }
            if (!eligible) { detailBody.add(Box.createVerticalStrut(10)); detailBody.add(detailText("Waiting #1 must begin preparation first.", true)); }
        }
        detailBody.revalidate(); detailBody.repaint();
    }

    private JTextArea detailText(String text, boolean bold) {
        JTextArea value = new JTextArea(text == null ? "—" : text); StaffStyles.readOnly(value);
        value.setOpaque(false); value.setLineWrap(true); value.setWrapStyleWord(true);
        value.setFont(new Font("Segoe UI", bold ? Font.BOLD : Font.PLAIN, 12)); value.setForeground(bold ? Ui.INK : Ui.MUTED);
        int lines = Math.max(1, (value.getText().length() + 27) / 28) + (value.getText().contains("\n") ? 1 : 0);
        value.setMinimumSize(new Dimension(0, lines * 18)); value.setPreferredSize(new Dimension(210, lines * 18));
        value.setMaximumSize(new Dimension(Integer.MAX_VALUE, lines * 18)); value.setAlignmentX(Component.LEFT_ALIGNMENT); return value;
    }

    private void updateCount() {
        message.setText(table.getRowCount() + " of " + model.getRowCount() + " active orders");
    }

    public void refresh() {
        List<Order> active = store.getActiveOrders();
        String state = store.getOrders().stream().map(o -> o.getId() + ":" + o.getStatus())
                .collect(java.util.stream.Collectors.joining("|")) + java.time.LocalDate.now();
        if (state.equals(lastState)) return;
        lastState = state;
        Order selected = selectedOrder(); refreshing = true;
        model.setRowCount(0); visibleOrders = active;
        java.util.Map<Integer, Integer> positions = StaffQueuePresentation.waitingPositions(visibleOrders);
        for (Order order : visibleOrders) {
            model.addRow(new Object[]{positions.get(order.getId()), "Q-" + String.format("%03d", order.getQueueNumber()), order.getCustomerName(),
                    order.getPlacedAt().format(DateTimeFormatter.ofPattern("MMM d, h:mm a")), order.getItemCount(),
                    String.format("₱%,.2f", order.getTotal()), order.getStatus().getLabel()});
        }
        restoreSelection(selected);
        Order next = nextWaiting();
        if (table.getSelectedRow() < 0) restoreSelection(next != null ? next : visibleOrders.isEmpty() ? null : visibleOrders.get(0));
        refreshing = false;
        selectNext.setEnabled(next != null); startNext.setEnabled(next != null);
        featuredTicket.setText(next == null ? "No orders waiting" : String.format("Q-%03d", next.getQueueNumber()));
        featuredInfo.setText(next == null ? "Continue working on active orders below." : next.getCustomerName() + " / " + next.getItemCount() + " items");
        featuredInfo.setToolTipText(featuredInfo.getText());
        metrics.removeAll();
        metrics.add(Ui.metricCard(String.valueOf(positions.size()), "Waiting to start"));
        metrics.add(Ui.metricCard(String.valueOf(active.size() - positions.size()), "Preparing / ready"));
        long completed = store.getCompletedOrders().stream().filter(o -> o.getCompletedAt() != null
                && o.getCompletedAt().toLocalDate().equals(java.time.LocalDate.now())).count();
        metrics.add(Ui.metricCard(String.valueOf(completed), "Completed today")); metrics.revalidate(); metrics.repaint();
        updateDetails(); updateCount();
    }

    private void selectNextWaiting() {
        clearSearch(this);
        refresh();
        for (int i = 0; i < visibleOrders.size(); i++) {
            if (visibleOrders.get(i).getStatus() == OrderStatus.CONFIRMED) {
                int row = table.convertRowIndexToView(i);
                if (row >= 0) {
                    table.setRowSelectionInterval(row, row);
                    table.scrollRectToVisible(table.getCellRect(row, 0, true));
                    table.requestFocusInWindow();
                }
                break;
            }
        }
    }

    private void clearSearch(Container parent) {
        for (Component child : parent.getComponents()) {
            if (child instanceof JTextField && Boolean.TRUE.equals(((JTextField) child).getClientProperty("searchField")))
                ((JTextField) child).setText("");
            else if (child instanceof Container) clearSearch((Container) child);
        }
    }

    private Order selectedOrder() {
        int row = table.getSelectedRow();
        if (row < 0 || visibleOrders == null) return null;
        int index = table.convertRowIndexToModel(row);
        return index < visibleOrders.size() ? visibleOrders.get(index) : null;
    }

    private void restoreSelection(Order selected) {
        if (selected == null) return;
        for (int i = 0; i < visibleOrders.size(); i++) {
            if (visibleOrders.get(i).getId() == selected.getId()) {
                int view = table.convertRowIndexToView(i);
                if (view >= 0) table.setRowSelectionInterval(view, view);
                return;
            }
        }
    }

    private void advanceSelected() {
        Order selected = selectedOrder();
        if (selected != null) StaffOrderActions.advance(this, selected, this::refresh);
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
