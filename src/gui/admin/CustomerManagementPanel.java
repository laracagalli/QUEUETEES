package gui.admin;

import java.awt.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

/** Administrator page for registered customer accounts. */
public final class CustomerManagementPanel extends JPanel {

    private final service.AuthService authService;
    private final model.User adminUser;
    private java.util.List<model.User> customers = new java.util.ArrayList<>();

    private final DefaultTableModel tableModel = new DefaultTableModel(
            new String[]{"Customer", "Email", "Contact", "Joined", "Status"}, 0) {
        public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable table = new JTable(tableModel);
    private final JLabel footer = AdminUi.label("", 11, false);
    private final JButton suspendBtn = new gui.components.RoundedButton("Suspend", new Color(180, 45, 35), Color.WHITE) {{
        setFont(new Font("Fira Code", Font.BOLD, 11));
        setHoverColor(new Color(155, 35, 25));
        setPreferredSize(new java.awt.Dimension(100, 40));
    }};
    private final JButton unsuspendBtn = new gui.components.RoundedButton("Unsuspend", new Color(29, 113, 75), Color.WHITE) {{
        setFont(new Font("Fira Code", Font.BOLD, 11));
        setHoverColor(new Color(22, 90, 58));
        setPreferredSize(new java.awt.Dimension(115, 40));
    }};
    private final JPanel metrics = new JPanel(new BorderLayout());

    public CustomerManagementPanel(service.AuthService authService, model.User adminUser) {
        this.authService = authService;
        this.adminUser = adminUser;
        setOpaque(false);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        Ui.addLeft(this, Ui.label("CUSTOMERS", 10, Font.BOLD, Ui.FOREST));
        add(Box.createVerticalStrut(4));
        Ui.addLeft(this, Ui.label("Customer accounts", 25, Font.BOLD, Ui.INK));
        add(Box.createVerticalStrut(5));
        Ui.addLeft(this, Ui.label("View registered customers and manage their access.", 11, Font.PLAIN, Ui.MUTED));
        add(Box.createVerticalStrut(18));

        metrics.setOpaque(false);
        metrics.setAlignmentX(0);
        metrics.setMaximumSize(new Dimension(Integer.MAX_VALUE, 104));
        add(metrics);
        add(Box.createVerticalStrut(16));

        AdminUi.style(table);
        add(AdminUi.filters(table, "All statuses", "ACTIVE", "UNVERIFIED", "SUSPENDED"));
        add(Box.createVerticalStrut(16));

        JPanel card = AdminUi.tableCard(table, "Account records");

        suspendBtn.setEnabled(false);
        unsuspendBtn.setEnabled(false);
        suspendBtn.addActionListener(e -> toggleSuspend(true));
        unsuspendBtn.addActionListener(e -> toggleSuspend(false));

        JButton refresh = AdminUi.button("Refresh");
        refresh.addActionListener(e -> refresh());

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setOpaque(false);
        actions.add(unsuspendBtn);
        actions.add(suspendBtn);
        actions.add(refresh);

        JPanel bottom = new JPanel(new BorderLayout(10, 0));
        bottom.setOpaque(false);
        bottom.add(footer, BorderLayout.WEST);
        bottom.add(actions, BorderLayout.EAST);
        card.add(bottom, BorderLayout.SOUTH);
        add(card);

        table.getSelectionModel().addListSelectionListener(e -> updateButtons());
        refresh();
    }

    private model.User selected() {
        int row = table.getSelectedRow();
        if (row < 0) return null;
        int modelRow = table.convertRowIndexToModel(row);
        return modelRow < customers.size() ? customers.get(modelRow) : null;
    }

    private void updateButtons() {
        model.User u = selected();
        suspendBtn.setEnabled(u != null && u.getStatus() == model.AccountStatus.ACTIVE);
        unsuspendBtn.setEnabled(u != null && u.getStatus() == model.AccountStatus.SUSPENDED);
    }

    private void toggleSuspend(boolean suspend) {
        model.User u = selected();
        if (u == null) return;
        String action = suspend ? "Suspend" : "Unsuspend";
        int confirm = JOptionPane.showConfirmDialog(this,
                action + " account for " + u.getUsername() + "?",
                action + " account", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;
        try {
            authService.suspendUser(adminUser, u.getId(), suspend);
            refresh();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.WARNING_MESSAGE);
        }
    }

    public void refresh() {
        table.clearSelection();
        tableModel.setRowCount(0);
        try {
            customers = authService.getCustomers(adminUser);
        } catch (Exception e) {
            customers = new java.util.ArrayList<>();
        }

        long activeCount = customers.stream()
                .filter(u -> u.getStatus() == model.AccountStatus.ACTIVE && u.isEmailVerified()).count();
        long suspendedCount = customers.stream()
                .filter(u -> u.getStatus() == model.AccountStatus.SUSPENDED).count();

        metrics.removeAll();
        metrics.add(AdminUi.metrics(
                String.valueOf(customers.size()), "Total customers",
                String.valueOf(activeCount), "Active accounts",
                String.valueOf(suspendedCount), "Suspended accounts"));
        metrics.revalidate();
        metrics.repaint();

        for (model.User u : customers) {
            String statusDisplay = u.getStatus() != null ? u.getStatus().name() : "UNKNOWN";
            if (!u.isEmailVerified() && model.AccountStatus.ACTIVE == u.getStatus()) statusDisplay = "UNVERIFIED";
            String name = (u.getFullName() == null || u.getFullName().isEmpty()) ? u.getUsername() : u.getFullName();
            String contact = (u.getContactNumber() == null || u.getContactNumber().isEmpty()) ? "N/A" : u.getContactNumber();
            String joined = u.getRegisteredAt() != null ? u.getRegisteredAt().toLocalDate().toString() : "Unknown";
            tableModel.addRow(new Object[]{name, u.getEmail(), contact, joined, statusDisplay});
        }

        footer.setText(customers.size() + " customer(s).");
        updateButtons();
    }

    private static final class Ui {
        static final Color INK   = new Color(28, 31, 27);
        static final Color MUTED = new Color(99, 106, 96);
        static final Color FOREST = new Color(55, 70, 56);

        static Font font(int size, int style) { return new Font("Fira Code", style, size); }
        static JLabel label(String v, int size, int style, Color color) {
            JLabel l = new JLabel(v); l.setFont(font(size, style)); l.setForeground(color); return l;
        }
        static void addLeft(JPanel p, JComponent c) { c.setAlignmentX(Component.LEFT_ALIGNMENT); p.add(c); }
    }
}
