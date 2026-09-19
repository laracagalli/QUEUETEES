package gui.admin;

import java.awt.*;
import javax.swing.*;

/** Administrator page for reviewing pending staff registrations. */
public final class StaffApprovalsPanel extends JPanel {
    private final service.AuthService authService;
    private final model.User administrator;
    private final javax.swing.table.DefaultTableModel records = new javax.swing.table.DefaultTableModel(
            new String[]{"Name", "Email", "Date requested", "Role", "Status"}, 0) {
        public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable table = new JTable(records);
    private java.util.List<model.User> staff = java.util.Collections.emptyList();
    private final JPanel metrics = new JPanel(new BorderLayout());
    private final JLabel message = AdminUi.label("", 11, false);
    private final JButton review = AdminUi.button("Review application");

    public StaffApprovalsPanel() { this(null, null); }

    public StaffApprovalsPanel(service.AuthService authService, model.User administrator) {
        this.authService = authService;
        this.administrator = administrator;
        setOpaque(false);setLayout(new BoxLayout(this,BoxLayout.Y_AXIS));
        Ui.addLeft(this,Ui.label("STAFF",10,Font.BOLD,Ui.FOREST));add(Box.createVerticalStrut(4));
        Ui.addLeft(this,Ui.label("Staff account approvals",25,Font.BOLD,Ui.INK));add(Box.createVerticalStrut(5));
        Ui.addLeft(this,Ui.label("Review staff registrations before granting access.",11,Font.PLAIN,Ui.MUTED));add(Box.createVerticalStrut(18));
        metrics.setOpaque(false);metrics.setAlignmentX(0);
        metrics.setMaximumSize(new Dimension(Integer.MAX_VALUE,104));
        add(metrics);add(Box.createVerticalStrut(16));
        AdminUi.style(table);add(AdminUi.filters(table,"All statuses", "Pending", "Approved", "Rejected", "Suspended", "Banned"));add(Box.createVerticalStrut(16));
        JPanel card=AdminUi.tableCard(table,"Account records");
        JPanel footer = new JPanel(new BorderLayout(10, 10));footer.setOpaque(false);
        footer.add(message, BorderLayout.NORTH);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));actions.setOpaque(false);
        JButton refresh = AdminUi.button("Refresh");refresh.addActionListener(e -> refresh());
        review.addActionListener(e -> reviewSelected());
        actions.add(refresh);actions.add(review);footer.add(actions, BorderLayout.SOUTH);
        card.add(footer, BorderLayout.SOUTH);add(card);
        table.getSelectionModel().addListSelectionListener(e -> updateReview());
        refresh();
    }

    private model.User selected() {
        int row = table.getSelectedRow();
        return row < 0 ? null : staff.get(table.convertRowIndexToModel(row));
    }

    private void updateReview() {
        model.User selected = selected();
        review.setEnabled(selected != null && selected.getStatus() == model.AccountStatus.PENDING_APPROVAL);
    }

    public void refresh() {
        table.clearSelection();
        records.setRowCount(0);
        String error = null;
        try {
            staff = authService == null ? java.util.Collections.emptyList() : authService.getStaffApplications(administrator);
        } catch (IllegalStateException ex) { staff = java.util.Collections.emptyList();error = ex.getMessage(); }
        for (model.User user : staff) {
            records.addRow(new Object[]{user.getFullName().isEmpty() ? user.getUsername() : user.getFullName(),
                    user.getEmail(), user.getRegisteredAt().format(java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy HH:mm")),
                    "Staff", statusLabel(user.getStatus())});
        }
        metrics.removeAll();
        metrics.add(AdminUi.metrics(count(model.AccountStatus.PENDING_APPROVAL), "Pending approvals",
                count(model.AccountStatus.ACTIVE), "Approved accounts", count(model.AccountStatus.REJECTED), "Rejected accounts"));
        metrics.revalidate();metrics.repaint();
        message.setText(error != null ? error : staff.isEmpty() ? "No staff applications yet." : staff.size() + " staff account(s). Select a pending application to review.");
        updateReview();
    }

    private String count(model.AccountStatus status) {
        return String.valueOf(staff.stream().filter(u -> u.getStatus() == status).count());
    }

    private static String statusLabel(model.AccountStatus status) {
        switch (status) {
            case PENDING_APPROVAL: return "Pending";
            case ACTIVE: return "Approved";
            case REJECTED: return "Rejected";
            case SUSPENDED: return "Suspended";
            case BANNED: return "Banned";
            default: return status.toString();
        }
    }

    private void reviewSelected() {
        model.User user = selected();
        if (user == null || user.getStatus() != model.AccountStatus.PENDING_APPROVAL) return;
        JPanel details = new JPanel(new GridLayout(0, 2, 14, 10));
        details.setBackground(AdminUi.PAPER);
        String[] fields = {"Full name", user.getFullName().isEmpty() ? user.getUsername() : user.getFullName(),
                "Username", user.getUsername(), "Email", user.getEmail(), "Contact", user.getContactNumber(),
                "Address", user.getAddress(), "Gender", user.getGender(), "Birthday", user.getBirthday() == null ? "—" : user.getBirthday().toString()};
        for (int i = 0; i < fields.length; i += 2) {
            details.add(AdminUi.label(fields[i], 11, true));
            JTextField value = new JTextField(fields[i+1], 24);value.setEditable(false);
            value.setFont(new Font("Fira Code", Font.PLAIN, 11));value.setBackground(AdminUi.PAPER);
            details.add(value);
        }
        Object[] options = {"Approve", "Reject", "Cancel"};
        int choice = JOptionPane.showOptionDialog(this, details, "Review staff application", JOptionPane.DEFAULT_OPTION,
                JOptionPane.PLAIN_MESSAGE, null, options, options[2]);
        if (choice != 0 && choice != 1) return;
        try {
            authService.reviewStaff(administrator, user.getId(), choice == 0);
            refresh();
            message.setText("Application " + (choice == 0 ? "approved. Staff can now sign in." : "rejected. Staff access remains blocked."));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            refresh();
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Staff approval", JOptionPane.WARNING_MESSAGE);
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

        static Font font(int size, int style) { return new Font("Fira Code", style, size);
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
            gui.components.RoundedButton button = new gui.components.RoundedButton(title, INK, Color.WHITE);
            button.setFont(font(11, Font.BOLD));
            button.setHoverColor(new Color(74, 91, 74));
            button.setPreferredSize(new Dimension(135, 38));
            return button;
        }
        static gui.components.RoundedButton lightButton(String title) {
            gui.components.RoundedButton button = new gui.components.RoundedButton(title, CREAM, INK);
            button.setFont(font(11, Font.BOLD));
            button.setHoverColor(new Color(218, 225, 211));
            return button;
        }
        static NavButton navButton(String title) { return new NavButton(title);
        }
        static void confirmLogout(Component parent, service.AuthService authService) {
            int choice = JOptionPane.showConfirmDialog(parent, "Log out of QueueTees?", "Confirm Log Out", JOptionPane.YES_NO_OPTION);
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
