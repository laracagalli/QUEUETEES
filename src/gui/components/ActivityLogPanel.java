package gui.components;

import java.awt.*;
import java.time.format.DateTimeFormatter;
import javax.swing.*;
import javax.swing.table.*;
import service.ActivityLogger;

/** Shared searchable session activity page for administrator and staff dashboards. */
public final class ActivityLogPanel extends JPanel {
    private final DefaultTableModel model = new DefaultTableModel(new String[]{"Time", "User", "Role", "Action", "Details"}, 0) {
        public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable table = new JTable(model);
    private final JTextField search = new JTextField();
    private final JComboBox<String> role = new RoundedComboBox<>(new String[]{"All roles", "ADMIN", "STAFF"});
    private final JComboBox<String> action = new RoundedComboBox<>(new String[]{"All actions"});
    private final JLabel count = new JLabel();
    private final JPanel userCounts = new JPanel();
    private final JButton print = new RoundedButton("Print activity log", new Color(28,31,27), Color.WHITE);
    private final Timer timer = new Timer(2000, e -> { if (isShowing()) refresh(); });
    private boolean updating;
    public ActivityLogPanel() {
        this(false);
    }
    public ActivityLogPanel(boolean overview) {
        super(new BorderLayout(0, 16));
        setOpaque(false);
        JPanel header = new JPanel(new BorderLayout(0, 16)); header.setOpaque(false);
        JPanel title = new JPanel(new GridLayout(2, 1, 0, 6)); title.setOpaque(false);
        JLabel heading = new JLabel(overview ? "Recent activity" : "Activity log"); heading.setFont(new Font("Segoe UI", Font.BOLD, overview ? 17 : 25));
        title.add(heading); title.add(new JLabel("Staff and administrator activity in this session. Logs reset when the application restarts."));
        JPanel headingRow = new JPanel(new BorderLayout(12, 0)); headingRow.setOpaque(false);
        headingRow.add(title);
        if (!overview) {
            print.setFont(new Font("Segoe UI", Font.BOLD, 12));
            print.setPreferredSize(new Dimension(154, 38));
            print.addActionListener(e -> {
                if (table.getRowCount() == 0) return;
                model.User user = ActivityLogger.getActor();
                for (Component parent = this; parent != null; parent = parent.getParent()) {
                    if (parent instanceof JComponent component && component.getClientProperty("activity.actor") instanceof model.User actor) {
                        user = actor; break;
                    }
                }
                gui.admin.AdminTableReport.open(this, table, "Activity log", user);
            });
            JPanel printSlot = new JPanel(new GridBagLayout()); printSlot.setOpaque(false);
            printSlot.add(print); headingRow.add(printSlot, BorderLayout.EAST);
        }
        header.add(headingRow, BorderLayout.NORTH);
        JPanel filters = new JPanel(new BorderLayout(12, 0)); filters.setOpaque(false);
        search.getAccessibleContext().setAccessibleName("Search activity log");
        search.putClientProperty("searchField", true);
        JPanel input = new JPanel(new BorderLayout(10, 0)); input.setOpaque(false);
        input.add(new JLabel("Search activity"), BorderLayout.WEST); input.add(search);
        filters.add(input);
        JPanel dropdowns = new JPanel(new GridLayout(1, 2, 10, 0)); dropdowns.setOpaque(false);
        role.getAccessibleContext().setAccessibleName("Filter by role");
        action.getAccessibleContext().setAccessibleName("Filter by action");
        dropdowns.add(role); dropdowns.add(action); filters.add(dropdowns, BorderLayout.EAST);
        header.add(filters); add(header, BorderLayout.NORTH);
        table.setAutoCreateRowSorter(true); table.setRowHeight(44);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 12)); table.setBackground(AppTheme.PAPER);
        table.setSelectionBackground(new Color(222,231,217)); table.setFillsViewportHeight(true);
        table.getColumnModel().getColumn(0).setPreferredWidth(160);
        table.getColumnModel().getColumn(4).setPreferredWidth(420);
        if (overview) {
            table.getColumnModel().getColumn(0).setHeaderValue("Date");
            table.getColumnModel().getColumn(1).setHeaderValue("Username");
            table.moveColumn(0, 2);
        }
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable t,Object value,boolean selected,boolean focus,int row,int column) {
                super.getTableCellRendererComponent(t,value,selected,focus,row,column);
                setBorder(BorderFactory.createEmptyBorder(8,12,8,12));
                setToolTipText(String.valueOf(value)); return this;
            }
        };
        table.setDefaultRenderer(Object.class, renderer);
        JPanel tableCard = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics graphics) {
                Graphics2D g = (Graphics2D) graphics.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(AppTheme.PAPER); g.fillRoundRect(0, 0, getWidth(), getHeight(), 22, 22);
                g.dispose();
            }
        };
        tableCard.setOpaque(false);
        tableCard.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        JScrollPane tableScroll = new ModernScrollPane(table);
        tableScroll.setBorder(null); tableScroll.setBackground(AppTheme.PAPER);
        tableScroll.getViewport().setBackground(AppTheme.PAPER);
        tableCard.add(tableScroll); add(tableCard);
        JPanel footer = new JPanel(new BorderLayout(0, 6)); footer.setOpaque(false);
        count.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        count.setForeground(AppTheme.GREEN); footer.add(count, BorderLayout.SOUTH);
        userCounts.setLayout(new BoxLayout(userCounts, BoxLayout.X_AXIS));
        userCounts.setOpaque(false);
        JScrollPane summaries = new ModernScrollPane(userCounts);
        summaries.setBorder(null); summaries.setOpaque(false); summaries.getViewport().setOpaque(false);
        summaries.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        summaries.setPreferredSize(new Dimension(0, 44));
        footer.add(summaries); add(footer, BorderLayout.SOUTH);
        search.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { filter(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { filter(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { filter(); }
        });
        role.addActionListener(e -> filter()); action.addActionListener(e -> { if (!updating) filter(); });
        refresh();
    }
    public void addNotify() { super.addNotify(); timer.start(); }
    public void removeNotify() { timer.stop(); super.removeNotify(); }
    public void refresh() {
        var entries = ActivityLogger.getEntries();
        var options = new java.util.ArrayList<String>(); options.add("All actions");
        entries.stream().map(ActivityLogger.Entry::action).distinct().sorted().forEach(options::add);
        boolean same = action.getItemCount() == options.size();
        if (same) for (int i = 0; i < options.size(); i++) same &= options.get(i).equals(action.getItemAt(i));
        if (!same) {
            Object selected = action.getSelectedItem(); updating = true;
            action.putClientProperty("activity.suppress", true);
            action.setModel(new DefaultComboBoxModel<>(options.toArray(String[]::new)));
            if (options.contains(selected)) action.setSelectedItem(selected);
            updating = false;
            action.putClientProperty("activity.suppress", false);
        }
        model.setRowCount(0);
        DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        for (var entry : entries) model.addRow(new Object[]{format.format(entry.time()), entry.username(), entry.role(), entry.action(), entry.details()});
        filter();
    }
    private void filter() {
        String query = search.getText().trim().toLowerCase(java.util.Locale.ROOT);
        ((TableRowSorter<?>)table.getRowSorter()).setRowFilter(new RowFilter<TableModel, Integer>() {
            public boolean include(Entry<? extends TableModel, ? extends Integer> entry) {
                if (role.getSelectedIndex() > 0 && !entry.getStringValue(2).equals(role.getSelectedItem())) return false;
                if (action.getSelectedIndex() > 0 && !entry.getStringValue(3).equals(action.getSelectedItem())) return false;
                for (int i = 0; i < entry.getValueCount(); i++)
                    if (entry.getStringValue(i).toLowerCase(java.util.Locale.ROOT).contains(query)) return true;
                return false;
            }
        });
        updateCounts();
    }
    private void updateCounts() {
        print.setEnabled(table.getRowCount() > 0);
        print.setToolTipText(table.getRowCount() == 0 ? "No matching activity to print." : "Preview and print the current filtered activity log.");
        count.setText(table.getRowCount() + " matching activity entries / " + model.getRowCount() + " logged in this session");
        java.util.Map<String, Integer> totals = new java.util.TreeMap<>();
        for (int row = 0; row < table.getRowCount(); row++) {
            int index = table.convertRowIndexToModel(row);
            String identity = model.getValueAt(index, 1) + " (" + model.getValueAt(index, 2) + ")";
            totals.merge(identity, 1, Integer::sum);
        }
        userCounts.removeAll();
        totals.forEach((identity, total) -> {
            JLabel label = new JLabel();
            // Treat usernames as plain text even when they begin with an HTML tag.
            label.putClientProperty("html.disable", true);
            label.setText(identity + ": " + total + " activity record(s)");
            label.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            label.setForeground(AppTheme.GREEN);
            label.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
            label.setOpaque(false);
            userCounts.add(label); userCounts.add(Box.createHorizontalStrut(8));
        });
        if (totals.isEmpty()) userCounts.add(new JLabel("No matching staff or administrator activity."));
        userCounts.revalidate(); userCounts.repaint();
    }
}
