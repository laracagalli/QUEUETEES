package gui.customer;

import gui.components.RoundedButton;
import java.awt.*;
import java.io.File;
import java.util.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import service.AuthService;
import model.User;

/** All customer shop navigation and pages. */
public final class CustomerDashboardPanel extends JPanel {
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel content = new JPanel(cardLayout);
    private final Map<String, CustomerNavButton> navigation = new LinkedHashMap<>();
    private final AuthService authService;
    private final User currentUser;

    private final ShopPanel shopPanel;
    private final CartPanel cartPanel;
    private final OrderTrackingPanel trackingPanel;
    private final OrderHistoryPanel historyPanel;

    // Dynamic container for profile
    private final JPanel profileContainer = new JPanel(new BorderLayout());

    public CustomerDashboardPanel(AuthService authService, User user) {
        this.authService = authService;
        this.currentUser = user;

        this.shopPanel = new ShopPanel(user, () -> showPanel("cart"));
        this.cartPanel = new CartPanel(user, () -> showPanel("tracking"));
        this.trackingPanel = new OrderTrackingPanel(user);
        this.historyPanel = new OrderHistoryPanel(user);

        setLayout(new BorderLayout());
        add(createHeader(), BorderLayout.NORTH);

        content.setOpaque(false);
        content.setBorder(new EmptyBorder(30, 36, 30, 36));
        add(content);

        content.add(shopPanel, "shop");
        content.add(cartPanel, "cart");
        content.add(trackingPanel, "tracking");
        content.add(historyPanel, "history");

        // Initialize dynamic Profile Panel
        profileContainer.setOpaque(false);
        profileContainer.add(createProfilePanel(currentUser), BorderLayout.CENTER);
        content.add(profileContainer, "profile");

        setOpaque(false);
        showPanel("shop");
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setPaint(new GradientPaint(0, 0, Ui.CREAM, 0, getHeight(), Ui.SAGE));
        g2.fillRect(0, 0, getWidth(), getHeight());
        g2.dispose();
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout(20, 0));
        header.setBackground(new Color(250, 250, 244));
        header.setBorder(new EmptyBorder(10, 36, 10, 36));
        header.setPreferredSize(new Dimension(0, 84));

        JPanel brand = Ui.verticalBox();
        brand.setPreferredSize(new Dimension(210, 64));
        JLabel logo = Ui.logo(110, 40);
        logo.setAlignmentX(Component.CENTER_ALIGNMENT);
        brand.add(logo);
        JLabel brandName = Ui.label("QUEUETEES: A Queuing Management System", 9, Font.BOLD, Ui.INK);
        brandName.setAlignmentX(Component.CENTER_ALIGNMENT);
        brandName.setHorizontalAlignment(SwingConstants.CENTER);
        brand.add(brandName);
        header.add(brand, BorderLayout.WEST);

        JPanel nav = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
        nav.setOpaque(false);
        addNavigation(nav, "Shop", "shop");
        addNavigation(nav, "My Cart", "cart");
        addNavigation(nav, "Track Order", "tracking");
        addNavigation(nav, "Order History", "history");
        addNavigation(nav, "Account", "profile");
        header.add(nav);

        RoundedButton logout = new RoundedButton("Log Out", Ui.INK, Color.WHITE);
        logout.setFont(Ui.font(10, Font.BOLD));
        logout.setPreferredSize(new Dimension(86, 34));
        logout.setMinimumSize(new Dimension(86, 34));
        logout.setMaximumSize(new Dimension(86, 34));
        logout.setHoverColor(new Color(65, 65, 65));
        logout.addActionListener(e -> Ui.confirmLogout(header, authService));

        JPanel logoutWrap = new JPanel(new GridBagLayout());
        logoutWrap.setOpaque(false);
        logoutWrap.setPreferredSize(new Dimension(210, 64));
        logoutWrap.add(logout);
        header.add(logoutWrap, BorderLayout.EAST);

        return header;
    }

    private void addNavigation(JPanel parent, String title, String key) {
        CustomerNavButton button = new CustomerNavButton(title);
        button.addActionListener(e -> showPanel(key));
        navigation.put(key, button);
        parent.add(button);
    }

    private void showPanel(String key) {
        cardLayout.show(content, key);
        if ("shop".equals(key))
            shopPanel.refresh();
        if ("cart".equals(key))
            cartPanel.refresh();
        if ("tracking".equals(key))
            trackingPanel.refresh();
        if ("history".equals(key))
            historyPanel.refresh();
        if ("profile".equals(key)) {
            refreshProfileView();
        }
        navigation.forEach((name, button) -> button.setSelectedState(name.equals(key)));
    }

    private void refreshProfileView() {
        profileContainer.removeAll();
        profileContainer.add(createProfilePanel(currentUser), BorderLayout.CENTER);
        profileContainer.revalidate();
        profileContainer.repaint();
        content.revalidate();
        content.repaint();
    }

    // --- PROFILE AND EDIT COMPONENTS ---

    private JPanel createProfilePanel(model.User user) {
        JPanel page = Ui.page("ACCOUNT", "My account", "Review and update customer account details.");

        // White Profile Card Background matching the mockup
        JPanel profileCard = Ui.card(Ui.PAPER, 14, false);
        profileCard.setLayout(new BoxLayout(profileCard, BoxLayout.Y_AXIS));
        profileCard.setBorder(new EmptyBorder(32, 32, 38, 32));
        profileCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 450));

        // 1. Top Section: Avatar and Name Row
        JPanel headerRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 18, 0));
        headerRow.setOpaque(false);
        headerRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        int size = 76;
        JLabel avatarLabel = new JLabel();
        avatarLabel.setPreferredSize(new Dimension(size, size));
        avatarLabel.setMinimumSize(new Dimension(size, size));
        avatarLabel.setMaximumSize(new Dimension(size, size));
        avatarLabel.setHorizontalAlignment(SwingConstants.CENTER);

        String picPath = (user != null) ? user.getProfilePicture() : null;
        boolean imageLoaded = false;

        if (picPath != null && !picPath.trim().isEmpty()) {
            File imgFile = new File(picPath);
            if (imgFile.exists() && imgFile.isFile()) {
                ImageIcon icon = new ImageIcon(imgFile.getAbsolutePath());
                if (icon.getIconWidth() > 0 && icon.getIconHeight() > 0) {
                    Image scaledImg = icon.getImage().getScaledInstance(size, size, Image.SCALE_SMOOTH);
                    avatarLabel.setIcon(new ImageIcon(scaledImg));
                    imageLoaded = true;
                }
            }
        }

        String username = (user != null && user.getUsername() != null && !user.getUsername().trim().isEmpty())
                ? user.getUsername()
                : "patrick";

        if (!imageLoaded) {
            String initial = "P";
            if (!username.isEmpty()) {
                initial = username.substring(0, 1).toUpperCase();
            }
            avatarLabel.setText(initial);
            avatarLabel.setFont(Ui.font(28, Font.BOLD));
            avatarLabel.setForeground(Ui.INK);
            avatarLabel.setOpaque(true);
            avatarLabel.setBackground(new Color(228, 232, 222));
        }

        headerRow.add(avatarLabel);

        JPanel infoPanel = Ui.verticalBox();
        infoPanel.add(Box.createVerticalStrut(8));
        infoPanel.add(Ui.label(username, 22, Font.BOLD, Ui.INK));
        infoPanel.add(Box.createVerticalStrut(6));
        infoPanel.add(Ui.label("Customer account", 12, Font.PLAIN, Ui.MUTED));

        headerRow.add(infoPanel);

        profileCard.add(headerRow);
        profileCard.add(Box.createVerticalStrut(46));

        // 2. Data Grid Section mapping to mockup values
        JPanel grid = new JPanel(new GridLayout(3, 2, 40, 28));
        grid.setOpaque(false);
        grid.setAlignmentX(Component.LEFT_ALIGNMENT);

        grid.add(createProfileField("Username", username));
        grid.add(createProfileField("Email address", "verified@queuetees.local"));

        grid.add(createProfileField("Role", "Customer"));
        grid.add(createProfileField("Account status", "Active"));

        grid.add(createProfileField("Email verification", "Verified"));
        grid.add(createProfileField("Account ID", "1"));

        profileCard.add(grid);

        // Enforce the card styling within the layout constraints
        JPanel cardWrap = new JPanel(new BorderLayout());
        cardWrap.setOpaque(false);
        cardWrap.setAlignmentX(Component.LEFT_ALIGNMENT);
        cardWrap.add(profileCard, BorderLayout.CENTER);

        page.add(cardWrap);
        page.add(Box.createVerticalStrut(24));

        // 3. Edit Button below the card
        RoundedButton editProfileBtn = Ui.primaryButton("Edit Profile");
        editProfileBtn.setPreferredSize(new Dimension(135, 36));
        editProfileBtn.setMinimumSize(new Dimension(135, 36));
        editProfileBtn.setMaximumSize(new Dimension(135, 36));
        editProfileBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        editProfileBtn.addActionListener(e -> showEditProfileDialog(user));

        page.add(editProfileBtn);
        return page;
    }

    private JPanel createProfileField(String label, String value) {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JLabel lbl = Ui.label(label, 11, Font.PLAIN, Ui.MUTED);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(lbl);

        panel.add(Box.createVerticalStrut(8));

        JLabel val = Ui.label(value, 13, Font.BOLD, Ui.INK);
        val.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(val);

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

        String currentPhotoName = (user != null && user.getProfilePicture() != null
                && !user.getProfilePicture().trim().isEmpty())
                        ? new File(user.getProfilePicture()).getName()
                        : "No picture selected";
        JLabel picPathLabel = Ui.label(currentPhotoName, 10, Font.PLAIN, Ui.MUTED);
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
                // FIX: Use the OS native absolute path without manual string replacements
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

                if (authService != null) {
                    try {
                        authService.updateUser(user);
                    } catch (Exception ex) {
                        System.err.println("Failed to save user updates: " + ex.getMessage());
                    }
                }
            }

            JOptionPane.showMessageDialog(dialog, "Profile updated successfully!", "Success",
                    JOptionPane.INFORMATION_MESSAGE);
            dialog.dispose();

            // Refresh on Event Dispatch Thread to ensure proper UI update
            SwingUtilities.invokeLater(this::refreshProfileView);
        });

        actionPanel.add(cancelButton);
        actionPanel.add(saveButton);

        editCard.add(actionPanel);

        dialog.setContentPane(editCard);
        dialog.pack();
        dialog.setSize(420, 290);
        dialog.setLocationRelativeTo(this);
        dialog.setResizable(false);
        dialog.setVisible(true);
    }

    private static final class CustomerNavButton extends JButton {
        private boolean selected, hovered;
        private float highlight;
        private float targetHighlight;
        private final javax.swing.Timer transitionTimer;

        CustomerNavButton(String title) {
            super(title);
            putClientProperty("queuetees.preserveButtonStyle", true);
            transitionTimer = new javax.swing.Timer(16, e -> animateHighlight());
            setFont(Ui.font(14, Font.PLAIN));
            setForeground(Ui.MUTED);
            setBorder(new EmptyBorder(11, 14, 11, 14));
            setOpaque(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseEntered(java.awt.event.MouseEvent e) {
                    hovered = true;
                    setHighlightTarget(selected ? 1f : 0.42f);
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
            setFont(Ui.font(14, value ? Font.BOLD : Font.PLAIN));
            setForeground(value ? Ui.INK : Ui.MUTED);
            setHighlightTarget(value ? 1f : (hovered ? 0.42f : 0f));
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
                g2.setColor(new Color(218, 224, 211, Math.round(255 * highlight)));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 18, 18);
                g2.dispose();
            }
            super.paintComponent(g);
        }
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
            java.net.URL url = CustomerDashboardPanel.class.getResource("/Gui_Images/hirayalogo2.png");
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
            panel.add(Box.createVerticalStrut(20));
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
    }
}