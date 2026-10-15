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
    private final JLabel warning = new JLabel();
    private final JCheckBox allDates=new JCheckBox("All dates",true);
    private final JSpinner from=new JSpinner(new SpinnerDateModel()),to=new JSpinner(new SpinnerDateModel());
    private java.util.List<ActivityLogger.Entry> lastEntries;
    private final JPanel userCounts = new JPanel();
    private final JButton print = new RoundedButton("Print activity log", new Color(28,31,27), Color.WHITE);
    private final Timer timer = new Timer(2000, e -> { if (isShowing()) refresh(); });
    private boolean updating;
    private final java.util.List<String> countIdentities = new java.util.ArrayList<>();
    public ActivityLogPanel() {
        this(false);
    }
    public ActivityLogPanel(boolean overview) {
        super(new BorderLayout(0, 16));
        setOpaque(false);
        JPanel header = new JPanel(new BorderLayout(0, 16)); header.setOpaque(false);
        JPanel title = new JPanel(new GridLayout(2, 1, 0, 6)); title.setOpaque(false);
        JLabel heading = new JLabel(overview ? "Recent activity" : "Activity log"); heading.setFont(new Font("Segoe UI", Font.BOLD, overview ? 17 : 25));
        title.add(heading); title.add(new JLabel("Review staff activity and reported usage warnings."));
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
        JPanel dates=new JPanel(new FlowLayout(FlowLayout.LEFT,8,4));dates.setOpaque(false);allDates.setOpaque(false);dates.add(allDates);dates.add(new JLabel("From"));dates.add(from);dates.add(new JLabel("Through"));dates.add(to);
        for(JSpinner spinner:new JSpinner[]{from,to}){spinner.setEditor(new JSpinner.DateEditor(spinner,"yyyy-MM-dd"));spinner.setPreferredSize(new Dimension(115,30));spinner.setEnabled(false);spinner.addChangeListener(e->filter());}
        from.getAccessibleContext().setAccessibleName("Activity date from");to.getAccessibleContext().setAccessibleName("Activity date through");
        allDates.addActionListener(e->{from.setEnabled(!allDates.isSelected());to.setEnabled(!allDates.isSelected());filter();});
        warning.setIcon(new NavigationIcon("activity"));warning.setIconTextGap(8);warning.setFont(new Font("Segoe UI",Font.BOLD,12));
        JButton review=new JButton("Review warnings");review.addActionListener(e->IncidentReview.show(this,ActivityLogger.getActor()));
        JPanel notices=new JPanel(new BorderLayout(8,0));notices.setOpaque(false);notices.add(warning);notices.add(review,BorderLayout.EAST);JPanel lower=new JPanel(new GridLayout(2,1,0,4));lower.setOpaque(false);lower.add(dates);lower.add(notices);header.add(lower,BorderLayout.SOUTH);
        table.setAutoCreateRowSorter(true); table.setRowHeight(44);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 12)); table.setBackground(AppTheme.PAPER);
        table.setSelectionBackground(new Color(222,231,217)); table.setFillsViewportHeight(true);
        table.getColumnModel().getColumn(0).setPreferredWidth(185);
        table.getColumnModel().getColumn(1).setPreferredWidth(125);
        table.getColumnModel().getColumn(2).setPreferredWidth(85);
        table.getColumnModel().getColumn(3).setPreferredWidth(225);
        table.getColumnModel().getColumn(4).setPreferredWidth(520);
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
        JLabel summaryTitle=new JLabel("Activity by account");summaryTitle.setFont(new Font("Segoe UI",Font.BOLD,13));summaryTitle.setForeground(AppTheme.GREEN);
        footer.add(summaryTitle,BorderLayout.NORTH);
        count.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        count.setForeground(AppTheme.GREEN); footer.add(count, BorderLayout.SOUTH);
        userCounts.setLayout(new BoxLayout(userCounts, BoxLayout.X_AXIS));
        userCounts.setOpaque(false);
        JScrollPane summaries = new ModernScrollPane(userCounts);
        summaries.setBorder(null); summaries.setOpaque(false); summaries.getViewport().setOpaque(false);
        summaries.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        summaries.setPreferredSize(new Dimension(0, 94));
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
        long unresolved=service.SecurityIncidents.incidents().stream().filter(i->!i.resolved()).count();
        warning.setText(unresolved==0?"No warnings awaiting review":unresolved+" usage warning"+(unresolved==1?"":"s")+" awaiting review");
        warning.setForeground(unresolved==0?new Color(46,104,65):new Color(170,45,35));
        if(entries.equals(lastEntries))return;lastEntries=entries;
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
        countIdentities.clear();
        java.util.Map<Integer,String> identities = new java.util.HashMap<>();
        DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        for (var entry : entries) {
            String identity = entry.username() + " (" + entry.role() + ")";
            countIdentities.add(entry.userId() > 0 ? identities.computeIfAbsent(entry.userId(),id -> identity) : identity);
            model.addRow(new Object[]{format.format(entry.time()), entry.username(), entry.role(), entry.action(), entry.details()});
        }
        filter();
    }
    private void filter() {
        String query = search.getText().trim().toLowerCase(java.util.Locale.ROOT);
        ((TableRowSorter<?>)table.getRowSorter()).setRowFilter(new RowFilter<TableModel, Integer>() {
            public boolean include(Entry<? extends TableModel, ? extends Integer> entry) {
                if(!allDates.isSelected()) {
                    java.time.LocalDate start=date(from),end=date(to),day=java.time.LocalDate.parse(entry.getStringValue(0).substring(0,10));
                    if(start.isAfter(end)||day.isBefore(start)||day.isAfter(end))return false;
                }
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
        java.util.Map<String, Integer> totals = new java.util.TreeMap<>();
        for (int index : TablePagination.matchingRows(table)) {
            String identity = countIdentities.get(index);
            totals.merge(identity, 1, Integer::sum);
        }
        userCounts.removeAll();
        totals.forEach((identity, total) -> {
            userCounts.add(accountCard(identity,total));userCounts.add(Box.createHorizontalStrut(12));
        });
        count.setText(TablePagination.matchingCount(table)+" matching records  ·  "+model.getRowCount()+" total logged  ·  "+totals.size()+" account"+(totals.size()==1?"":"s"));
        if (totals.isEmpty()) userCounts.add(new JLabel("No matching staff or administrator activity."));
        userCounts.revalidate(); userCounts.repaint();
    }
    private static JPanel accountCard(String identity,int total){
        int separator=identity.lastIndexOf(" (");
        String username=separator<0?identity:identity.substring(0,separator),role=separator<0?"Account":identity.substring(separator+2,identity.length()-1);
        JPanel card=new JPanel(new BorderLayout(10,0));card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(AppTheme.LINE),BorderFactory.createEmptyBorder(12,12,12,12)));
        card.setPreferredSize(new Dimension(230,76));card.setMinimumSize(new Dimension(200,76));card.setMaximumSize(new Dimension(260,76));
        card.putClientProperty("activity.identity",identity);card.putClientProperty("activity.count",total);
        card.getAccessibleContext().setAccessibleName(identity+": "+total+" activity records");card.setToolTipText(identity+": "+total+" matching activity records");
        JLabel icon=new JLabel(new NavigationIcon("profile"));icon.setForeground(AppTheme.GREEN);card.add(icon,BorderLayout.WEST);
        JPanel account=new JPanel(new GridLayout(2,1,0,3));account.setOpaque(false);
        JLabel name=DetailCards.label(username,13,true);name.setToolTipText(username);account.add(name);
        JLabel badge=DetailCards.label(role,10,false);badge.setForeground(role.equals("ADMIN")?new Color(35,105,62):new Color(36,88,149));account.add(badge);card.add(account);
        JPanel number=new JPanel(new BorderLayout());number.setOpaque(false);
        JLabel value=DetailCards.label(String.valueOf(total),24,true);value.setHorizontalAlignment(SwingConstants.RIGHT);number.add(value);
        JLabel caption=DetailCards.label(total==1?"record":"records",10,false);caption.setHorizontalAlignment(SwingConstants.RIGHT);number.add(caption,BorderLayout.SOUTH);card.add(number,BorderLayout.EAST);
        return card;
    }
    private static java.time.LocalDate date(JSpinner spinner){return ((java.util.Date)spinner.getValue()).toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();}
}
