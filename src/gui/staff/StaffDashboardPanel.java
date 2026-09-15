package gui.staff;

import gui.RoundedButton;
import java.awt.*;
import java.util.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import service.AuthService;

/** All staff navigation and operational pages. */
public final class StaffDashboardPanel extends JPanel {
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel content = new JPanel(cardLayout);
    private final Map<String, Ui.NavButton> navigation = new LinkedHashMap<>();
    private final AuthService authService;
    private final OrderQueuePanel queuePanel = new OrderQueuePanel();
    private final CompletedOrdersPanel completedPanel;
    private final OrderDetailsPanel detailsPanel = new OrderDetailsPanel();
    private final QueueStatusPanel statusPanel = new QueueStatusPanel();

    private final StaffOverviewPanel overviewPanel = new StaffOverviewPanel(() -> showPanel("queue"));
    private final javax.swing.Timer refreshTimer = new javax.swing.Timer(2000, e -> refreshVisiblePanel());
    private String currentPanel = "overview";

    public StaffDashboardPanel(AuthService authService, model.User user) {
        this.authService = authService;
        completedPanel = new CompletedOrdersPanel(user);
        setLayout(new BorderLayout());
        add(createSidebar(), BorderLayout.WEST);
        content.setOpaque(false);
        content.setBorder(new EmptyBorder(30, 36, 30, 36));
        add(content);
        content.add(overviewPanel, "overview");
        content.add(queuePanel, "queue");
        content.add(detailsPanel, "orders");
        content.add(completedPanel, "completed");
        content.add(statusPanel, "status");
        content.add(new StaffProfilePanel(user), "profile");
        setOpaque(false);
        showPanel("overview");
    }

    @Override protected void paintComponent(Graphics g) {
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
        addNavigation(sidebar, "Order Queue", "queue");
        addNavigation(sidebar, "Order Details", "orders");
        addNavigation(sidebar, "Completed Orders", "completed");
        addNavigation(sidebar, "Queue Status", "status");
        addNavigation(sidebar, "My Account", "profile");
        sidebar.add(Box.createVerticalGlue());
        Ui.addLeft(sidebar, Ui.label("STAFF  •  ONLINE", 10, Font.BOLD, new Color(221, 230, 216)));
        sidebar.add(Box.createVerticalStrut(14));
        RoundedButton logout = Ui.lightButton("Log Out");
        logout.setPreferredSize(new Dimension(186, 40));
        logout.setMaximumSize(new Dimension(186, 40));
        logout.setAlignmentX(Component.LEFT_ALIGNMENT);
        logout.addActionListener(e -> Ui.confirmLogout(sidebar, authService));
        sidebar.add(logout);
        return sidebar;
    }

    private void addNavigation(JPanel sidebar, String title, String key) { Ui.NavButton button = Ui.navButton(title);
        button.setPreferredSize(new Dimension(186, 36));
        button.setMinimumSize(new Dimension(186, 36));
        button.setMaximumSize(new Dimension(186, 36));
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        button.addActionListener(e -> showPanel(key));
        navigation.put(key, button);
        sidebar.add(button);
        sidebar.add(Box.createVerticalStrut(5));
    }
    @Override public void addNotify() {
        super.addNotify();
        refreshTimer.start();
    }

    @Override public void removeNotify() {
        refreshTimer.stop();
        super.removeNotify();
    }

    private void refreshVisiblePanel() {
        for (Window window : Window.getWindows())
            if (window instanceof JDialog && window.isShowing() && ((JDialog) window).isModal()) return;
        refreshPanel(currentPanel);
    }

    private void showPanel(String key) {
        currentPanel = key;
        cardLayout.show(content, key);
        refreshPanel(key);
        navigation.forEach((name, button) -> button.setSelectedState(name.equals(key)));
    }

    private void refreshPanel(String key) {
        if ("overview".equals(key)) overviewPanel.refresh();
        if ("queue".equals(key)) queuePanel.refresh();
        if ("orders".equals(key)) detailsPanel.refresh();
        if ("completed".equals(key)) completedPanel.refresh();
        if ("status".equals(key)) statusPanel.refresh();
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
        static JLabel logo(int width, int height) {
            JLabel logo = new JLabel();
            java.net.URL url = StaffDashboardPanel.class.getResource("/Gui_Images/hirayalogo2.png");
            if (url != null) {
                Image source = new ImageIcon(url).getImage();
                logo.setIcon(new ImageIcon(source.getScaledInstance(width, height, Image.SCALE_SMOOTH)));
            }
            logo.setPreferredSize(new Dimension(width, height));
            logo.setMinimumSize(new Dimension(width, height));
            logo.setMaximumSize(new Dimension(width, height));
            return logo;
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
            gui.RoundedButton button = new gui.RoundedButton(title, CREAM, INK);
            button.setFont(font(11, Font.BOLD));
            button.setHoverColor(new Color(218, 225, 211));
            return button;
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
                    @Override public void mouseEntered(java.awt.event.MouseEvent e) { hovered = true;
                        setHighlightTarget(selected ? 1f : 0.58f);
                    }
                    @Override public void mouseExited(java.awt.event.MouseEvent e) { hovered = false;
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
            @Override protected void paintComponent(Graphics g) {
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
