package gui;

import java.awt.*;
import javax.swing.*;
import service.AuthService;

/** Shared modern dashboard visual for QueueTees user roles. */
public final class DashboardLayout {
    private static final Color INK = new Color(48, 55, 47);
    private static final Color MUTED = new Color(94, 105, 93);
    private static final Color ACCENT = new Color(75, 98, 78);
    private static final Color CARD = new Color(255, 255, 252, 225);

    private DashboardLayout() { }

    public static JPanel create(AuthService authService, String role) {
        JPanel root = new JPanel(null) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setPaint(new GradientPaint(0, 0, new Color(0xF1, 0xF1, 0xE8),
                        0, getHeight(), new Color(0x91, 0x9B, 0x91)));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(new Color(255, 255, 255, 30));
                g2.fillOval(680, -250, 520, 520);
                g2.dispose();
            }
        };

        JPanel sidebar = card(new Color(43, 54, 43, 225), 30);
        sidebar.setBounds(22, 22, 210, 665);
        root.add(sidebar);
        JLabel brand = text("QUEUETEES", 19, Font.BOLD, new Color(248, 248, 241));
        brand.setBounds(26, 30, 160, 30); sidebar.add(brand);
        JLabel tagline = text("QUEUE WITH EASE", 10, Font.PLAIN, new Color(217, 225, 210));
        tagline.setBounds(26, 58, 160, 20); sidebar.add(tagline);
        String[] nav = moduleNavigation(role);
        for (int i = 0; i < nav.length; i++) {
            JLabel item = text((i == 0 ? "●  " : "○  ") + nav[i], 14, i == 0 ? Font.BOLD : Font.PLAIN,
                    i == 0 ? Color.WHITE : new Color(220, 228, 215));
            item.setBounds(26, 132 + (48 * i), 160, 34);
            if (i == 0) { item.setOpaque(true); item.setBackground(new Color(255, 255, 255, 38));
                item.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0)); }
            sidebar.add(item);
        }
        JLabel support = text("Need help?", 12, Font.BOLD, new Color(237, 242, 232));
        support.setBounds(26, 510, 150, 20); sidebar.add(support);
        JLabel supportText = text("Contact the QueueTees team", 10, Font.PLAIN, new Color(202, 213, 198));
        supportText.setBounds(26, 534, 170, 20); sidebar.add(supportText);
        RoundedButton logout = new RoundedButton("Log Out", new Color(244, 246, 239), INK);
        logout.setFont(new Font("SansSerif", Font.BOLD, 13)); logout.setBounds(26, 587, 158, 42);
        logout.setHoverColor(new Color(218, 228, 211));
        logout.addActionListener(e -> { Window w = SwingUtilities.getWindowAncestor(root); if (w != null) w.dispose(); new LoginFrame(authService).setVisible(true); });
        sidebar.add(logout);

        JLabel greeting = text("Good day!", 14, Font.PLAIN, MUTED); greeting.setBounds(265, 42, 180, 22); root.add(greeting);
        JLabel heading = text(role + " DASHBOARD", 28, Font.BOLD, INK); heading.setBounds(265, 64, 460, 42); root.add(heading);
        JLabel badge = text(role + "  •  ONLINE", 11, Font.BOLD, ACCENT);
        badge.setOpaque(true); badge.setHorizontalAlignment(SwingConstants.CENTER); badge.setBackground(new Color(238, 243, 231, 200)); badge.setBounds(875, 51, 180, 34); root.add(badge);

        JPanel queue = card(CARD, 26); queue.setBounds(265, 130, 790, 145); root.add(queue);
        String qTitle = queueTitle(role);
        JLabel queueTitle = text(qTitle, 16, Font.BOLD, INK); queueTitle.setBounds(28, 23, 250, 25); queue.add(queueTitle);
        JLabel queueCopy = text(queueDescription(role), 12, Font.PLAIN, MUTED);
        queueCopy.setBounds(28, 51, 430, 22); queue.add(queueCopy);
        JLabel queueValue = text(queueValue(role), 42, Font.BOLD, ACCENT);
        queueValue.setHorizontalAlignment(SwingConstants.CENTER); queueValue.setBounds(590, 25, 150, 50); queue.add(queueValue);
        JLabel queueType = text(queueLabel(role), 10, Font.BOLD, MUTED);
        queueType.setHorizontalAlignment(SwingConstants.CENTER); queueType.setBounds(570, 78, 190, 20); queue.add(queueType);
        JLabel live = text("●  LIVE UPDATES ENABLED", 11, Font.BOLD, new Color(63, 123, 74)); live.setBounds(28, 100, 250, 20); queue.add(live);

        String[] metricTitles = moduleMetrics(role);
        String[] metricValues = moduleValues(role);
        for (int i = 0; i < 3; i++) {
            JPanel metric = card(CARD, 22); metric.setBounds(265 + i * 267, 298, 245, 118); root.add(metric);
            JLabel value = text(metricValues[i], 28, Font.BOLD, INK); value.setBounds(20, 20, 205, 38); metric.add(value);
            JLabel desc = text(metricTitles[i], 12, Font.PLAIN, MUTED); desc.setBounds(20, 64, 205, 22); metric.add(desc);
        }
        JPanel activity = card(CARD, 26); activity.setBounds(265, 440, 520, 225); root.add(activity);
        JLabel activityTitle = text("Recent activity", 16, Font.BOLD, INK); activityTitle.setBounds(25, 20, 230, 25); activity.add(activityTitle);
        String[] updates = moduleActivity(role);
        for (int i = 0; i < updates.length; i++) { JLabel update = text("●  " + updates[i], 12, Font.PLAIN, INK); update.setBounds(26, 60 + i * 42, 420, 22); activity.add(update); }
        JPanel quick = card(new Color(72, 92, 73, 220), 26); quick.setBounds(808, 440, 247, 225); root.add(quick);
        JLabel quickTitle = text("Quick action", 16, Font.BOLD, Color.WHITE); quickTitle.setBounds(24, 22, 180, 25); quick.add(quickTitle);
        JLabel quickText = text(actionDescription(role), 12, Font.PLAIN, new Color(229, 235, 222)); quickText.setBounds(24, 58, 190, 42); quick.add(quickText);
        RoundedButton action = new RoundedButton(actionLabel(role), Color.WHITE, ACCENT);
        action.setFont(new Font("SansSerif", Font.BOLD, 13)); action.setBounds(24, 148, 195, 42); action.setHoverColor(new Color(223, 233, 216)); quick.add(action);
        return root;
    }

    private static String[] moduleNavigation(String role) { if (role.equals("ADMIN")) return new String[] { "Overview", "Staff approvals", "Products & stock", "Customer accounts", "Order queue", "Sales reports" }; if (role.equals("STAFF")) return new String[] { "Overview", "Order queue", "Order details", "Process order", "Queue status" }; return new String[] { "Overview", "Browse products", "My cart & checkout", "Track my order", "Order history" }; }
    private static String queueTitle(String role) { return role.equals("ADMIN") ? "Order queue monitoring" : role.equals("STAFF") ? "Next order to process" : "Order status and queue"; }
    private static String queueDescription(String role) { return role.equals("ADMIN") ? "Monitor every order as it moves from pending to completed." : role.equals("STAFF") ? "Open the next FCFS order, review its details, and begin processing." : "Track your confirmed order after checkout and payment verification."; }
    private static String queueValue(String role) { return role.equals("CUSTOMER") ? "A-014" : role.equals("STAFF") ? "A-013" : "24"; }
    private static String queueLabel(String role) { return role.equals("CUSTOMER") ? "QUEUE NUMBER" : role.equals("STAFF") ? "NOW SERVING" : "PENDING ORDERS"; }
    private static String[] moduleMetrics(String role) { if (role.equals("ADMIN")) return new String[] { "Staff approvals", "Low-stock alerts", "Completed orders" }; if (role.equals("STAFF")) return new String[] { "Orders waiting", "Processing now", "Completed today" }; return new String[] { "Items in cart", "My queue ticket", "Order history" }; }
    private static String[] moduleValues(String role) { if (role.equals("ADMIN")) return new String[] { "03", "07", "96" }; if (role.equals("STAFF")) return new String[] { "24", "A-013", "18" }; return new String[] { "03", "A-014", "07" }; }
    private static String[] moduleActivity(String role) { if (role.equals("ADMIN")) return new String[] { "3 staff registrations are awaiting review", "Low-stock alert: 7 products need replenishment", "Order A-013 moved to Processing" }; if (role.equals("STAFF")) return new String[] { "Order A-013 is ready to process", "Update statuses: Pending, Processing, Completed", "New checkout added to the FCFS queue" }; return new String[] { "Order confirmed and queue ticket generated", "Payment receipt is available in your order", "Browse products or continue shopping" }; }
    private static String actionDescription(String role) { return role.equals("ADMIN") ? "Review the module that needs attention first." : role.equals("STAFF") ? "Open the FCFS queue and process the next order." : "Review your cart, check out, or track an order."; }
    private static String actionLabel(String role) { return role.equals("ADMIN") ? "Review Approvals" : role.equals("STAFF") ? "Open Order Queue" : "Browse Products"; }

    private static JPanel card(Color color, int radius) {
        JPanel panel = new JPanel(null) { @Override protected void paintComponent(Graphics g) { Graphics2D g2 = (Graphics2D) g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); g2.setColor(color); g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius); g2.dispose(); } };
        panel.setOpaque(false); return panel;
    }
    private static JLabel text(String value, int size, int style, Color color) { JLabel label = new JLabel(value); label.setFont(new Font("SansSerif", style, size)); label.setForeground(color); return label; }
}
