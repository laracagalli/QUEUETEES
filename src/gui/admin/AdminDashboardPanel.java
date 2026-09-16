package gui.admin;

import gui.components.RoundedButton;
import java.awt.*;
import java.util.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import service.AuthService;
import service.StoreService;

/** All administrator navigation and pages. */
public final class AdminDashboardPanel extends JPanel {
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel content = new JPanel(cardLayout);
    private final Map<String, Ui.NavButton> navigation = new LinkedHashMap<>();
    private final AuthService authService;
    private final model.User currentUser;
    private final ProductManagementPanel productPanel;
    private final SalesReportsPanel salesPanel;
    private final AdminOrdersPanel queuePanel;
    private final StaffApprovalsPanel staffPanel;

    // Dynamic Metric Labels
    private final JLabel pendingMetricLabel = Ui.label("—", 24, Font.BOLD, Ui.FOREST);
    private final JLabel staffMetricLabel = Ui.label("—", 24, Font.BOLD, Ui.FOREST);
    private final JLabel stockMetricLabel = Ui.label("—", 24, Font.BOLD, Ui.FOREST);

    public AdminDashboardPanel(AuthService authService) {
        this(authService, null);
    }

    public AdminDashboardPanel(AuthService authService, model.User user) {
        this.authService = authService;
        this.currentUser = user;
        this.productPanel = new ProductManagementPanel(user);
        this.salesPanel = new SalesReportsPanel(user);
        this.queuePanel = new AdminOrdersPanel(false, user);

        setLayout(new BorderLayout());
        add(createSidebar(), BorderLayout.WEST);

        content.setOpaque(false);
        content.setBorder(new EmptyBorder(30, 36, 30, 36));
        add(content);

        content.add(createDashboardPanel(), "overview");
        this.staffPanel = new StaffApprovalsPanel(authService, user);
        content.add(staffPanel, "staff");
        content.add(productPanel, "products");
        content.add(new CustomerManagementPanel(authService, user), "customers");
        content.add(queuePanel, "queue");
        content.add(salesPanel, "reports");
        content.add(createProfilePanel(user), "profile");

        setOpaque(false);
        showPanel("overview");
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setPaint(new GradientPaint(0, 0, Ui.CREAM, 0, getHeight(), Ui.SAGE));
        g2.fillRect(0, 0, getWidth(), getHeight());
        g2.dispose();
    }

    private JPanel createSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setBackground(Ui.FOREST);
        sidebar.setPreferredSize(new Dimension(230, 0));
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBorder(new EmptyBorder(24, 22, 22, 22));

        JLabel logo = Ui.logo(168, 68);
        logo.setAlignmentX(Component.LEFT_ALIGNMENT);
        sidebar.add(logo);
        sidebar.add(Box.createVerticalStrut(8));
        Ui.addLeft(sidebar, Ui.label("QUEUETEES: A Queuing Management System", 8, Font.BOLD, Color.WHITE));
        sidebar.add(Box.createVerticalStrut(30));
        addNavigation(sidebar, "Overview", "overview");
        addNavigation(sidebar, "Staff Approvals", "staff");
        addNavigation(sidebar, "Products & Stock", "products");
        addNavigation(sidebar, "Customers", "customers");
        addNavigation(sidebar, "Order Queue", "queue");
        addNavigation(sidebar, "Sales Reports", "reports");
        addNavigation(sidebar, "My Account", "profile");
        sidebar.add(Box.createVerticalGlue());
        Ui.addLeft(sidebar, Ui.label("ADMIN  •  ONLINE", 10, Font.BOLD, new Color(221, 230, 216)));
        sidebar.add(Box.createVerticalStrut(14));
        RoundedButton logout = Ui.lightButton("Log Out");
        logout.setPreferredSize(new Dimension(186, 40));
        logout.setMaximumSize(new Dimension(186, 40));
        logout.setAlignmentX(Component.LEFT_ALIGNMENT);
        logout.addActionListener(e -> Ui.confirmLogout(sidebar, authService));
        sidebar.add(logout);
        return sidebar;
    }

    private void addNavigation(JPanel sidebar, String title, String key) {
        Ui.NavButton button = Ui.navButton(title);
        button.setPreferredSize(new Dimension(186, 36));
        button.setMinimumSize(new Dimension(186, 36));
        button.setMaximumSize(new Dimension(186, 36));
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        button.addActionListener(e -> showPanel(key));
        navigation.put(key, button);
        sidebar.add(button);
        sidebar.add(Box.createVerticalStrut(5));
    }

    private void showPanel(String key) {
        cardLayout.show(content, key);
        if ("overview".equals(key))
            refreshOverview();
        if ("staff".equals(key))
            staffPanel.refresh();
        if ("products".equals(key))
            productPanel.refresh();
        if ("reports".equals(key))
            salesPanel.refresh();
        if ("queue".equals(key))
            queuePanel.refresh();
        navigation.forEach((name, button) -> button.setSelectedState(name.equals(key)));
    }

    private void refreshOverview() {
        // Calculate Pending Orders (Confirmed or Preparing)
        try {
            long pending = StoreService.getInstance().getOrders().stream()
                    .filter(o -> o.getStatus() == model.OrderStatus.CONFIRMED
                            || o.getStatus() == model.OrderStatus.PREPARING)
                    .count();
            pendingMetricLabel.setText(String.valueOf(pending));
        } catch (Exception e) {
            pendingMetricLabel.setText("—");
        }

        // Calculate Pending Staff Approvals
        try {
            long staffCount = authService != null ? authService.getStaffApplications(currentUser).stream()
                    .filter(u -> u.getStatus() == model.AccountStatus.PENDING_APPROVAL)
                    .count() : 0;
            staffMetricLabel.setText(String.valueOf(staffCount));
        } catch (Exception e) {
            staffMetricLabel.setText("—");
        }

        // Calculate Low Stock Items (Stock threshold <= 5)
        try {
            long lowStock = StoreService.getInstance().getProducts().stream()
                    .filter(p -> p.getStock() <= 5)
                    .count();
            stockMetricLabel.setText(String.valueOf(lowStock));
        } catch (Exception e) {
            stockMetricLabel.setText("—");
        }
    }

    private JPanel createDashboardPanel() {
        JPanel page = Ui.page("Good day!", "Administrator dashboard", "Monitor QueueTees operations from one place.");
        JPanel hero = Ui.card(Ui.FOREST, 24, false);
        hero.setLayout(new BorderLayout(20, 0));
        hero.setBorder(new EmptyBorder(22, 25, 22, 25));
        JPanel copy = Ui.verticalBox();
        copy.add(Ui.label("Order queue overview", 17, Font.BOLD, Color.WHITE));
        copy.add(Box.createVerticalStrut(8));
        copy.add(Ui.label("Operational summaries appear when data is connected.", 11, Font.PLAIN,
                new Color(220, 227, 216)));
        hero.add(copy);
        RoundedButton open = Ui.lightButton("View Queue");
        open.setPreferredSize(new Dimension(130, 36));
        open.setMinimumSize(new Dimension(130, 36));
        open.setMaximumSize(new Dimension(130, 36));
        open.addActionListener(e -> showPanel("queue"));
        JPanel openWrap = new JPanel(new GridBagLayout());
        openWrap.setOpaque(false);
        openWrap.add(open);
        hero.add(openWrap, BorderLayout.EAST);
        hero.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));
        hero.setPreferredSize(new Dimension(1000, 110));
        page.add(hero);
        page.add(Box.createVerticalStrut(17));

        JPanel metrics = new JPanel(new GridLayout(1, 3, 15, 0));
        metrics.setOpaque(false);
        metrics.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Add dynamic metric cards
        metrics.add(Ui.metricCard(pendingMetricLabel, "Pending orders"));
        metrics.add(Ui.metricCard(staffMetricLabel, "Staff approvals"));
        metrics.add(Ui.metricCard(stockMetricLabel, "Low-stock items"));

        metrics.setMaximumSize(new Dimension(Integer.MAX_VALUE, 108));
        metrics.setPreferredSize(new Dimension(1000, 108));
        page.add(metrics);
        page.add(Box.createVerticalStrut(17));
        page.add(Ui.emptyState("Recent activity", "No operational activity is available yet."));
        return page;
    }

    private JPanel createProfilePanel(model.User user) {
        JPanel page = Ui.page("ACCOUNT", "My account", "Review the signed-in administrator account.");
        page.add(new gui.components.ProfileCard(user));
        return page;
    }

    /**
     * Styling owned by this panel so the screen can be configured independently.
     */
    private static final class Ui {
        static final Color INK = new Color(28, 31, 27);
        static final Color MUTED = new Color(99, 106, 96);
        static final Color FOREST = new Color(55, 70, 56);
        static final Color SAGE = new Color(145, 155, 145);
        static final Color CREAM = new Color(241, 241, 232);
        static final Color PAPER = new Color(252, 252, 247);
        static final Color LINE = new Color(218, 220, 209);

        static Font font(int size, int style) {
            return new Font("Fira Code", style, size);
        }

        static JLabel label(String value, int size, int style, Color color) {
            JLabel label = new JLabel(value);
            label.setFont(font(size, style));
            label.setForeground(color);
            return label;
        }

        static JLabel logo(int width, int height) {
            JLabel logo = new JLabel();
            java.net.URL url = AdminDashboardPanel.class.getResource("/Gui_Images/hirayalogo2.png");
            if (url != null) {
                Image source = new ImageIcon(url).getImage();
                logo.setIcon(new ImageIcon(source.getScaledInstance(width, height, Image.SCALE_SMOOTH)));
            }
            logo.setPreferredSize(new Dimension(width, height));
            logo.setMinimumSize(new Dimension(width, height));
            logo.setMaximumSize(new Dimension(width, height));
            return logo;
        }

        static void addLeft(JPanel parent, JComponent child) {
            child.setAlignmentX(Component.LEFT_ALIGNMENT);
            parent.add(child);
        }

        static JPanel verticalBox() {
            JPanel panel = new JPanel();
            panel.setOpaque(false);
            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
            return panel;
        }

        static JPanel page(String eyebrow, String title, String description) {
            JPanel panel = verticalBox();
            addLeft(panel, label(eyebrow, 10, Font.BOLD, FOREST));
            panel.add(Box.createVerticalStrut(4));
            addLeft(panel, label(title, 25, Font.BOLD, INK));
            panel.add(Box.createVerticalStrut(5));
            addLeft(panel, label(description, 11, Font.PLAIN, MUTED));
            panel.add(Box.createVerticalStrut(18));
            return panel;
        }

        static JPanel card(Color color, int radius, boolean outlined) {
            JPanel panel = new JPanel() {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(color);
                    g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);
                    if (outlined) {
                        g2.setColor(LINE);
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

        // Updated to accept a JLabel reference rather than a hardcoded String
        static JPanel metricCard(JLabel valueLabel, String caption) {
            JPanel panel = card(PAPER, 20, true);
            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
            panel.setBorder(new javax.swing.border.EmptyBorder(17, 19, 15, 19));
            panel.add(valueLabel);
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

        static NavButton navButton(String title) {
            return new NavButton(title);
        }

        static void confirmLogout(Component parent, service.AuthService authService) {
            int choice = JOptionPane.showConfirmDialog(parent, "Log out of QueueTees?", "Confirm Log Out",
                    JOptionPane.YES_NO_OPTION);
            if (choice == JOptionPane.YES_OPTION) {
                Window window = SwingUtilities.getWindowAncestor(parent);
                if (window != null)
                    window.dispose();
                new gui.auth.LoginFrame(authService).setVisible(true);
            }
        }

        static final class NavButton extends JButton {
            private boolean selected, hovered;
            private float highlight;
            private float targetHighlight;
            private final javax.swing.Timer transitionTimer;

            NavButton(String title) {
                super(title);
                transitionTimer = new javax.swing.Timer(16, e -> animateHighlight());
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
                    @Override
                    public void mouseEntered(java.awt.event.MouseEvent e) {
                        hovered = true;
                        setHighlightTarget(selected ? 1f : 0.58f);
                    }

                    @Override
                    public void mouseExited(java.awt.event.MouseEvent e) {
                        hovered = false;
                        setHighlightTarget(selected ? 1f : 0f);
                    }
                });
            }

            void setSelectedState(boolean value) {
                selected = value;
                setFont(font(12, value ? Font.BOLD : Font.PLAIN));
                setForeground(value ? Color.WHITE : new Color(224, 230, 219));
                setHighlightTarget(value ? 1f : (hovered ? 0.58f : 0f));
            }

            private void setHighlightTarget(float value) {
                targetHighlight = value;
                transitionTimer.start();
            }

            private void animateHighlight() {
                highlight += (targetHighlight - highlight) * 0.28f;
                if (Math.abs(targetHighlight - highlight) < 0.02f) {
                    highlight = targetHighlight;
                    transitionTimer.stop();
                }
                repaint();
            }

            @Override
            protected void paintComponent(Graphics g) {
                if (highlight > 0f) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setColor(new Color(255, 255, 255, Math.round(34 * highlight)));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                    g2.dispose();
                }
                super.paintComponent(g);
            }
        }
    }
}