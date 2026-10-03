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
    private final Timer timer = new Timer(2000, e -> { if (isShowing()) refresh(); });
    private boolean updating;
    public ActivityLogPanel() {
        super(new BorderLayout(0, 16));
        setOpaque(false);
        JPanel header = new JPanel(new BorderLayout(0, 16)); header.setOpaque(false);
        JPanel title = new JPanel(new GridLayout(2, 1, 0, 6)); title.setOpaque(false);
        JLabel heading = new JLabel("Activity log"); heading.setFont(new Font("Segoe UI", Font.BOLD, 25));
        title.add(heading); title.add(new JLabel("Staff and administrator activity in this session. Logs reset when the application restarts."));
        header.add(title, BorderLayout.NORTH);
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
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable t,Object value,boolean selected,boolean focus,int row,int column) {
                super.getTableCellRendererComponent(t,value,selected,focus,row,column);
                setBorder(BorderFactory.createEmptyBorder(8,12,8,12));
                setToolTipText(String.valueOf(value)); return this;
            }
        };
        table.setDefaultRenderer(Object.class, renderer);
        add(new ModernScrollPane(table));
        count.setForeground(AppTheme.GREEN); add(count, BorderLayout.SOUTH);
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
        count.setText(table.getRowCount() + " matching activity entries");
    }
}
