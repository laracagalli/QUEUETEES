package gui.admin;

import java.awt.*;
import java.io.File;
import java.net.URL;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import model.Product;
import service.StoreService;

/** Administrator page for product catalog and inventory maintenance. */
public final class ProductManagementPanel extends JPanel {
    private final StoreService store = StoreService.getInstance();
    private final DefaultTableModel model = new DefaultTableModel(
            new String[] { "Product", "Category", "Subcategory", "Price", "Stock", "Picture", "Status" }, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(model);
    private final JLabel emptyLabel = Ui.label("", 11, Font.PLAIN, Ui.MUTED);
    private final JTextField search = new JTextField();

    private final model.User user;

    public ProductManagementPanel() {
        this(null);
    }

    public ProductManagementPanel(model.User user) {
        this.user = user;
        setOpaque(false);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        Ui.addLeft(this, Ui.label("CATALOG", 10, Font.BOLD, Ui.FOREST));
        add(Box.createVerticalStrut(4));
        Ui.addLeft(this, Ui.label("Products & inventory", 25, Font.BOLD, Ui.INK));
        add(Box.createVerticalStrut(5));
        Ui.addLeft(this, Ui.label("Maintain the catalog and monitor availability.", 11, Font.PLAIN, Ui.MUTED));
        add(Box.createVerticalStrut(18));
        add(createToolbar());
        add(Box.createVerticalStrut(16));
        add(createTableCard());
        refresh();
    }

    private JPanel createToolbar() {
        JPanel toolbar = new JPanel(new BorderLayout(12, 0));
        toolbar.setOpaque(false);
        toolbar.setAlignmentX(Component.LEFT_ALIGNMENT);
        toolbar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        search.setFont(Ui.font(11, Font.PLAIN));
        search.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Ui.LINE),
                new javax.swing.border.EmptyBorder(0, 13, 0, 13)));
        search.setToolTipText("Search products");
        search.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                refresh();
            }

            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                refresh();
            }

            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                refresh();
            }
        });
        toolbar.add(search);
        gui.components.RoundedButton add = Ui.primaryButton("Add Product");
        add.setPreferredSize(new Dimension(140, 40));
        add.addActionListener(e -> showProductDialog());
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setOpaque(false);
        JButton print = AdminUi.button("Preview / print report");
        print.addActionListener(e -> AdminTableReport.open(this, table, "Products and stock report", user));
        actions.add(print);
        actions.add(add);
        toolbar.add(actions, BorderLayout.EAST);
        toolbar.add(AdminUi.label("Search products", 11, false), BorderLayout.WEST);
        return toolbar;
    }

    private JPanel createTableCard() {
        JPanel card = Ui.card(Ui.PAPER, 22, true);
        card.setLayout(new BorderLayout());
        card.setBorder(new javax.swing.border.EmptyBorder(12, 12, 12, 12));
        AdminUi.style(table);
        table.setRowHeight(48);
        table.getColumnModel().getColumn(0).setPreferredWidth(260);

        // Custom Cell Renderer to render image icon on the left of the product name
        table.getColumnModel().getColumn(0).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row,
                        column);
                label.setFont(Ui.font(11, Font.BOLD));
                label.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
                label.setIconTextGap(12);

                int modelRow = table.convertRowIndexToModel(row);
                Object rawPicture = table.getModel().getValueAt(modelRow, 5);
                String path = rawPicture != null ? rawPicture.toString() : "";

                label.setIcon(loadProductIcon(path));
                return label;
            }
        });

        table.removeColumn(table.getColumnModel().getColumn(5));
        JScrollPane scroll = new JScrollPane(table);
        scroll.setColumnHeaderView(table.getTableHeader());
        scroll.setBorder(null);
        scroll.getViewport().setBackground(Ui.PAPER);
        card.add(scroll);
        emptyLabel.setHorizontalAlignment(SwingConstants.CENTER);
        emptyLabel.setBorder(new javax.swing.border.EmptyBorder(12, 8, 12, 8));
        card.add(emptyLabel, BorderLayout.NORTH);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        card.setPreferredSize(new Dimension(1000, 520));
        return card;
    }

    /**
     * Helper method to locate and scale product image from classpath resources or
     * disk.
     */
    private ImageIcon loadProductIcon(String path) {
        if (path == null || path.trim().isEmpty() || path.equalsIgnoreCase("None")) {
            return null;
        }
        Image img = null;
        try {
            String cleanName = new File(path).getName();

            // Try matching resource in /Gui_Images/ directory
            URL url = getClass().getResource("/Gui_Images/" + cleanName);
            if (url == null) {
                // Try extension variations (.png / .jpg)
                String baseName = cleanName.contains(".") ? cleanName.substring(0, cleanName.lastIndexOf('.'))
                        : cleanName;
                url = getClass().getResource("/Gui_Images/" + baseName + ".png");
                if (url == null) {
                    url = getClass().getResource("/Gui_Images/" + baseName + ".jpg");
                }
            }
            if (url == null && path.startsWith("/")) {
                url = getClass().getResource(path);
            }

            if (url != null) {
                img = new ImageIcon(url).getImage();
            } else {
                File file = new File(path);
                if (file.exists()) {
                    img = new ImageIcon(file.getAbsolutePath()).getImage();
                }
            }
        } catch (Exception ignored) {
        }

        if (img != null) {
            return new ImageIcon(img.getScaledInstance(38, 38, Image.SCALE_SMOOTH));
        }
        return null;
    }

    private void showProductDialog() {
        JTextField name = new JTextField();
        JComboBox<String> category = new JComboBox<>(new String[] { "Women", "Men", "Kids", "Accessories" });
        JTextField subcategory = new JTextField("Tops");
        JTextField price = new JTextField();
        JTextField stock = new JTextField();
        JTextField imagePath = new JTextField();
        JButton browse = new JButton("Browse picture...");
        browse.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileFilter(
                    new javax.swing.filechooser.FileNameExtensionFilter("Images", "png", "jpg", "jpeg", "gif"));
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                imagePath.setText(chooser.getSelectedFile().getAbsolutePath());
            }
        });
        JPanel picture = new JPanel(new BorderLayout(6, 0));
        picture.add(imagePath);
        picture.add(browse, BorderLayout.EAST);
        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        form.add(new JLabel("Product name"));
        form.add(name);
        form.add(new JLabel("Category"));
        form.add(category);
        form.add(new JLabel("Subcategory"));
        form.add(subcategory);
        form.add(new JLabel("Price"));
        form.add(price);
        form.add(new JLabel("Stock"));
        form.add(stock);
        form.add(new JLabel("Product picture"));
        form.add(picture);
        int result = JOptionPane.showConfirmDialog(this, form, "Add catalog product", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION)
            return;
        try {
            store.addProduct(name.getText(), String.valueOf(category.getSelectedItem()), subcategory.getText(),
                    Double.parseDouble(price.getText().trim()), Integer.parseInt(stock.getText().trim()),
                    imagePath.getText());
            refresh();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Enter a valid numeric price and stock.", "Invalid product",
                    JOptionPane.WARNING_MESSAGE);
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Invalid product", JOptionPane.WARNING_MESSAGE);
        }
    }

    public void refresh() {
        String query = search.getText().trim().toLowerCase();
        model.setRowCount(0);
        List<Product> products = store.getProducts();
        for (Product product : products) {
            String haystack = (product.getName() + " " + product.getCategory() + " " + product.getSubcategory())
                    .toLowerCase();
            if (!haystack.contains(query))
                continue;
            String picture = product.getImagePath().isEmpty() ? "None" : product.getImagePath();
            model.addRow(new Object[] { product.getName(), product.getCategory(), product.getSubcategory(),
                    String.format("₱%,.2f", product.getPrice()), product.getStock(), picture,
                    product.isAvailable() ? "Available" : "Out of stock" });
        }
        emptyLabel.setText(model.getRowCount() == 0 ? "No matching products are available."
                : model.getRowCount() + " product(s) in the catalog");
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
            search.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(LINE),
                    new javax.swing.border.EmptyBorder(0, 13, 0, 13)));
            toolbar.add(search);
            if (action != null) {
                gui.components.RoundedButton button = primaryButton(action);
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
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };
            JTable table = new JTable(model);
            styleTable(table);
            JScrollPane scroll = new JScrollPane(table);
            scroll.setColumnHeaderView(table.getTableHeader());
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
                if (url != null)
                    image.setIcon(new ImageIcon(
                            new ImageIcon(url).getImage().getScaledInstance(260, 245, Image.SCALE_SMOOTH)));
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
                    @Override
                    public void mouseEntered(java.awt.event.MouseEvent e) {
                        hovered = true;
                        repaint();
                    }

                    @Override
                    public void mouseExited(java.awt.event.MouseEvent e) {
                        hovered = false;
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

            @Override
            protected void paintComponent(Graphics g) {
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