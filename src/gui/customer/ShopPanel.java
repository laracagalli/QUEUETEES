package gui.customer;

import gui.components.RoundedButton;
import java.awt.*;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import model.Product;
import model.User;
import service.StoreService;

/** Customer storefront and featured catalog. */
public final class ShopPanel extends JPanel {
    private final ButtonGroup categoryGroup = new ButtonGroup();
    private final ButtonGroup typeGroup = new ButtonGroup();
    private final StoreService store = StoreService.getInstance();
    private final User user;
    private final JPanel products = new JPanel(new GridLayout(0, 3, 16, 16));
    private final JTextField searchField = new JTextField();
    private final SlidingShowcasePanel showcase = new SlidingShowcasePanel();
    private final ModelShowcasePanel modelShowcase = new ModelShowcasePanel();
    private String selectedCategory = "Women";
    private String selectedType = "Tops";

    public ShopPanel(User user, Runnable openCart) {
        this.user = user;
        setOpaque(false);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        JPanel hero = Ui.card(Ui.FOREST, 26, false);
        hero.setLayout(new BorderLayout(14, 0));
        hero.setBorder(new EmptyBorder(16, 26, 16, 18));
        JPanel copy = Ui.verticalBox();
        copy.add(Ui.label("<html>Everyday pieces,<br>ordered with ease.</html>", 21, Font.BOLD, Color.WHITE));
        copy.add(Box.createVerticalStrut(5));
        copy.add(Ui.label("Confirmed orders keep their place in the QueueTees line.", 11, Font.PLAIN, new Color(221, 228, 216)));
        copy.add(Box.createVerticalGlue());
        RoundedButton cart = Ui.lightButton("View My Cart");
        cart.setPreferredSize(new Dimension(140, 40));
        cart.setMaximumSize(new Dimension(140, 40));
        cart.setAlignmentX(Component.LEFT_ALIGNMENT);
        cart.addActionListener(e -> openCart.run());
        copy.add(cart);
        copy.setPreferredSize(new Dimension(330, 228));
        hero.add(copy, BorderLayout.WEST);
        hero.add(showcase, BorderLayout.CENTER);
        hero.add(modelShowcase, BorderLayout.EAST);
        hero.setMaximumSize(new Dimension(Integer.MAX_VALUE, 260));
        hero.setPreferredSize(new Dimension(1000, 260));
        add(hero);
        add(Box.createVerticalStrut(14));
        add(createCatalogFilters());
        add(Box.createVerticalStrut(14));
        add(createCatalogSearch());
        add(Box.createVerticalStrut(10));
        products.setOpaque(false);
        JScrollPane catalog = new gui.components.ModernScrollPane(products);
        catalog.setBorder(null);
        catalog.setOpaque(false);
        catalog.getViewport().setOpaque(false);
        catalog.setAlignmentX(Component.LEFT_ALIGNMENT);
        catalog.setMaximumSize(new Dimension(Integer.MAX_VALUE, 310));
        catalog.setPreferredSize(new Dimension(1000, 310));
        add(catalog);
        refresh();
    }

    private JPanel createCatalogFilters() {
        JPanel filters = Ui.card(Ui.PAPER, 18, true);
        filters.setLayout(new BorderLayout(15, 0));
        filters.setBorder(new EmptyBorder(8, 12, 8, 12));
        filters.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        filters.setPreferredSize(new Dimension(1000, 48));
        JPanel categories = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        categories.setOpaque(false);
        categories.add(Ui.label("Shop:", 10, Font.BOLD, Ui.MUTED));
        addFilter(categories, categoryGroup, "Women", true, true);
        addFilter(categories, categoryGroup, "Men", false, true);
        addFilter(categories, categoryGroup, "Kids", false, true);
        addFilter(categories, categoryGroup, "Accessories", false, true);
        JPanel types = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        types.setOpaque(false);
        addFilter(types, typeGroup, "Tops", true, false);
        addFilter(types, typeGroup, "Bottoms", false, false);
        addFilter(types, typeGroup, "All", false, false);
        filters.add(categories, BorderLayout.WEST);
        filters.add(types, BorderLayout.EAST);
        return filters;
    }

    private JPanel createCatalogSearch() {
        JPanel search = new JPanel(new BorderLayout(10, 0));
        search.setOpaque(false);
        search.setAlignmentX(Component.LEFT_ALIGNMENT);
        search.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        search.setPreferredSize(new Dimension(1000, 36));
        search.add(Ui.label("Search products", 12, Font.BOLD, Ui.INK), BorderLayout.WEST);
        searchField.setFont(Ui.font(10, Font.PLAIN));
        searchField.setForeground(Ui.INK);
        searchField.setBackground(Color.WHITE);
        searchField.setPreferredSize(new Dimension(500, 34));
        searchField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Ui.LINE),
                new EmptyBorder(0, 10, 0, 10)));
        searchField.setToolTipText("Search by product name, category, or type");
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { refresh(); }
            @Override public void removeUpdate(DocumentEvent e) { refresh(); }
            @Override public void changedUpdate(DocumentEvent e) { refresh(); }
        });
        search.add(searchField, BorderLayout.CENTER);
        return search;
    }

    private void addFilter(JPanel parent, ButtonGroup group, String title, boolean selected, boolean categoryFilter) {
        AnimatedFilterButton button = new AnimatedFilterButton(title, selected);
        button.addActionListener(e -> {
            if (categoryFilter) selectedCategory = title;
            else selectedType = title;
            for (Component component : parent.getComponents()) {
                if (component instanceof AnimatedFilterButton) {
                    ((AnimatedFilterButton) component).animateSelection();
                }
            }
            refresh();
        });
        group.add(button);
        parent.add(button);
    }

    /** Filter button whose selected background eases between neighboring choices. */
    private static final class AnimatedFilterButton extends JToggleButton {
        private final Timer transitionTimer;
        private boolean hovered;
        private float highlight;
        private float targetHighlight;

        AnimatedFilterButton(String title, boolean selected) {
            super(title);
            setSelected(selected);
            highlight = selected ? 1f : 0f;
            targetHighlight = highlight;
            transitionTimer = new Timer(16, e -> animateHighlight());
            setFont(Ui.font(10, selected ? Font.BOLD : Font.PLAIN));
            setForeground(Ui.INK);
            setFocusPainted(false);
            setOpaque(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setBorder(new EmptyBorder(6, 11, 6, 11));
            addMouseListener(new java.awt.event.MouseAdapter() {
                @Override public void mouseEntered(java.awt.event.MouseEvent e) {
                    hovered = true;
                    setHighlightTarget(isSelected() ? 1f : 0.36f);
                }
                @Override public void mouseExited(java.awt.event.MouseEvent e) {
                    hovered = false;
                    setHighlightTarget(isSelected() ? 1f : 0f);
                }
            });
        }

        void animateSelection() {
            setFont(Ui.font(10, isSelected() ? Font.BOLD : Font.PLAIN));
            setHighlightTarget(isSelected() ? 1f : (hovered ? 0.36f : 0f));
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

        @Override protected void paintComponent(Graphics graphics) {
            if (highlight > 0f) {
                Graphics2D g2 = (Graphics2D) graphics.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(202, 213, 199, Math.round(255 * highlight)));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.dispose();
            }
            super.paintComponent(graphics);
        }
    }

    public void refresh() {
        products.removeAll();
        List<Product> catalog = store.getProducts();
        showcase.setProducts(catalog);
        String query = searchField.getText().trim().toLowerCase();
        for (Product product : catalog) {
            if (!product.getCategory().equalsIgnoreCase(selectedCategory)) continue;
            if (!"Accessories".equalsIgnoreCase(selectedCategory)
                    && !"All".equalsIgnoreCase(selectedType)
                    && !product.getSubcategory().equalsIgnoreCase(selectedType)) continue;
            if (!query.isEmpty()
                    && !product.getName().toLowerCase().contains(query)
                    && !product.getCategory().toLowerCase().contains(query)
                    && !product.getSubcategory().toLowerCase().contains(query)) continue;
            products.add(createProductCard(product));
        }
        if (products.getComponentCount() == 0) {
            JLabel empty = Ui.label("No products in this category yet. Add one from the admin catalog.", 11, Font.PLAIN, Ui.MUTED);
            empty.setHorizontalAlignment(SwingConstants.CENTER);
            products.add(empty);
        }
        products.revalidate();
        products.repaint();
    }

    private JPanel createProductCard(Product product) {
        JPanel card = Ui.card(Ui.PAPER, 22, true);
        card.setLayout(new BorderLayout());
        JLabel image = new JLabel("No picture", SwingConstants.CENTER);
        image.setPreferredSize(new Dimension(260, 205));
        image.setOpaque(true);
        image.setBackground(new Color(229, 230, 220));
        ImageIcon icon = loadImage(product.getImagePath(), 260, 205);
        if (icon != null) { image.setText(""); image.setIcon(icon); }
        card.add(image);
        JPanel details = Ui.verticalBox();
        details.setBorder(new EmptyBorder(11, 14, 12, 14));
        details.add(Ui.label(product.getName(), 13, Font.BOLD, Ui.INK));
        details.add(Box.createVerticalStrut(3));
        details.add(Ui.label(String.format("₱%,.2f  •  %d in stock", product.getPrice(), product.getStock()), 10, Font.PLAIN, Ui.MUTED));
        details.add(Box.createVerticalStrut(10));
        RoundedButton add = Ui.primaryButton(product.isAvailable() ? "Add to Cart" : "Out of Stock");
        add.setPreferredSize(new Dimension(116, 36));
        add.setEnabled(product.isAvailable());
        QuantitySelector amount = new QuantitySelector(Math.max(1, product.getStock()), product.isAvailable());
        add.addActionListener(e -> {
            try {
                int quantity = amount.getAmount();
                store.addToCart(user.getId(), product.getId(), quantity);
                JOptionPane.showMessageDialog(this, quantity + " × " + product.getName() + " added to your cart.");
            } catch (RuntimeException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Cart", JOptionPane.WARNING_MESSAGE);
            }
        });
        JPanel purchase = new JPanel(new BorderLayout(10, 0));
        purchase.setOpaque(false);
        purchase.setAlignmentX(Component.LEFT_ALIGNMENT);
        purchase.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        JPanel quantityGroup = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        quantityGroup.setOpaque(false);
        quantityGroup.add(Ui.label("Qty", 10, Font.BOLD, Ui.MUTED));
        quantityGroup.add(amount);
        purchase.add(quantityGroup, BorderLayout.WEST);
        purchase.add(add, BorderLayout.EAST);
        details.add(purchase);
        card.add(details, BorderLayout.SOUTH);
        card.setPreferredSize(new Dimension(280, 330));
        return card;
    }

    /** Compact quantity stepper styled to match the storefront cards. */
    private static final class QuantitySelector extends JPanel {
        private final int maximum;
        private final JLabel valueLabel;
        private final JButton minus;
        private final JButton plus;
        private int amount = 1;

        QuantitySelector(int maximum, boolean enabled) {
            this.maximum = maximum;
            setOpaque(false);
            setLayout(new BorderLayout());
            setPreferredSize(new Dimension(88, 36));
            setMinimumSize(new Dimension(88, 36));
            setMaximumSize(new Dimension(88, 36));
            minus = stepButton("−");
            plus = stepButton("+");
            valueLabel = Ui.label("1", 10, Font.BOLD, Ui.INK);
            valueLabel.setHorizontalAlignment(SwingConstants.CENTER);
            minus.addActionListener(e -> setAmount(amount - 1));
            plus.addActionListener(e -> setAmount(amount + 1));
            add(minus, BorderLayout.WEST);
            add(valueLabel, BorderLayout.CENTER);
            add(plus, BorderLayout.EAST);
            setEnabledState(enabled);
            updateButtons();
        }

        int getAmount() { return amount; }

        private void setAmount(int value) {
            amount = Math.max(1, Math.min(maximum, value));
            valueLabel.setText(String.valueOf(amount));
            updateButtons();
        }

        private void setEnabledState(boolean enabled) {
            valueLabel.setEnabled(enabled);
            minus.setEnabled(enabled);
            plus.setEnabled(enabled);
        }

        private void updateButtons() {
            boolean selectorEnabled = valueLabel.isEnabled();
            minus.setEnabled(selectorEnabled && amount > 1);
            plus.setEnabled(selectorEnabled && amount < maximum);
        }

        private JButton stepButton(String title) {
            JButton button = new JButton(title);
            button.putClientProperty("queuetees.preserveButtonStyle",true);
            button.setFont(Ui.font(13, Font.BOLD));
            button.setForeground(Ui.INK);
            button.setPreferredSize(new Dimension(28, 36));
            button.setBorder(new EmptyBorder(0, 0, 0, 0));
            button.setFocusPainted(false);
            button.setOpaque(false);
            button.setContentAreaFilled(false);
            button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            return button;
        }

        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(Color.WHITE);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
            g2.setColor(Ui.LINE);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
            g2.drawLine(28, 5, 28, getHeight() - 6);
            g2.drawLine(getWidth() - 29, 5, getWidth() - 29, getHeight() - 6);
            g2.dispose();
            super.paintComponent(graphics);
        }
    }

    private ImageIcon loadImage(String path, int width, int height) {
        if (path == null || path.trim().isEmpty()) return null;
        ImageIcon original;
        if (path.startsWith("/")) {
            java.net.URL url = getClass().getResource(path);
            if (url == null) return null;
            original = new ImageIcon(url);
        } else {
            File file = new File(path);
            if (!file.isFile()) return null;
            original = new ImageIcon(path);
        }
        return new ImageIcon(original.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH));
    }

    /** Auto-advancing storefront banner backed by the live product catalog. */
    private final class SlidingShowcasePanel extends JPanel {
        private final List<Product> items = new java.util.ArrayList<>();
        private final Map<Integer, Image> imageCache = new HashMap<>();
        private final Timer pauseTimer;
        private final Timer animationTimer;
        private int currentIndex;
        private double progress;
        private boolean sliding;

        SlidingShowcasePanel() {
            setOpaque(false);
            setPreferredSize(new Dimension(390, 228));
            pauseTimer = new Timer(3000, e -> startSlide());
            pauseTimer.setRepeats(false);
            animationTimer = new Timer(20, e -> advanceAnimation());
        }

        void setProducts(List<Product> catalog) {
            int currentId = items.isEmpty() ? -1 : items.get(currentIndex).getId();
            items.clear();
            for (Product product : catalog) {
                if (product.getImagePath() != null && !product.getImagePath().trim().isEmpty()) {
                    items.add(product);
                }
            }
            currentIndex = 0;
            for (int i = 0; i < items.size(); i++) {
                if (items.get(i).getId() == currentId) {
                    currentIndex = i;
                    break;
                }
            }
            imageCache.keySet().removeIf(id -> items.stream().noneMatch(item -> item.getId() == id));
            progress = 0;
            sliding = false;
            animationTimer.stop();
            restartPause();
            repaint();
        }

        private void startSlide() {
            if (items.size() < 2 || sliding) return;
            sliding = true;
            progress = 0;
            animationTimer.start();
        }

        private void advanceAnimation() {
            progress += 0.055;
            if (progress >= 1) {
                currentIndex = (currentIndex + 1) % items.size();
                progress = 0;
                sliding = false;
                animationTimer.stop();
                restartPause();
            }
            repaint();
        }

        private void restartPause() {
            pauseTimer.stop();
            if (isShowing() && items.size() > 1) pauseTimer.restart();
        }

        @Override public void addNotify() {
            super.addNotify();
            restartPause();
        }

        @Override public void removeNotify() {
            pauseTimer.stop();
            animationTimer.stop();
            super.removeNotify();
        }

        @Override protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int width = getWidth();
            int height = getHeight();
            g2.setColor(new Color(248, 247, 237, 28));
            g2.fillRoundRect(0, 0, width, height, 22, 22);
            Shape oldClip = g2.getClip();
            g2.clip(new java.awt.geom.RoundRectangle2D.Double(0, 0, width, height, 22, 22));
            if (items.isEmpty()) {
                g2.setColor(new Color(225, 230, 220));
                g2.setFont(Ui.font(11, Font.PLAIN));
                drawCentered(g2, "Your latest styles will appear here", width / 2, height / 2);
            } else {
                int offset = sliding ? (int) Math.round(ease(progress) * width) : 0;
                drawSlide(g2, items.get(currentIndex), -offset, width, height);
                if (sliding) {
                    Product next = items.get((currentIndex + 1) % items.size());
                    drawSlide(g2, next, width - offset, width, height);
                }
            }
            g2.setClip(oldClip);
            g2.dispose();
        }

        private void drawSlide(Graphics2D g2, Product product, int x, int width, int height) {
            Image image = imageFor(product);
            int imageWidth = 160;
            int imageHeight = 128;
            int imageX = x + 16;
            int imageY = (height - imageHeight) / 2;
            if (image != null) g2.drawImage(image, imageX, imageY, imageWidth, imageHeight, this);

            int textX = x + 194;
            g2.setColor(new Color(220, 228, 216));
            g2.setFont(Ui.font(9, Font.BOLD));
            g2.drawString(product.getCategory().toUpperCase() + "  /  " + product.getSubcategory().toUpperCase(), textX, 53);
            g2.setColor(Color.WHITE);
            g2.setFont(Ui.font(15, Font.BOLD));
            drawClippedString(g2, product.getName(), textX, 81, width - 210);
            g2.setColor(new Color(236, 238, 226));
            g2.setFont(Ui.font(12, Font.BOLD));
            g2.drawString(String.format("₱%,.2f", product.getPrice()), textX, 111);
        }

        private Image imageFor(Product product) {
            if (imageCache.containsKey(product.getId())) return imageCache.get(product.getId());
            ImageIcon icon = loadImage(product.getImagePath(), 160, 128);
            Image image = icon == null ? null : icon.getImage();
            imageCache.put(product.getId(), image);
            return image;
        }

        private void drawClippedString(Graphics2D g2, String text, int x, int y, int availableWidth) {
            FontMetrics metrics = g2.getFontMetrics();
            String value = text;
            while (value.length() > 1 && metrics.stringWidth(value) > availableWidth) {
                value = value.substring(0, value.length() - 1);
            }
            if (!value.equals(text)) value = value.substring(0, Math.max(1, value.length() - 2)) + "…";
            g2.drawString(value, x, y);
        }

        private void drawCentered(Graphics2D g2, String text, int centerX, int baselineY) {
            int textWidth = g2.getFontMetrics().stringWidth(text);
            g2.drawString(text, centerX - textWidth / 2, baselineY);
        }

        private double ease(double value) {
            return value < 0.5 ? 2 * value * value : 1 - Math.pow(-2 * value + 2, 2) / 2;
        }
    }

    /** Independent sliding lookbook using the model photography in Gui_Images. */
    private final class ModelShowcasePanel extends JPanel {
        private final String[] modelPaths = {
            "/Gui_Images/model1.png",
            "/Gui_Images/model2.png",
            "/Gui_Images/model3.png"
        };
        private final Map<String, Image> imageCache = new HashMap<>();
        private final Timer pauseTimer;
        private final Timer animationTimer;
        private int currentIndex;
        private double progress;
        private boolean sliding;

        ModelShowcasePanel() {
            setOpaque(false);
            setPreferredSize(new Dimension(320, 228));
            pauseTimer = new Timer(3900, e -> startSlide());
            pauseTimer.setRepeats(false);
            animationTimer = new Timer(20, e -> advanceAnimation());
        }

        private void startSlide() {
            if (sliding) return;
            sliding = true;
            progress = 0;
            animationTimer.start();
        }

        private void advanceAnimation() {
            progress += 0.045;
            if (progress >= 1) {
                currentIndex = (currentIndex + 1) % modelPaths.length;
                progress = 0;
                sliding = false;
                animationTimer.stop();
                pauseTimer.restart();
            }
            repaint();
        }

        @Override public void addNotify() {
            super.addNotify();
            pauseTimer.restart();
        }

        @Override public void removeNotify() {
            pauseTimer.stop();
            animationTimer.stop();
            super.removeNotify();
        }

        @Override protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int width = getWidth();
            int height = getHeight();
            Shape oldClip = g2.getClip();
            g2.clip(new java.awt.geom.RoundRectangle2D.Double(0, 0, width, height, 22, 22));
            int offset = sliding ? (int) Math.round(ease(progress) * width) : 0;
            drawModel(g2, modelPaths[currentIndex], -offset, width, height);
            if (sliding) {
                drawModel(g2, modelPaths[(currentIndex + 1) % modelPaths.length], width - offset, width, height);
            }
            g2.setClip(oldClip);
            g2.setColor(new Color(255, 255, 255, 90));
            g2.drawRoundRect(0, 0, width - 1, height - 1, 22, 22);
            g2.dispose();
        }

        private void drawModel(Graphics2D g2, String path, int x, int width, int height) {
            g2.setColor(new Color(38, 48, 40));
            g2.fillRect(x, 0, width, height);
            Image image = modelImage(path);
            if (image == null) {
                g2.setColor(new Color(74, 89, 74));
                g2.fillRect(x, 0, width, height);
                return;
            }
            int sourceWidth = image.getWidth(this);
            int sourceHeight = image.getHeight(this);
            int padding = 7;
            double scale = Math.min((double) (width - padding * 2) / sourceWidth,
                    (double) (height - padding * 2) / sourceHeight);
            int drawWidth = (int) Math.ceil(sourceWidth * scale);
            int drawHeight = (int) Math.ceil(sourceHeight * scale);
            int drawX = x + (width - drawWidth) / 2;
            int drawY = (height - drawHeight) / 2;
            g2.drawImage(image, drawX, drawY, drawWidth, drawHeight, this);
        }

        private Image modelImage(String path) {
            if (imageCache.containsKey(path)) return imageCache.get(path);
            java.net.URL url = getClass().getResource(path);
            Image image = url == null ? null : new ImageIcon(url).getImage();
            imageCache.put(path, image);
            return image;
        }

        private double ease(double value) {
            return value < 0.5 ? 2 * value * value : 1 - Math.pow(-2 * value + 2, 2) / 2;
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
            panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 205));
            panel.setPreferredSize(new Dimension(1000, 205));
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
            panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 350));
            panel.setPreferredSize(new Dimension(1000, 350));
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
