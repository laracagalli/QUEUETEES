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
            new String[] { "Product", "Category", "Subcategory", "Price", "Stock", "Picture", "Status", "Size" }, 0) {
        @Override public Class<?> getColumnClass(int column) {
            return column == 4 ? Integer.class : String.class;
        }
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(model);
    private final JLabel emptyLabel = Ui.label("", 11, Font.PLAIN, Ui.MUTED);
    private final JTextField search = new JTextField();
    private final JComboBox<String> categoryFilter = new gui.components.RoundedComboBox<>(new String[] {"All categories"});
    private final JComboBox<String> subcategoryFilter = new gui.components.RoundedComboBox<>(new String[] {"All subcategories"});
    private final JComboBox<String> statusFilter = new gui.components.RoundedComboBox<>(new String[] {"All statuses", "Available", "Out of stock"});
    private final JComboBox<String> sizeFilter = new gui.components.RoundedComboBox<>(new String[]{"All sizes","XS","S","M","L","XL","XXL","One Size"});
    private boolean updatingFilters;
    private final List<Product> rowProducts = new java.util.ArrayList<>();
    private final JLabel selectedStockLabel = Ui.label("Select a product to replenish its stock", 12, Font.BOLD, Ui.INK);
    private final JLabel selectedStockDetails = Ui.label("Choose a row above, then enter the quantity to add.", 11, Font.PLAIN, Ui.MUTED);
    private final JSpinner replenishQuantity = new JSpinner(new SpinnerNumberModel(1, 1, Integer.MAX_VALUE, 1));
    private final JButton replenishButton = Ui.primaryButton("Replenish stock");

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
        add(Box.createVerticalStrut(10));
        add(createFilters());
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
        search.putClientProperty("searchField", true);
        search.getAccessibleContext().setAccessibleName("Search products");
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
        table.getRowSorter().setSortKeys(List.of(new RowSorter.SortKey(4, SortOrder.ASCENDING)));
        table.setRowHeight(48);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) updateStockSelection();
        });
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
        JScrollPane scroll = new gui.components.ModernScrollPane(table);
        scroll.setColumnHeaderView(table.getTableHeader());
        scroll.setBorder(null);
        scroll.getViewport().setBackground(Ui.PAPER);
        card.add(scroll);
        emptyLabel.setHorizontalAlignment(SwingConstants.LEFT);
        emptyLabel.setBorder(new javax.swing.border.EmptyBorder(4, 2, 12, 2));
        card.add(emptyLabel, BorderLayout.NORTH);
        card.add(createStockBar(), BorderLayout.SOUTH);

        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        card.setPreferredSize(new Dimension(1000, 520));
        return card;
    }

    private JPanel createStockBar() {
        JPanel bar = new JPanel(new BorderLayout(18, 0));
        bar.setOpaque(false);
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Ui.LINE),
                BorderFactory.createEmptyBorder(14, 4, 2, 4)));
        JPanel description = new JPanel(new GridLayout(2, 1, 0, 4));
        description.setOpaque(false);
        description.add(selectedStockLabel);
        description.add(selectedStockDetails);
        bar.add(description, BorderLayout.CENTER);
        JPanel controls = new JPanel(new BorderLayout(10, 0));
        controls.setOpaque(false);
        JPanel quantity = new JPanel(new BorderLayout(8, 0));
        quantity.setOpaque(false);
        quantity.add(Ui.label("Quantity to add", 11, Font.PLAIN, Ui.MUTED), BorderLayout.WEST);
        replenishQuantity.setPreferredSize(new Dimension(100, 38));
        replenishQuantity.getAccessibleContext().setAccessibleName("Stock adjustment quantity");
        quantity.add(replenishQuantity, BorderLayout.CENTER);
        replenishButton.setPreferredSize(new Dimension(156, 38));
        controls.add(quantity, BorderLayout.CENTER);
        controls.add(replenishButton, BorderLayout.EAST);
        bar.add(controls, BorderLayout.EAST);
        replenishButton.addActionListener(e -> {
            Product selected = selectedProduct();
            if (selected == null) return;
            try {
                replenishQuantity.commitEdit();
                adjustStock(selected, ((Number)replenishQuantity.getValue()).intValue());
            } catch (java.text.ParseException ex) {
                JOptionPane.showMessageDialog(this, "Enter a positive whole-number quantity.", "Invalid quantity", JOptionPane.WARNING_MESSAGE);
                replenishQuantity.requestFocusInWindow();
            }
        });
        updateStockSelection();
        return bar;
    }

    private void updateStockSelection() {
        Product selected = selectedProduct();
        boolean available = selected != null && selected.getStock() < Integer.MAX_VALUE;
        replenishQuantity.setEnabled(available);
        replenishButton.setEnabled(available);
        replenishQuantity.setValue(1);
        selectedStockLabel.setText(selected == null ? "Select a product to replenish its stock" : selected.getDisplayName());
        selectedStockLabel.setToolTipText(selected == null ? null : selected.getDisplayName());
        selectedStockDetails.setText(selected == null ? "Choose a row above, then enter the quantity to add."
                : "Current stock: " + selected.getStock() + "  •  Additions require confirmation.");
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
        JComboBox<String> category = new gui.components.RoundedComboBox<>(new String[] { "Women", "Men", "Kids", "Accessories" });
        JTextField subcategory = new JTextField("Tops");
        JTextField price = new JTextField();
        JTextField stock = new JTextField();
        JComboBox<String> size = new gui.components.RoundedComboBox<>(Product.SIZES);
        category.addActionListener(e->size.setModel(new DefaultComboBoxModel<>("Accessories".equals(category.getSelectedItem())?new String[]{"One Size"}:Product.SIZES)));
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
        form.add(new JLabel("Size (stock is specific to this size)"));form.add(size);
        form.add(new JLabel("Product picture"));
        form.add(picture);
        int result = JOptionPane.showConfirmDialog(this, form, "Add catalog product", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION)
            return;
        try {
            store.addProduct(name.getText(), String.valueOf(category.getSelectedItem()), subcategory.getText(),
                    Double.parseDouble(price.getText().trim()), Integer.parseInt(stock.getText().trim()),
                    imagePath.getText(),String.valueOf(size.getSelectedItem()));
            refresh();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Enter a valid numeric price and stock.", "Invalid product",
                    JOptionPane.WARNING_MESSAGE);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Invalid product", JOptionPane.WARNING_MESSAGE);
        }
    }

    public void refresh() {
        if (updatingFilters) return;
        Product selected = selectedProduct();
        List<Product> products = store.getProducts();
        updatingFilters = true;
        try {
            updateOptions(categoryFilter, "All categories", products.stream().map(Product::getCategory).toList());
            updateOptions(subcategoryFilter, "All subcategories", products.stream().map(Product::getSubcategory).toList());
        } finally { updatingFilters = false; }
        String query = search.getText().trim().toLowerCase(java.util.Locale.ROOT);
        model.setRowCount(0);
        rowProducts.clear();
        for (Product product : products) {
            String haystack = (product.getName() + " " + product.getCategory() + " " + product.getSubcategory())
                    .toLowerCase();
            if (!haystack.contains(query))
                continue;
            if (!matches(categoryFilter, product.getCategory()) || !matches(subcategoryFilter, product.getSubcategory())
                    || !matches(statusFilter, product.isAvailable() ? "Available" : "Out of stock") || !matches(sizeFilter,product.getSize())) continue;
            String picture = product.getImagePath().isEmpty() ? "None" : product.getImagePath();
            rowProducts.add(product);
            model.addRow(new Object[] { product.getName(), product.getCategory(), product.getSubcategory(),
                    String.format("₱%,.2f", product.getPrice()), product.getStock(), picture,
                    product.isAvailable() ? "Available" : "Out of stock", product.getSize() });
        }
        emptyLabel.setText(model.getRowCount() == 0 ? "No matching products are available."
                : model.getRowCount() + " product(s) in the catalog");
        if (selected != null) {
            for (int row = 0; row < rowProducts.size(); row++) {
                if (rowProducts.get(row).getId() == selected.getId()) {
                    int view = table.convertRowIndexToView(row);
                    if (view >= 0) table.setRowSelectionInterval(view, view);
                    break;
                }
            }
        }
        updateStockSelection();
    }

    private Product selectedProduct() {
        int view = table.getSelectedRow();
        if (view < 0) return null;
        int row = table.convertRowIndexToModel(view);
        return row >= 0 && row < rowProducts.size() ? rowProducts.get(row) : null;
    }

    private void adjustStock(Product product, int delta) {
        long next = (long) product.getStock() + delta;
        if (next < 0 || next > Integer.MAX_VALUE) {
            JOptionPane.showMessageDialog(this, "Stock cannot go below zero or exceed the stock limit.", "Invalid stock adjustment", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String message = product.getDisplayName() + "\nCurrent stock: " + product.getStock()
                + "\n" + (delta > 0 ? "Add: " : "Remove: ") + Math.abs((long) delta)
                + "\nNew stock: " + next;
        if (JOptionPane.showConfirmDialog(this, message, "Confirm stock adjustment", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE) != JOptionPane.OK_OPTION) return;
        try {
            store.adjustStock(product.getId(), delta, user);
            refresh();
        } catch (IllegalArgumentException | IllegalStateException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Unable to adjust stock", JOptionPane.WARNING_MESSAGE);
        }
    }
    private JPanel createFilters() {
        JPanel row = new JPanel(new GridLayout(1, 4, 12, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 62));
        JComboBox<?>[] filters = {categoryFilter, subcategoryFilter, statusFilter,sizeFilter};
        String[] labels = {"Category", "Subcategory", "Status","Size"};
        for (int i = 0; i < filters.length; i++) {
            JComboBox<?> filter = filters[i];
            filter.getAccessibleContext().setAccessibleName("Filter by " + labels[i]);
            filter.addActionListener(e -> refresh());
            JPanel field = new JPanel(new BorderLayout(0, 4));
            field.setOpaque(false);
            field.add(AdminUi.label(labels[i], 11, false), BorderLayout.NORTH);
            field.add(filter);
            row.add(field);
        }
        return row;
    }

    private static boolean matches(JComboBox<String> filter, String value) {
        return filter.getSelectedIndex() == 0 || value.equals(filter.getSelectedItem());
    }

    private static void updateOptions(JComboBox<String> filter, String all, List<String> values) {
        java.util.List<String> options = new java.util.ArrayList<>();
        options.add(all);
        values.stream().filter(java.util.Objects::nonNull).distinct().sorted().forEach(options::add);
        if (filter.getItemCount() == options.size()) {
            boolean same = true;
            for (int i = 0; i < options.size(); i++) same &= options.get(i).equals(filter.getItemAt(i));
            if (same) return;
        }
        Object selected = filter.getSelectedItem();
        filter.setModel(new DefaultComboBoxModel<>(options.toArray(String[]::new)));
        if (options.contains(selected)) filter.setSelectedItem(selected);
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
                    return column == 7;
                }
            };
            JTable table = new JTable(model);
            styleTable(table);
            JScrollPane scroll = new gui.components.ModernScrollPane(table);
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
