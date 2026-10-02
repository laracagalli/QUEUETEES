package gui.admin;

import gui.components.RoundedButton;
import java.awt.*;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.*;
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
    private final CustomerManagementPanel customerPanel;

    // Dynamic Metric Labels
    private final JLabel pendingMetricLabel = Ui.label("—", 20, Font.BOLD, Ui.FOREST);
    private final JLabel staffMetricLabel = Ui.label("—", 20, Font.BOLD, Ui.FOREST);
    private final JLabel stockMetricLabel = Ui.label("—", 20, Font.BOLD, Ui.FOREST);

    // Dynamic Overview Containers
    private final JPanel lowStockContainer = new JPanel(new BorderLayout());
    private final JPanel activityContainer = new JPanel(new BorderLayout());

    // Dynamic Profile Container
    private final JPanel profileContainer = new JPanel(new BorderLayout());

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
        content.setBorder(new EmptyBorder(20, 24, 20, 24));
        add(content);

        content.add(createDashboardPanel(), "overview");
        this.staffPanel = new StaffApprovalsPanel(authService, user);
        content.add(staffPanel, "staff");
        content.add(productPanel, "products");
        this.customerPanel = new CustomerManagementPanel(authService, user);
        content.add(customerPanel, "customers");
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
        sidebar.setPreferredSize(new Dimension(220, 0));
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBorder(new EmptyBorder(20, 18, 18, 18));

        JLabel logo = Ui.logo(150, 58);
        logo.setAlignmentX(Component.LEFT_ALIGNMENT);
        sidebar.add(logo);
        sidebar.add(Box.createVerticalStrut(6));
        Ui.addLeft(sidebar, Ui.label("QUEUETEES: A Queuing Management System", 8, Font.BOLD, Color.WHITE));
        sidebar.add(Box.createVerticalStrut(20));
        addNavigation(sidebar, "Overview", "overview");
        addNavigation(sidebar, "Staff Approvals", "staff");
        addNavigation(sidebar, "Products & Stock", "products");
        addNavigation(sidebar, "Customers", "customers");
        addNavigation(sidebar, "Order Queue", "queue");
        addNavigation(sidebar, "Sales Reports", "reports");
        addNavigation(sidebar, "My Account", "profile");
        sidebar.add(Box.createVerticalGlue());
        Ui.addLeft(sidebar, Ui.label("ADMIN  •  ONLINE", 10, Font.BOLD, new Color(221, 230, 216)));
        sidebar.add(Box.createVerticalStrut(10));
        RoundedButton logout = Ui.lightButton("Log Out");
        logout.setPreferredSize(new Dimension(180, 34));
        logout.setMaximumSize(new Dimension(180, 34));
        logout.setAlignmentX(Component.LEFT_ALIGNMENT);
        logout.addActionListener(e -> Ui.confirmLogout(sidebar, authService, currentUser));
        sidebar.add(logout);
        return sidebar;
    }

    private void addNavigation(JPanel sidebar, String title, String key) {
        Ui.NavButton button = Ui.navButton(title);
        button.setPreferredSize(new Dimension(180, 32));
        button.setMinimumSize(new Dimension(180, 32));
        button.setMaximumSize(new Dimension(180, 32));
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        button.addActionListener(e -> showPanel(key));
        navigation.put(key, button);
        sidebar.add(button);
        sidebar.add(Box.createVerticalStrut(3));
    }

    private void showPanel(String key) {
        cardLayout.show(content, key);
        if ("overview".equals(key)) {
            refreshOverview();
            SwingUtilities.invokeLater(() -> activityContainer.repaint());
        }
        if ("staff".equals(key))
            staffPanel.refresh();
        if ("products".equals(key))
            productPanel.refresh();
        if ("reports".equals(key))
            salesPanel.refresh();
        if ("customers".equals(key))
            customerPanel.refresh();
        if ("queue".equals(key))
            queuePanel.refresh();
        navigation.forEach((name, button) -> button.setSelectedState(name.equals(key)));
    }

    private void refreshOverview() {
        try {
            long pending = StoreService.getInstance().getOrders().stream()
                    .filter(o -> o.getStatus() == model.OrderStatus.CONFIRMED
                            || o.getStatus() == model.OrderStatus.PREPARING)
                    .count();
            pendingMetricLabel.setText(String.valueOf(pending));
        } catch (Exception e) {
            pendingMetricLabel.setText("—");
        }

        try {
            long staffCount = authService != null ? authService.getStaffApplications(currentUser).stream()
                    .filter(u -> u.getStatus() == model.AccountStatus.PENDING_APPROVAL)
                    .count() : 0;
            staffMetricLabel.setText(String.valueOf(staffCount));
        } catch (Exception e) {
            staffMetricLabel.setText("—");
        }

        try {
            long lowStock = StoreService.getInstance().getProducts().stream()
                    .filter(p -> p.getStock() <= 5)
                    .count();
            stockMetricLabel.setText(String.valueOf(lowStock));
        } catch (Exception e) {
            stockMetricLabel.setText("—");
        }

        refreshLowStockItems();
        refreshRecentActivity();
    }

    private void refreshLowStockItems() {
        lowStockContainer.removeAll();
        java.util.List<model.Product> lowStockProducts = new ArrayList<>();
        try {
            lowStockProducts = StoreService.getInstance().getProducts().stream()
                    .filter(p -> p.getStock() <= 5)
                    .toList();
        } catch (Exception e) {
            // Keep empty list if error occurs
        }

        if (lowStockProducts.isEmpty()) {
            lowStockContainer.setVisible(false);
        } else {
            lowStockContainer.setVisible(true);

            JPanel card = Ui.card(Ui.PAPER, 22, true);
            card.setLayout(new BorderLayout());
            card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 150));
            card.setPreferredSize(new Dimension(1000, 150));

            JPanel heading = new JPanel(new BorderLayout());
            heading.setOpaque(false);
            heading.setBorder(new EmptyBorder(12, 18, 8, 18));

            JLabel titleLabel = Ui.label("Low-Stock Items Alert", 15, Font.BOLD, new Color(180, 45, 35));
            heading.add(titleLabel, BorderLayout.WEST);

            RoundedButton manageBtn = Ui.primaryButton("Manage Products");
            manageBtn.setPreferredSize(new Dimension(145, 30));
            manageBtn.addActionListener(e -> showPanel("products"));
            heading.add(manageBtn, BorderLayout.EAST);

            card.add(heading, BorderLayout.NORTH);

            JPanel itemsPanel = new JPanel();
            itemsPanel.setLayout(new BoxLayout(itemsPanel, BoxLayout.X_AXIS));
            itemsPanel.setOpaque(false);
            itemsPanel.setBorder(new EmptyBorder(4, 18, 14, 18));

            for (int i = 0; i < lowStockProducts.size(); i++) {
                if (i > 0) {
                    itemsPanel.add(Box.createHorizontalStrut(12));
                }
                itemsPanel.add(createLowStockItemCard(lowStockProducts.get(i)));
            }

            JScrollPane scroll = new gui.components.ModernScrollPane(itemsPanel);
            scroll.setBorder(null);
            scroll.setOpaque(false);
            scroll.getViewport().setOpaque(false);
            scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
            scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

            card.add(scroll, BorderLayout.CENTER);
            lowStockContainer.add(card, BorderLayout.CENTER);
        }

        lowStockContainer.revalidate();
        lowStockContainer.repaint();
    }

    private JPanel createLowStockItemCard(model.Product p) {
        JPanel panel = Ui.card(new Color(248, 248, 242), 12, true);
        panel.setLayout(new BorderLayout(12, 0));
        panel.setBorder(new EmptyBorder(8, 10, 8, 14));
        panel.setPreferredSize(new Dimension(240, 68));
        panel.setMaximumSize(new Dimension(240, 68));
        panel.setMinimumSize(new Dimension(240, 68));

        JPanel imgContainer = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(230, 233, 225));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        imgContainer.setOpaque(false);
        imgContainer.setPreferredSize(new Dimension(52, 52));
        imgContainer.setMinimumSize(new Dimension(52, 52));
        imgContainer.setMaximumSize(new Dimension(52, 52));
        imgContainer.setLayout(new GridBagLayout());

        JLabel imgLabel = new JLabel();
        imgLabel.setHorizontalAlignment(SwingConstants.CENTER);

        boolean imageLoaded = false;
        String path = p.getImagePath();
        if (path != null && !path.trim().isEmpty()) {
            path = path.trim();
            Image loadedImage = null;

            // 1. Try file path on disk
            File imgFile = new File(path);
            if (imgFile.exists()) {
                try {
                    loadedImage = ImageIO.read(imgFile);
                } catch (Exception ignored) {
                }
            }

            // 2. Fallback to classpath resource
            if (loadedImage == null) {
                try {
                    java.net.URL url = AdminDashboardPanel.class.getResource(path);
                    if (url == null && !path.startsWith("/")) {
                        url = AdminDashboardPanel.class.getResource("/" + path);
                    }
                    if (url != null) {
                        loadedImage = ImageIO.read(url);
                    }
                } catch (Exception ignored) {
                }
            }

            if (loadedImage != null) {
                Image scaled = loadedImage.getScaledInstance(44, 44, Image.SCALE_SMOOTH);
                imgLabel.setIcon(new ImageIcon(scaled));
                imageLoaded = true;
            }
        }

        if (!imageLoaded) {
            imgLabel.setText("👕");
            imgLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 24));
        }
        imgContainer.add(imgLabel);

        JPanel details = Ui.verticalBox();
        details.add(Ui.label(p.getName(), 12, Font.BOLD, Ui.INK));
        details.add(Box.createVerticalStrut(3));

        String stockText = p.getStock() == 0 ? "Out of stock (0)" : "Stock: " + p.getStock() + " remaining";
        details.add(Ui.label(stockText, 11, Font.BOLD, new Color(180, 45, 35)));
        details.add(Box.createVerticalStrut(2));
        details.add(Ui.label(String.format("₱%.2f", p.getPrice()), 10, Font.PLAIN, Ui.MUTED));

        panel.add(imgContainer, BorderLayout.WEST);
        panel.add(details, BorderLayout.CENTER);

        return panel;
    }

    private void refreshRecentActivity() {
        activityContainer.removeAll();
        java.util.List<String> logs = service.ActivityLogger.getLogs();

        if (logs == null || logs.isEmpty()) {
            activityContainer.add(Ui.emptyState("Recent activity", "No operational activity is available yet."),
                    BorderLayout.CENTER);
        } else {
            JPanel card = Ui.card(Ui.PAPER, 22, true);
            card.setLayout(new BorderLayout());
            card.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
            card.setPreferredSize(new Dimension(1000, 300));
            card.setBorder(new EmptyBorder(16, 16, 16, 16));

            JPanel heading = new JPanel(new BorderLayout());
            heading.setOpaque(false);
            heading.setBorder(new EmptyBorder(0, 0, 12, 0));
            heading.add(Ui.label("Recent activity", 17, Font.BOLD, Ui.INK), BorderLayout.WEST);
            heading.add(Ui.label("All activity logs / newest first", 11, Font.PLAIN, Ui.MUTED), BorderLayout.EAST);
            card.add(heading, BorderLayout.NORTH);

            String[] columns = { "Name", "Email", "Date requested", "Role", "Activity" };
            DefaultTableModel model = new DefaultTableModel(columns, 0) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };

            for (int i = logs.size() - 1; i >= 0; i--) {
                String rawLog = logs.get(i);
                LogDetails logData = LogDetails.parse(rawLog, currentUser);
                model.addRow(new Object[] {
                        logData.name,
                        logData.email,
                        logData.date,
                        logData.role,
                        logData.activity
                });
            }

            JTable table = new JTable(model);
            Ui.styleTable(table);
            table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

            JScrollPane scroll = new gui.components.ModernScrollPane(table);
            scroll.setBorder(null);
            scroll.setBackground(Ui.PAPER);
            scroll.getViewport().setBackground(Ui.PAPER);
            table.getTableHeader().setBackground(new Color(238, 239, 230));
            table.getTableHeader().setBorder(null);

            card.add(scroll, BorderLayout.CENTER);

            JPanel footer = new JPanel(new FlowLayout(FlowLayout.LEFT, 18, 10));
            footer.setOpaque(false);
            footer.add(Ui.label(logs.size() + " activity record(s) logged.", 11, Font.PLAIN, Ui.MUTED));
            card.add(footer, BorderLayout.SOUTH);

            activityContainer.add(card, BorderLayout.CENTER);
        }

        activityContainer.revalidate();
        activityContainer.repaint();
    }

    private JPanel createDashboardPanel() {
        JPanel page = Ui.page("Good day!", "Administrator dashboard", "Monitor QueueTees operations from one place.");

        JPanel hero = Ui.card(Ui.FOREST, 16, false);
        hero.setLayout(new BorderLayout(12, 0));
        hero.setBorder(new EmptyBorder(14, 18, 14, 18));

        JPanel copy = Ui.verticalBox();
        copy.add(Ui.label("Order queue overview", 15, Font.BOLD, Color.WHITE));
        copy.add(Box.createVerticalStrut(4));
        copy.add(Ui.label("Operational summaries appear when data is connected.", 10, Font.PLAIN,
                new Color(220, 227, 216)));
        hero.add(copy);

        RoundedButton open = Ui.lightButton("View Queue");
        open.setPreferredSize(new Dimension(115, 32));
        open.setMinimumSize(new Dimension(115, 32));
        open.setMaximumSize(new Dimension(115, 32));
        open.addActionListener(e -> showPanel("queue"));

        JPanel openWrap = new JPanel(new GridBagLayout());
        openWrap.setOpaque(false);
        openWrap.add(open);
        hero.add(openWrap, BorderLayout.EAST);

        hero.setMaximumSize(new Dimension(Integer.MAX_VALUE, 78));
        hero.setPreferredSize(new Dimension(1000, 78));
        page.add(hero);
        page.add(Box.createVerticalStrut(10));

        JPanel metrics = new JPanel(new GridLayout(1, 3, 12, 0));
        metrics.setOpaque(false);
        metrics.setAlignmentX(Component.LEFT_ALIGNMENT);

        metrics.add(Ui.metricCard(pendingMetricLabel, "Pending orders"));
        metrics.add(Ui.metricCard(staffMetricLabel, "Staff approvals"));
        metrics.add(Ui.metricCard(stockMetricLabel, "Low-stock items"));

        metrics.setMaximumSize(new Dimension(Integer.MAX_VALUE, 78));
        metrics.setPreferredSize(new Dimension(1000, 78));
        page.add(metrics);
        page.add(Box.createVerticalStrut(10));

        lowStockContainer.setOpaque(false);
        lowStockContainer.setAlignmentX(Component.LEFT_ALIGNMENT);
        lowStockContainer.setMaximumSize(new Dimension(Integer.MAX_VALUE, 150));
        lowStockContainer.setPreferredSize(new Dimension(1000, 150));
        page.add(lowStockContainer);
        page.add(Box.createVerticalStrut(10));

        activityContainer.setOpaque(false);
        activityContainer.setAlignmentX(Component.LEFT_ALIGNMENT);
        page.add(activityContainer);

        return page;
    }

    private JPanel createProfilePanel(model.User user) {
        JPanel page = Ui.page("ACCOUNT", "My account", "Review and update administrator account details.");
        profileContainer.setOpaque(false);
        profileContainer.setAlignmentX(Component.LEFT_ALIGNMENT);
        refreshProfileCard();
        page.add(profileContainer);
        page.add(Box.createVerticalGlue());
        return page;
    }

    private void refreshProfileCard() {
        profileContainer.removeAll();
        profileContainer.setLayout(new BoxLayout(profileContainer, BoxLayout.Y_AXIS));
        profileContainer.add(createProfileCardUI(currentUser));
        profileContainer.revalidate();
        profileContainer.repaint();
    }

    private JPanel createProfileCardUI(model.User user) {
        String username = user != null && user.getUsername() != null && !user.getUsername().trim().isEmpty()
                ? user.getUsername() : "Admin";
        String email = user != null && user.getEmail() != null ? user.getEmail()
                : username.toLowerCase() + "@queuetees.local";
        String roleStr = user != null && user.getRole() != null
                ? user.getRole().toString().substring(0, 1).toUpperCase() + user.getRole().toString().substring(1).toLowerCase()
                : "Administrator";

        JPanel card = Ui.card(Ui.PAPER, 22, true);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(20, 24, 20, 24));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        // ── Avatar + name row ──
        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 0));
        header.setOpaque(false);
        header.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel avatar = new JPanel(new GridBagLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(220, 227, 213));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.dispose();
            }
        };
        avatar.setOpaque(false);
        avatar.setPreferredSize(new Dimension(56, 56));

        JLabel avatarLabel = new JLabel();
        avatarLabel.setHorizontalAlignment(SwingConstants.CENTER);
        boolean imageLoaded = false;
        if (user != null && user.getProfilePicture() != null && !user.getProfilePicture().isEmpty()) {
            File f = new File(user.getProfilePicture());
            if (f.exists()) {
                avatarLabel.setIcon(new ImageIcon(new ImageIcon(f.getAbsolutePath())
                        .getImage().getScaledInstance(56, 56, Image.SCALE_SMOOTH)));
                imageLoaded = true;
            }
        }
        if (!imageLoaded) {
            avatarLabel.setText(username.substring(0, 1).toUpperCase());
            avatarLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
            avatarLabel.setForeground(Ui.INK);
        }
        avatar.add(avatarLabel);

        JPanel nameBox = new JPanel();
        nameBox.setLayout(new BoxLayout(nameBox, BoxLayout.Y_AXIS));
        nameBox.setOpaque(false);
        nameBox.add(Ui.label(username, 17, Font.BOLD, Ui.INK));
        nameBox.add(Box.createVerticalStrut(2));
        nameBox.add(Ui.label("Administrator account", 11, Font.PLAIN, Ui.MUTED));

        header.add(avatar);
        header.add(nameBox);
        card.add(header);
        card.add(Box.createVerticalStrut(14));

        // ── Divider ──
        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(218, 220, 209));
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        sep.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(sep);
        card.add(Box.createVerticalStrut(14));

        // ── Fields: 3 rows × 2 columns ──
        JPanel grid = new JPanel(new GridLayout(3, 2, 32, 12));
        grid.setOpaque(false);
        grid.setAlignmentX(Component.LEFT_ALIGNMENT);
        grid.add(createField("Username", username));
        grid.add(createField("Email address", email));
        grid.add(createField("Role", roleStr));
        grid.add(createField("Account status", "Active"));
        grid.add(createField("Email verification", "Verified"));
        grid.add(createField("Account ID", "1"));
        card.add(grid);
        card.add(Box.createVerticalStrut(16));

        // ── Edit button ──
        RoundedButton editBtn = Ui.primaryButton("Edit Profile");
        editBtn.setPreferredSize(new Dimension(120, 34));
        editBtn.setMaximumSize(new Dimension(120, 34));
        editBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        editBtn.addActionListener(e -> showEditProfileDialog(user));
        card.add(editBtn);

        return card;
    }

    private JPanel createField(String labelText, String valueText) {
        JPanel panel = Ui.verticalBox();
        panel.add(Ui.label(labelText, 11, Font.PLAIN, Ui.MUTED));
        panel.add(Box.createVerticalStrut(4));
        panel.add(Ui.label(valueText, 14, Font.BOLD, Ui.INK));
        return panel;
    }

    private void showEditProfileDialog(model.User user) {
        Window ancestor = SwingUtilities.getWindowAncestor(this);
        JDialog dialog = new JDialog(ancestor instanceof Frame ? (Frame) ancestor : null, "Edit Profile",
                Dialog.ModalityType.APPLICATION_MODAL);

        JPanel editCard = Ui.card(Ui.PAPER, 0, false);
        editCard.setLayout(new BoxLayout(editCard, BoxLayout.Y_AXIS));
        editCard.setBorder(new EmptyBorder(16, 18, 16, 18));

        Ui.addLeft(editCard, Ui.label("Edit Profile", 13, Font.BOLD, Ui.INK));
        editCard.add(Box.createVerticalStrut(12));

        JPanel formGrid = new JPanel(new GridLayout(2, 1, 12, 8));
        formGrid.setOpaque(false);
        formGrid.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel usernameBox = Ui.verticalBox();
        usernameBox.add(Ui.label("Username", 10, Font.BOLD, Ui.MUTED));
        usernameBox.add(Box.createVerticalStrut(3));
        JTextField usernameField = new JTextField(user != null && user.getUsername() != null ? user.getUsername() : "");
        usernameField.setFont(Ui.font(11, Font.PLAIN));
        usernameBox.add(usernameField);

        JPanel avatarBox = Ui.verticalBox();
        avatarBox.add(Ui.label("Profile Picture", 10, Font.BOLD, Ui.MUTED));
        avatarBox.add(Box.createVerticalStrut(3));

        JPanel picChooserPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        picChooserPanel.setOpaque(false);

        JLabel picPathLabel = Ui.label("No picture selected", 10, Font.PLAIN, Ui.MUTED);
        RoundedButton choosePicBtn = Ui.lightButton("Choose Picture...");

        final String[] selectedPath = new String[] { null };

        choosePicBtn.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Select Profile Picture");
            fileChooser.setFileFilter(
                    new FileNameExtensionFilter("Image Files (*.jpg, *.png, *.jpeg)", "jpg", "png", "jpeg"));
            int result = fileChooser.showOpenDialog(dialog);
            if (result == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                selectedPath[0] = file.getAbsolutePath();
                picPathLabel.setText(file.getName());
                picPathLabel.setForeground(Ui.INK);
            }
        });

        picChooserPanel.add(choosePicBtn);
        picChooserPanel.add(picPathLabel);
        avatarBox.add(picChooserPanel);

        formGrid.add(usernameBox);
        formGrid.add(avatarBox);

        editCard.add(formGrid);
        editCard.add(Box.createVerticalStrut(16));

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        actionPanel.setOpaque(false);
        actionPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        RoundedButton cancelButton = Ui.lightButton("Cancel");
        cancelButton.setPreferredSize(new Dimension(85, 32));
        cancelButton.addActionListener(e -> dialog.dispose());

        RoundedButton changePassButton = Ui.lightButton("Change Password");
        changePassButton.setPreferredSize(new Dimension(140, 32));
        changePassButton.addActionListener(e -> {
            dialog.dispose();
            gui.components.ChangePasswordDialog.show(this, authService, user);
        });

        RoundedButton saveButton = Ui.primaryButton("Save Changes");
        saveButton.setPreferredSize(new Dimension(120, 32));
        saveButton.addActionListener(e -> {
            String newUsername = usernameField.getText().trim();

            if (newUsername.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Username cannot be empty.", "Validation Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (user != null) {
                user.setUsername(newUsername);
                if (selectedPath[0] != null) {
                    user.setProfilePicture(selectedPath[0]);
                }
            }

            JOptionPane.showMessageDialog(dialog, "Profile updated successfully!", "Success",
                    JOptionPane.INFORMATION_MESSAGE);
            dialog.dispose();

            refreshProfileCard();
            showPanel("profile");
        });

        actionPanel.add(cancelButton);
        actionPanel.add(changePassButton);
        actionPanel.add(saveButton);

        editCard.add(actionPanel);

        dialog.setContentPane(editCard);
        dialog.pack();
        dialog.setSize(460, 290);
        dialog.setLocationRelativeTo(this);
        dialog.setResizable(false);
        dialog.setVisible(true);
    }

    private static final class LogDetails {
        final String name;
        final String email;
        final String date;
        final String role;
        final String activity;

        LogDetails(String name, String email, String date, String role, String activity) {
            this.name = name;
            this.email = email;
            this.date = date;
            this.role = role;
            this.activity = activity;
        }

        static LogDetails parse(String rawLog, model.User currentUser) {
            String defaultDate = LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("MMM dd, yyyy hh:mm a", Locale.ENGLISH));

            if (rawLog != null && rawLog.contains("|")) {
                String[] parts = rawLog.split("\\|");
                if (parts.length >= 5) {
                    String parsedName = cleanName(parts[0].trim());
                    String parsedEmail = cleanEmail(parts[1].trim(), parsedName);
                    String parsedDate = formatAmPm(parts[2].trim());
                    String parsedRole = parts[3].trim().toUpperCase();
                    String parsedActivity = cleanActivity(parts[4].trim());

                    return new LogDetails(parsedName, parsedEmail, parsedDate, parsedRole, parsedActivity);
                }
            }

            String cleanedName = cleanName(rawLog != null ? rawLog : "User");
            String extractedDate = extractDate(rawLog, defaultDate);
            String activity = cleanActivity(rawLog);
            String role = extractRole(rawLog, "STAFF");
            String email = cleanEmail("", cleanedName);

            return new LogDetails(cleanedName, email, extractedDate, role, activity);
        }

        private static String cleanName(String raw) {
            if (raw == null || raw.trim().isEmpty())
                return "User";
            String cleaned = raw.replaceAll("^\\[.*?\\]\\s*", "").trim();
            if (cleaned.toLowerCase().contains("logged")) {
                cleaned = cleaned.replaceAll("(?i)\\s*logged.*", "").trim();
            }
            if (cleaned.contains("@")) {
                cleaned = cleaned.split("@")[0].trim();
            }
            return cleaned.isEmpty() ? "User" : cleaned;
        }

        private static String cleanEmail(String rawEmail, String userName) {
            if (rawEmail != null && !rawEmail.trim().isEmpty() && rawEmail.contains("@")) {
                String cleaned = rawEmail.replaceAll("^\\[.*?\\]\\s*", "").trim();
                if (!cleaned.isEmpty()) {
                    return cleaned;
                }
            }
            String safeName = (userName != null && !userName.trim().isEmpty()) ? userName.trim() : "staff";
            return safeName.toLowerCase().replaceAll("[^a-zA-Z0-9]", "") + "@queuetees.local";
        }

        private static String extractDate(String raw, String defaultDate) {
            if (raw == null)
                return defaultDate;
            if (raw.startsWith("[") && raw.contains("]")) {
                String inside = raw.substring(1, raw.indexOf("]")).trim();
                return formatAmPm(inside);
            }
            return defaultDate;
        }

        private static String extractRole(String raw, String defaultRole) {
            if (raw == null)
                return defaultRole;
            String upper = raw.toUpperCase();
            if (upper.contains("STAFF"))
                return "STAFF";
            if (upper.contains("ADMIN"))
                return "ADMIN";
            if (upper.contains("CUSTOMER"))
                return "CUSTOMER";
            return defaultRole;
        }

        private static String cleanActivity(String raw) {
            if (raw == null)
                return "Logged in";
            String lower = raw.toLowerCase();
            if (lower.contains("out") || lower.contains("logout")) {
                return "Logged out";
            }
            return "Logged in";
        }

        private static String formatAmPm(String dateStr) {
            if (dateStr == null || dateStr.trim().isEmpty()) {
                return LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy hh:mm a", Locale.ENGLISH));
            }
            dateStr = dateStr.replaceAll("^\\[.*?\\]\\s*", "").trim();
            dateStr = dateStr.replace(" - ", " ");

            if (dateStr.toUpperCase().contains("AM") || dateStr.toUpperCase().contains("PM")) {
                return dateStr;
            }
            try {
                DateTimeFormatter parser = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm", Locale.ENGLISH);
                LocalDateTime ldt = LocalDateTime.parse(dateStr, parser);
                return ldt.format(DateTimeFormatter.ofPattern("MMM dd, yyyy hh:mm a", Locale.ENGLISH));
            } catch (Exception e) {
                return dateStr;
            }
        }
    }

    private static final class Ui {
        static final Color INK = new Color(28, 31, 27);
        static final Color MUTED = new Color(99, 106, 96);
        static final Color FOREST = new Color(55, 70, 56);
        static final Color SAGE = new Color(145, 155, 145);
        static final Color CREAM = new Color(237, 241, 233);
        static final Color PAPER = new Color(252, 252, 247);
        static final Color LINE = new Color(218, 220, 209);

        static Font font(int size, int style) {
            return new Font("Segoe UI", style, size);
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
            addLeft(panel, label(eyebrow, 9, Font.BOLD, FOREST));
            panel.add(Box.createVerticalStrut(2));
            addLeft(panel, label(title, 20, Font.BOLD, INK));
            panel.add(Box.createVerticalStrut(3));
            addLeft(panel, label(description, 10, Font.PLAIN, MUTED));
            panel.add(Box.createVerticalStrut(10));
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

        static JPanel metricCard(JLabel valueLabel, String caption) {
            JPanel panel = card(PAPER, 14, true);
            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
            panel.setBorder(new javax.swing.border.EmptyBorder(10, 14, 10, 14));
            panel.add(valueLabel);
            panel.add(Box.createVerticalStrut(2));
            panel.add(label(caption, 10, Font.PLAIN, MUTED));
            return panel;
        }

        static JPanel emptyState(String title, String message) {
            JPanel panel = card(PAPER, 22, true);
            panel.setLayout(new GridBagLayout());
            JPanel center = verticalBox();
            JLabel icon = label("○", 24, Font.PLAIN, SAGE);
            icon.setAlignmentX(Component.CENTER_ALIGNMENT);
            center.add(icon);
            center.add(Box.createVerticalStrut(4));
            JLabel heading = label(title, 13, Font.BOLD, INK);
            heading.setAlignmentX(Component.CENTER_ALIGNMENT);
            center.add(heading);
            center.add(Box.createVerticalStrut(3));
            JLabel detail = label(message, 10, Font.PLAIN, MUTED);
            detail.setAlignmentX(Component.CENTER_ALIGNMENT);
            center.add(detail);
            panel.add(center);
            panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
            panel.setPreferredSize(new Dimension(900, 200));
            return panel;
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

            JTableHeader header = table.getTableHeader();
            header.setFont(font(10, Font.BOLD));
            header.setForeground(MUTED);
            header.setBackground(new Color(238, 239, 230));
            header.setPreferredSize(new Dimension(0, 38));
            header.setReorderingAllowed(false);

            DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
                @Override
                public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                        boolean hasFocus, int row, int column) {
                    JLabel c = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row,
                            column);
                    c.setBorder(new EmptyBorder(0, 12, 0, 12));
                    c.setFont(font(11, column == 4 ? Font.BOLD : Font.PLAIN));

                    if (column == 4 && value != null) {
                        String act = value.toString();
                        boolean isLogout = "Logged out".equalsIgnoreCase(act);
                        c.setForeground(isLogout ? new Color(180, 83, 9) : new Color(22, 101, 52));
                    } else {
                        c.setForeground(INK);
                    }

                    if (isSelected) {
                        c.setBackground(table.getSelectionBackground());
                    } else {
                        c.setBackground(PAPER);
                    }
                    return c;
                }
            };
            table.setDefaultRenderer(Object.class, renderer);
        }

        static JButton linkButton(String text) {
            JButton button = new JButton(text);
            button.setFont(font(11, Font.BOLD));
            button.setForeground(FOREST);
            button.setContentAreaFilled(false);
            button.setBorderPainted(false);
            button.setFocusPainted(false);
            button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            return button;
        }

        static RoundedButton primaryButton(String title) {
            RoundedButton button = new RoundedButton(title, INK, Color.WHITE);
            button.setFont(font(10, Font.BOLD));
            button.setHoverColor(new Color(74, 91, 74));
            button.setPreferredSize(new Dimension(120, 32));
            return button;
        }

        static RoundedButton lightButton(String title) {
            RoundedButton button = new RoundedButton(title, CREAM, INK);
            button.setFont(font(10, Font.BOLD));
            button.setHoverColor(new Color(218, 225, 211));
            return button;
        }

        static NavButton navButton(String title) {
            return new NavButton(title);
        }

        static void confirmLogout(Component parent, service.AuthService authService, model.User currentUser) {
            int choice = JOptionPane.showConfirmDialog(parent, "Log out of QueueTees?", "Confirm Log Out",
                    JOptionPane.YES_NO_OPTION);
            if (choice == JOptionPane.YES_OPTION) {
                String name = (currentUser != null && currentUser.getUsername() != null
                        && !currentUser.getUsername().trim().isEmpty())
                                ? currentUser.getUsername().trim()
                                : "User";
                String email = (currentUser != null && currentUser.getEmail() != null
                        && !currentUser.getEmail().trim().isEmpty())
                                ? currentUser.getEmail().trim()
                                : name.toLowerCase().replaceAll("\\s+", "") + "@queuetees.local";
                String role = (currentUser != null && currentUser.getRole() != null)
                        ? currentUser.getRole().toString().toUpperCase()
                        : "ADMIN";
                String date = LocalDateTime.now()
                        .format(DateTimeFormatter.ofPattern("MMM dd, yyyy hh:mm a", Locale.ENGLISH));

                service.ActivityLogger.log(name + " | " + email + " | " + date + " | " + role + " | Logged out");

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
                setFont(font(11, Font.PLAIN));
                setForeground(new Color(224, 230, 219));
                setHorizontalAlignment(LEFT);
                setBorder(new javax.swing.border.EmptyBorder(0, 10, 0, 10));
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
                setFont(font(11, value ? Font.BOLD : Font.PLAIN));
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
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                    g2.dispose();
                }
                super.paintComponent(g);
            }
        }
    }
}