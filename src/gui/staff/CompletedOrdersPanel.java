package gui.staff;

import java.awt.*;
import java.time.format.DateTimeFormatter;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import model.Order;
import service.StoreService;

/** Staff page containing completed order records. */
public final class CompletedOrdersPanel extends JPanel {
    private final java.util.List<Order> rowOrders = new java.util.ArrayList<>();
    private final StoreService store = StoreService.getInstance();
    private final DefaultTableModel model = new DefaultTableModel(new String[]{"Queue no.", "Customer", "Completed", "Items", "Total"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable table = new JTable(model);
    private final JLabel message = Ui.label("", 11, Font.PLAIN, Ui.MUTED);
    private final model.User staff;
    private final JCheckBox allDates = new JCheckBox("All dates", true);
    private final JSpinner from = new JSpinner(new SpinnerDateModel());
    private final JSpinner to = new JSpinner(new SpinnerDateModel());
    private final JButton print = StaffStyles.button("Preview / print report");
    private final JPanel content = new JPanel();
    private JPanel preview;

    public CompletedOrdersPanel() {
        this(null);
    }

    public CompletedOrdersPanel(model.User staff) {
        this.staff = staff;
        setOpaque(false);
        setLayout(new BorderLayout());
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        Ui.addLeft(content, Ui.label("ORDERS", 10, Font.BOLD, Ui.FOREST));
        content.add(Box.createVerticalStrut(4));
        Ui.addLeft(content, Ui.label("Completed orders", 25, Font.BOLD, Ui.INK));
        content.add(Box.createVerticalStrut(5));
        Ui.addLeft(content, Ui.label("A record of orders completed by the staff team.", 11, Font.PLAIN, Ui.MUTED));
        content.add(Box.createVerticalStrut(18));
        content.add(StaffOrderActions.search(table));
        content.add(dateFilters());
        content.add(createTableCard());
        add(content, BorderLayout.CENTER);
        table.getRowSorter().addRowSorterListener(e -> updateCount());
        refresh();
    }

    private JPanel dateFilters() {
        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        filters.setOpaque(false);
        filters.setAlignmentX(Component.LEFT_ALIGNMENT);
        filters.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        allDates.setOpaque(false);
        for (JSpinner spinner : new JSpinner[]{from, to}) {
            spinner.setEditor(new JSpinner.DateEditor(spinner, "yyyy-MM-dd"));
            spinner.setPreferredSize(new Dimension(125, 30));
            spinner.setEnabled(false);
            spinner.addChangeListener(e -> refresh());
        }
        from.getAccessibleContext().setAccessibleName("Completion date from");
        to.getAccessibleContext().setAccessibleName("Completion date through");
        allDates.addActionListener(e -> {
            from.setEnabled(!allDates.isSelected());
            to.setEnabled(!allDates.isSelected());
            refresh();
        });
        filters.add(allDates);
        filters.add(new JLabel("Completed from")); filters.add(from);
        filters.add(new JLabel("through")); filters.add(to);
        return filters;
    }

    private java.time.LocalDate date(JSpinner spinner) {
        return ((java.util.Date) spinner.getValue()).toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
    }

    private void updateCount() {
        boolean valid = allDates.isSelected() || !date(from).isAfter(date(to));
        message.setText(!valid ? "Choose a start date on or before the end date."
                : table.getRowCount() + " completed order(s) shown - report includes these rows in this order");
        print.setEnabled(valid && table.getRowCount() > 0 && staff != null);
        print.setToolTipText(staff == null ? "Sign in as staff to print a report."
                : !valid ? "Correct the completion-date range first."
                : table.getRowCount() == 0 ? "Complete an order or change the filters to create a report."
                : "Open a report preview of the completed orders shown here.");
    }

    private void printReport() {
        try {
            if (!allDates.isSelected()) { from.commitEdit(); to.commitEdit(); }
            refresh();
            if (!print.isEnabled()) {
                StaffStyles.showMessage(this, print.getToolTipText(), "Report unavailable", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            java.util.List<Order> snapshot = new java.util.ArrayList<>();
            for (int row = 0; row < table.getRowCount(); row++)
                snapshot.add(rowOrders.get(table.convertRowIndexToModel(row)));
            String scope = allDates.isSelected() ? "All completion dates" : date(from) + " through " + date(to) + " (inclusive)";
            JTextField search = findSearch(this);
            if (search != null && !search.getText().trim().isEmpty()) scope += "; search: " + search.getText().trim();
            JPanel nextPreview = ReportPreview.create(this, new CompletedOrdersReport(snapshot, staff, scope), this::closePreview);
            preview = nextPreview;
            remove(content);
            add(preview, BorderLayout.CENTER);
            revalidate();
            repaint();
        } catch (java.text.ParseException ex) {
            StaffStyles.showMessage(this, "Enter valid dates in yyyy-MM-dd format.", "Report dates", JOptionPane.WARNING_MESSAGE);
        } catch (RuntimeException ex) {
            ex.printStackTrace();
            StaffStyles.showMessage(this, "Could not open the report preview: " + ex.getMessage(), "Print report", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void closePreview() {
        if (preview != null) remove(preview);
        preview = null;
        add(content, BorderLayout.CENTER);
        refresh();
        revalidate();
        repaint();
    }

    private JTextField findSearch(Container parent) {
        for (Component child : parent.getComponents()) {
            if (child instanceof JTextField && Boolean.TRUE.equals(((JTextField) child).getClientProperty("searchField"))) return (JTextField) child;
            if (child instanceof Container) {
                JTextField found = findSearch((Container) child);
                if (found != null) return found;
            }
        }
        return null;
    }

    private JPanel createTableCard() {
        JPanel card = Ui.card(Ui.PAPER, 22, true);
        card.setLayout(new BorderLayout());
        Ui.styleTable(table);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setColumnHeaderView(table.getTableHeader());
        scroll.setBorder(null);
        card.add(scroll);
        message.setHorizontalAlignment(SwingConstants.CENTER);
        message.setBorder(new javax.swing.border.EmptyBorder(12, 8, 12, 8));
        card.add(message, BorderLayout.NORTH);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actions.setOpaque(false);
        actions.add(StaffOrderActions.detailsButton(this, table, () -> {
            int row = table.getSelectedRow();
            return row < 0 ? null : rowOrders.get(table.convertRowIndexToModel(row));
        }, this::refresh));
        print.addActionListener(e -> printReport());
        actions.add(print);
        card.add(actions, BorderLayout.SOUTH);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        card.setPreferredSize(new Dimension(1000, 520));
        return card;
    }

    public void refresh() {
        // A report is a stable snapshot; leave it visible during the dashboard's timer ticks.
        if (preview != null) return;
        int selectedRow = table.getSelectedRow();
        Order selected = selectedRow < 0 ? null : rowOrders.get(table.convertRowIndexToModel(selectedRow));
        model.setRowCount(0);
        rowOrders.clear();
        for (Order order : store.getCompletedOrders()) {
            java.time.LocalDate completed = order.getCompletedAt().toLocalDate();
            if (!allDates.isSelected() && (completed.isBefore(date(from)) || completed.isAfter(date(to)))) continue;
            rowOrders.add(order);
            model.addRow(new Object[]{"Q-" + String.format("%03d", order.getQueueNumber()), order.getCustomerName(),
                    order.getCompletedAt().format(DateTimeFormatter.ofPattern("MMM d, h:mm a")), order.getItemCount(),
                    String.format("₱%,.2f", order.getTotal())});
        }
        if (selected != null) {
            for (int i = 0; i < rowOrders.size(); i++) {
                if (rowOrders.get(i).getId() == selected.getId()) {
                    int view = table.convertRowIndexToView(i);
                    if (view >= 0) table.setRowSelectionInterval(view, view);
                    break;
                }
            }
        }
        updateCount();
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
            if (action != null) { gui.RoundedButton button = primaryButton(action);
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
                gui.RoundedButton button = primaryButton(action);
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
        static gui.RoundedButton primaryButton(String title) {
            return StaffStyles.button(title);
        }
        static gui.RoundedButton lightButton(String title) {
            return StaffStyles.lightButton(title);
        }
        static NavButton navButton(String title) { return new NavButton(title);
        }
        static void confirmLogout(Component parent, service.AuthService authService) {
            int choice = StaffStyles.confirm(parent, "Log out of QueueTees?", "Confirm Log Out", JOptionPane.YES_NO_OPTION);
            if (choice == JOptionPane.YES_OPTION) {
                Window window = SwingUtilities.getWindowAncestor(parent);
                if (window != null) window.dispose();
                new gui.LoginFrame(authService).setVisible(true);
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
