package gui.staff;

import gui.components.RoundedButton;
import java.awt.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/** Staff landing page and operational summary. */
public final class StaffOverviewPanel extends JPanel {
    private final JPanel metrics = new JPanel(new GridLayout(1, 4, 15, 0));
    private final javax.swing.table.DefaultTableModel recentModel = new javax.swing.table.DefaultTableModel(
            new String[]{"Queue no.", "Customer", "Placed", "Status"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable activity = StaffQueuePresentation.table(recentModel, "Your orders will appear here",
            "Once a customer checks out, review their order from the queue.");
    private final JLabel nextOrder = StaffStyles.label("", 17, true, Color.WHITE);
    private java.util.List<model.Order> recentOrders = new java.util.ArrayList<>();

    public StaffOverviewPanel(Runnable openQueue) {
        setOpaque(false);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        Ui.addLeft(this, Ui.label("Good day!", 10, Font.BOLD, Ui.FOREST));
        add(Box.createVerticalStrut(4));
        Ui.addLeft(this, Ui.label("Staff dashboard", 25, Font.BOLD, Ui.INK));
        add(Box.createVerticalStrut(5));
        Ui.addLeft(this, Ui.label("Keep customer orders moving in first-come, first-served order.", 11, Font.PLAIN, Ui.MUTED));
        add(Box.createVerticalStrut(18));
        JPanel hero = Ui.card(Ui.FOREST, 24, false);
        hero.setLayout(new BorderLayout(20, 0));
        hero.setBorder(new EmptyBorder(22, 25, 22, 25));
        JPanel copy = Ui.verticalBox();
        copy.add(nextOrder);
        copy.add(Box.createVerticalStrut(8));
        copy.add(Ui.label("Open the queue to review the next confirmed order.", 11, Font.PLAIN, new Color(220, 227, 216)));
        hero.add(copy);
        RoundedButton open = Ui.lightButton("Open Queue");
        open.setPreferredSize(new Dimension(130, 36));
        open.setMinimumSize(new Dimension(130, 36));
        open.setMaximumSize(new Dimension(130, 36));
        open.addActionListener(e -> openQueue.run());
        JPanel openWrap = new JPanel(new GridBagLayout());
        openWrap.setOpaque(false);
        openWrap.add(open);
        hero.add(openWrap, BorderLayout.EAST);
        hero.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));
        hero.setPreferredSize(new Dimension(1000, 110));
        add(hero);
        add(Box.createVerticalStrut(17));
        metrics.setOpaque(false);
        metrics.setAlignmentX(Component.LEFT_ALIGNMENT);
        metrics.setMaximumSize(new Dimension(Integer.MAX_VALUE, 108));
        metrics.setPreferredSize(new Dimension(1000, 108));
        add(metrics);
        add(Box.createVerticalStrut(17));
        JPanel recent = Ui.card(Ui.PAPER, 22, true);
        recent.setLayout(new BorderLayout());
        recent.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        recent.setPreferredSize(new Dimension(1000, 320));
        JPanel heading = new JPanel(new BorderLayout());
        heading.setOpaque(false);
        heading.setBorder(new EmptyBorder(16, 18, 16, 18));
        heading.add(StaffStyles.label("Recent orders", 17, true, Ui.INK));
        heading.add(StaffStyles.label("Latest 10 orders / newest first", 11, false, Ui.MUTED), BorderLayout.EAST);
        recent.add(heading, BorderLayout.NORTH);
        Ui.styleTable(activity);
        activity.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        StaffQueuePresentation.style(activity, 3, false);
        JScrollPane scroll = new gui.components.ModernScrollPane(activity);
        scroll.setColumnHeaderView(activity.getTableHeader());
        scroll.setBorder(null);
        recent.add(scroll);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 16, 10));
        actions.setOpaque(false);
        actions.add(StaffOrderActions.detailsButton(this, activity, () -> {
            int row = activity.getSelectedRow();
            return row < 0 ? null : recentOrders.get(activity.convertRowIndexToModel(row));
        }, this::refresh));
        recent.add(actions, BorderLayout.SOUTH);
        add(recent);
        refresh();
    }

    public void refresh() {
        java.util.List<model.Order> orders = service.StoreService.getInstance().getOrders();
        long waiting = orders.stream().filter(o -> o.getStatus() == model.OrderStatus.CONFIRMED).count();
        long processing = orders.stream().filter(o -> o.getStatus() == model.OrderStatus.PREPARING).count();
        long ready = orders.stream().filter(o -> o.getStatus() == model.OrderStatus.READY_FOR_PICKUP).count();
        long completed = orders.stream().filter(o -> o.getCompletedAt() != null
                && o.getCompletedAt().toLocalDate().equals(java.time.LocalDate.now())).count();
        metrics.removeAll();
        metrics.add(Ui.metricCard(String.valueOf(waiting), "Waiting"));
        metrics.add(Ui.metricCard(String.valueOf(processing), "Preparing"));
        metrics.add(Ui.metricCard(String.valueOf(ready), "Ready for pickup"));
        metrics.add(Ui.metricCard(String.valueOf(completed), "Completed today"));
        metrics.revalidate();
        metrics.repaint();
        model.Order next = orders.stream().filter(o -> o.getStatus() == model.OrderStatus.CONFIRMED)
                .min(java.util.Comparator.comparingInt(model.Order::getQueueNumber)).orElse(null);
        nextOrder.setText(next == null ? "No orders waiting to start" : String.format("Next to prepare: Q-%03d  /  Waiting #1", next.getQueueNumber()));
        int selected = activity.getSelectedRow();
        Integer selectedId = selected < 0 ? null : recentOrders.get(selected).getId();
        recentModel.setRowCount(0);
        recentOrders = orders.stream().sorted(java.util.Comparator.comparingInt(model.Order::getQueueNumber).reversed())
                .limit(10).collect(java.util.stream.Collectors.toList());
        for (model.Order order : recentOrders) {
            recentModel.addRow(new Object[]{String.format("Q-%03d", order.getQueueNumber()), order.getCustomerName(),
                    order.getPlacedAt().format(java.time.format.DateTimeFormatter.ofPattern("MMM d, h:mm a")), order.getStatus().getLabel()});
        }
        if (selectedId != null) for (int i = 0; i < recentOrders.size(); i++) {
            if (recentOrders.get(i).getId() == selectedId) { activity.setRowSelectionInterval(i, i); break; }
        }
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
            JScrollPane scroll = new gui.components.ModernScrollPane(table);

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
