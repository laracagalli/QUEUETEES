package gui.customer;

import java.awt.*;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import model.CartItem;
import model.CheckoutDetails;
import model.Order;
import model.User;
import service.StoreService;

/** Customer shopping cart page. */
public final class CartPanel extends JPanel {
    private final StoreService store = StoreService.getInstance();
    private final User user;
    private final Runnable openTracking;
    private final DefaultTableModel model = new DefaultTableModel(new String[]{"Product", "Price", "Quantity", "Subtotal", "Photo"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable table = new JTable(model);
    private final JLabel summary = Ui.label("", 12, Font.BOLD, Ui.INK);
    private final CardLayout checkoutLayout = new CardLayout();
    private final JPanel checkoutViews = new JPanel(checkoutLayout);
    private final JTextField fullName = new JTextField();
    private final JTextField email = new JTextField();
    private final JTextField contact = new JTextField();
    private final JTextArea address = new JTextArea(2, 20);
    private final JComboBox<String> payment = new gui.components.RoundedComboBox<>(new String[]{"GCash", "Card"});
    private final JTextArea notes = new JTextArea(2, 20);
    private final JTextArea receipt = new JTextArea();
    private final CardLayout paymentDetailsLayout = new CardLayout();
    private final JPanel paymentDetails = new JPanel(paymentDetailsLayout);
    private final JTextField cardholderName = new JTextField();
    private final JTextField cardNumber = new JTextField();
    private final JTextField cardExpiry = new JTextField();
    private final JPasswordField cardCvv = new JPasswordField();
    private final JTextField qrReference = new JTextField();
    private final JLabel fullNameError = Ui.label("", 10, Font.PLAIN, new Color(170, 35, 35));
    private final JLabel contactError = Ui.label("", 10, Font.PLAIN, new Color(170, 35, 35));
    private final JLabel referenceError = Ui.label("", 10, Font.PLAIN, new Color(170, 35, 35));
    private List<CartItem> visibleItems;

    public CartPanel(User user, Runnable openTracking) {
        this.user = user;
        this.openTracking = openTracking;
        setOpaque(false);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        Ui.addLeft(this, Ui.label("CART", 10, Font.BOLD, Ui.FOREST));
        add(Box.createVerticalStrut(4));
        Ui.addLeft(this, Ui.label("My cart", 25, Font.BOLD, Ui.INK));
        add(Box.createVerticalStrut(5));
        Ui.addLeft(this, Ui.label("Review selected products before checkout.", 11, Font.PLAIN, Ui.MUTED));
        add(Box.createVerticalStrut(18));
        CheckoutValidation.restrict(fullName, 36, "[A-Za-z ]*",
                "Use letters and spaces only (maximum 36 characters).", fullNameError::setText);
        CheckoutValidation.restrict(contact, 10, "(?:[1-9][0-9]*)?",
                "Enter 10 digits after +63 without a leading 0.", contactError::setText);
        CheckoutValidation.restrict(qrReference, 24, "[0-9]*",
                "Numbers only (maximum 24 digits).", referenceError::setText);
        // Usernames are not verified full names and may contain digits or punctuation.
        fullName.setToolTipText("Full name: letters and spaces, up to 36 characters");
        contact.setToolTipText("Enter 10 digits after +63, without a leading country code");
        qrReference.setToolTipText("Enter the 6-24 digit payment reference number");
        email.setText(user.getEmail());
        email.setEditable(false);
        checkoutViews.setOpaque(false);
        checkoutViews.setAlignmentX(Component.LEFT_ALIGNMENT);
        checkoutViews.add(createCartCard(), "cart");
        checkoutViews.add(createCheckoutPanel(), "checkout");
        checkoutViews.setMaximumSize(new Dimension(Integer.MAX_VALUE, 520));
        checkoutViews.setPreferredSize(new Dimension(1000, 520));
        add(checkoutViews);
        refresh();
    }

    private JPanel createCartCard() {
        JPanel card = Ui.card(Ui.PAPER, 22, true);
        card.setLayout(new BorderLayout());
        Ui.styleTable(table);
        table.setRowHeight(72);
        table.getColumnModel().getColumn(0).setPreferredWidth(360);
        table.getColumnModel().getColumn(0).setCellRenderer(new javax.swing.table.DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable source, Object value,
                    boolean selected, boolean focused, int row, int column) {
                super.getTableCellRendererComponent(source, value, selected, focused, row, column);
                Object photo = source.getModel().getValueAt(source.convertRowIndexToModel(row), 4);
                setIcon(photo instanceof Icon ? (Icon) photo : null);
                setHorizontalAlignment(SwingConstants.LEFT);
                setIconTextGap(12);
                setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
                putClientProperty("html.disable", true);
                return this;
            }
        });
        table.removeColumn(table.getColumnModel().getColumn(4));
        JScrollPane scroll = new gui.components.ModernScrollPane(table);
        scroll.setBorder(null);
        card.add(scroll);
        JPanel actions = new JPanel(new BorderLayout(12, 0));
        actions.setOpaque(false);
        actions.setBorder(new javax.swing.border.EmptyBorder(12, 15, 12, 15));
        actions.add(summary, BorderLayout.WEST);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setOpaque(false);
        gui.components.RoundedButton remove = Ui.lightButton("Remove Selected");
        remove.addActionListener(e -> removeSelected());
        gui.components.RoundedButton checkout = Ui.primaryButton("Check Out");
        checkout.addActionListener(e -> showCheckout());
        buttons.add(remove);
        buttons.add(checkout);
        actions.add(buttons, BorderLayout.EAST);
        card.add(actions, BorderLayout.SOUTH);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 430));
        card.setPreferredSize(new Dimension(1000, 430));
        return card;
    }

    private static final class CheckoutFormStack extends JPanel implements Scrollable {
        public Dimension getPreferredScrollableViewportSize(){return getPreferredSize();}
        public int getScrollableUnitIncrement(Rectangle r,int orientation,int direction){return 20;}
        public int getScrollableBlockIncrement(Rectangle r,int orientation,int direction){return Math.max(20,r.height-20);}
        public boolean getScrollableTracksViewportWidth(){return true;}
        public boolean getScrollableTracksViewportHeight(){return false;}
    }

    private JPanel createCheckoutPanel() {
        JPanel columns = new JPanel(new BorderLayout(24, 0));
        columns.setOpaque(false);

        JPanel formCard = Ui.card(Ui.PAPER, 22, true);
        formCard.setLayout(new BorderLayout());
        formCard.setBorder(new javax.swing.border.EmptyBorder(18, 20, 18, 20));
        JLabel formTitle = Ui.label("Customer & payment details", 16, Font.BOLD, Ui.INK);
        formTitle.setBorder(new javax.swing.border.EmptyBorder(0, 0, 16, 0));
        formTitle.setOpaque(true);formTitle.setBackground(Ui.PAPER);
        formCard.add(formTitle, BorderLayout.NORTH);

        JPanel formStack = new CheckoutFormStack();
        formStack.setOpaque(false);
        formStack.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 16));
        formStack.setLayout(new BoxLayout(formStack, BoxLayout.Y_AXIS));
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        form.setAlignmentX(Component.LEFT_ALIGNMENT);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 0, 0);
        addFormField(form, gbc, "Full name", validatedField(fullName, fullNameError));
        addFormField(form, gbc, "Email", email);
        JPanel phone = new JPanel(new BorderLayout(8, 0));
        phone.setOpaque(false);
        JLabel countryCode = Ui.label("+63", 12, Font.BOLD, Ui.INK);
        countryCode.setHorizontalAlignment(SwingConstants.CENTER);
        countryCode.setPreferredSize(new Dimension(45, 30));
        countryCode.setOpaque(true);
        countryCode.setBackground(Ui.CREAM);
        phone.add(countryCode, BorderLayout.WEST);
        phone.add(contact, BorderLayout.CENTER);
        addFormField(form, gbc, "Contact number", validatedField(phone, contactError));
        address.setLineWrap(true); address.setWrapStyleWord(true);
        addFormField(form, gbc, "Delivery address", new gui.components.ModernScrollPane(address));
        addFormField(form, gbc, "Payment method", payment);
        notes.setLineWrap(true); notes.setWrapStyleWord(true);
        addFormField(form, gbc, "Order notes (optional)", new gui.components.ModernScrollPane(notes));
        gbc.gridy++;
        form.setMinimumSize(new Dimension(0,form.getPreferredSize().height));
        form.setMaximumSize(new Dimension(Integer.MAX_VALUE,form.getPreferredSize().height));
        formStack.add(form);
        formStack.add(Box.createVerticalStrut(22));
        configurePaymentDetails();
        paymentDetails.setOpaque(false);
        paymentDetails.setAlignmentX(Component.LEFT_ALIGNMENT);
        paymentDetails.setPreferredSize(new Dimension(800, 190));
        paymentDetails.setMinimumSize(new Dimension(300, 190));
        paymentDetails.setMaximumSize(new Dimension(Integer.MAX_VALUE, 190));
        formStack.add(paymentDetails);
        formStack.add(Box.createVerticalGlue());
        JScrollPane formScroll = new gui.components.ModernScrollPane(formStack);
        formScroll.setBorder(null);
        formScroll.setOpaque(false);
        formScroll.getViewport().setOpaque(false);
        formScroll.getVerticalScrollBar().setUnitIncrement(14);
        formCard.add(formScroll);
        columns.add(formCard, BorderLayout.CENTER);

        JPanel receiptCard = Ui.card(Color.WHITE, 22, true);
        receiptCard.setLayout(new BorderLayout());
        receiptCard.setBorder(new javax.swing.border.EmptyBorder(18, 20, 18, 20));
        JLabel receiptTitle = Ui.label("Receipt", 16, Font.BOLD, Ui.INK);
        receiptTitle.setHorizontalAlignment(SwingConstants.CENTER);
        receiptCard.add(receiptTitle, BorderLayout.NORTH);
        receipt.setEditable(false);
        receipt.setOpaque(false);
        receipt.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        receipt.setForeground(Ui.INK);
        receipt.setBorder(new javax.swing.border.EmptyBorder(14, 40, 10, 0));
        JScrollPane receiptScroll = new gui.components.ModernScrollPane(receipt);
        receiptScroll.setBorder(null);
        receiptScroll.setOpaque(false);
        receiptScroll.getViewport().setOpaque(false);
        receiptCard.add(receiptScroll);
        JPanel receiptActions = new JPanel(new GridLayout(2, 1, 0, 12));
        receiptActions.setOpaque(false);
        receiptActions.setBorder(new javax.swing.border.CompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Ui.LINE),
                new javax.swing.border.EmptyBorder(18, 0, 0, 0)));
        gui.components.RoundedButton placeOrder = Ui.primaryButton("Place Order");
        placeOrder.setPreferredSize(new Dimension(300, 44));
        placeOrder.addActionListener(e -> placeOrder());
        gui.components.OutlineButton back = new gui.components.OutlineButton("Back to Cart", Ui.FOREST, Ui.FOREST);
        back.setFont(Ui.font(11, Font.BOLD));
        back.setBgColor(new Color(222, 229, 217));
        back.setPreferredSize(new Dimension(300, 42));
        back.addActionListener(e -> checkoutLayout.show(checkoutViews, "cart"));
        receiptActions.add(placeOrder);
        receiptActions.add(back);
        receiptCard.add(receiptActions, BorderLayout.SOUTH);
        receiptCard.setPreferredSize(new Dimension(350, 500));
        columns.add(receiptCard, BorderLayout.EAST);

        payment.addActionListener(e -> {
            showSelectedPaymentDetails();
            updateReceipt();
        });
        watchReceipt(qrReference);
        watchReceipt(cardNumber);
        watchReceipt(fullName);
        watchReceipt(contact);
        showSelectedPaymentDetails();
        return columns;
    }

    private void watchReceipt(JTextField field) {
        field.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { updateReceipt(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { updateReceipt(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { updateReceipt(); }
        });
    }

    private void configurePaymentDetails() {
        JPanel qrPanel = Ui.card(new Color(244, 244, 236), 16, true);
        qrPanel.setLayout(new BorderLayout(12, 8));
        qrPanel.setBorder(new javax.swing.border.EmptyBorder(12, 14, 12, 14));
        JLabel qr = createQrPreview();
        qrPanel.add(qr, BorderLayout.WEST);
        JPanel qrCopy = Ui.verticalBox();
        qrCopy.add(Ui.label("Scan to pay", 12, Font.BOLD, Ui.INK));
        qrCopy.add(Box.createVerticalStrut(5));
        qrCopy.add(Ui.label("Replace payment_qr.png with your QR image.", 10, Font.PLAIN, Ui.MUTED));
        qrCopy.add(Box.createVerticalStrut(10));
        qrCopy.add(Ui.label("Payment reference number", 10, Font.BOLD, Ui.MUTED));
        qrCopy.add(Box.createVerticalStrut(6));
        qrReference.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        qrCopy.add(validatedField(qrReference, referenceError));
        qrPanel.add(qrCopy);
        paymentDetails.add(qrPanel, "qr");

        JPanel cardPanel = Ui.card(new Color(244, 244, 236), 16, true);
        cardPanel.setLayout(new GridBagLayout());
        cardPanel.setBorder(new javax.swing.border.EmptyBorder(12, 14, 12, 14));
        GridBagConstraints cardGbc = new GridBagConstraints();
        cardGbc.gridx = 0; cardGbc.gridy = 0; cardGbc.gridwidth = 2; cardGbc.weightx = 1;
        cardGbc.fill = GridBagConstraints.HORIZONTAL; cardGbc.insets = new Insets(0, 0, 4, 0);
        cardPanel.add(Ui.label("Card details", 12, Font.BOLD, Ui.INK), cardGbc);
        addCardField(cardPanel, cardGbc, "Name on card", cardholderName, 2);
        addCardField(cardPanel, cardGbc, "Card number", cardNumber, 2);
        JPanel shortFields = new JPanel(new GridLayout(1, 2, 8, 0));
        shortFields.setOpaque(false);
        shortFields.add(labeledField("Expiry (MM/YY)", cardExpiry));
        shortFields.add(labeledField("CVV", cardCvv));
        cardGbc.gridx = 0; cardGbc.gridy++; cardGbc.gridwidth = 2;
        cardGbc.insets = new Insets(6, 0, 0, 0);
        cardPanel.add(shortFields, cardGbc);
        paymentDetails.add(cardPanel, "card");
    }

    private JPanel paymentInfoPanel(String text) {
        JPanel panel = Ui.card(new Color(244, 244, 236), 16, true);
        panel.setLayout(new GridBagLayout());
        panel.setBorder(new javax.swing.border.EmptyBorder(18, 20, 18, 20));
        panel.add(Ui.label(text, 11, Font.PLAIN, Ui.MUTED));
        return panel;
    }

    private void addCardField(JPanel panel, GridBagConstraints gbc, String label, JComponent field, int width) {
        gbc.gridy++;
        gbc.gridwidth = width;
        gbc.insets = new Insets(5, gbc.gridx == 1 ? 6 : 0, 3, 0);
        panel.add(Ui.label(label, 10, Font.BOLD, Ui.MUTED), gbc);
        gbc.gridy++;
        gbc.insets = new Insets(0, gbc.gridx == 1 ? 6 : 0, 0, 0);
        field.setPreferredSize(new Dimension(100, 30));
        panel.add(field, gbc);
        if (width == 2) gbc.gridx = 0;
    }

    private JPanel labeledField(String label, JComponent field) {
        JPanel panel = Ui.verticalBox();
        panel.add(Ui.label(label, 10, Font.BOLD, Ui.MUTED));
        panel.add(Box.createVerticalStrut(3));
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        panel.add(field);
        return panel;
    }

    private JLabel createQrPreview() {
        JLabel preview = new JLabel("<html><center>QR IMAGE<br>PLACEHOLDER<br><small>payment_qr.png</small></center></html>", SwingConstants.CENTER);
        preview.setPreferredSize(new Dimension(150, 150));
        preview.setOpaque(true);
        preview.setBackground(Color.WHITE);
        preview.setForeground(Ui.MUTED);
        preview.setBorder(BorderFactory.createDashedBorder(Ui.SAGE, 2, 5));
        java.net.URL url = getClass().getResource("/Gui_Images/payment_qr.png");
        ImageIcon source = url == null ? null : new ImageIcon(url);
        if (source == null) {
            java.io.File developmentFile = new java.io.File("src/Gui_Images/payment_qr.png");
            if (developmentFile.isFile()) source = new ImageIcon(developmentFile.getAbsolutePath());
        }
        if (source != null && source.getIconWidth() > 0) {
            preview.setText("");
            preview.setIcon(new ImageIcon(source.getImage().getScaledInstance(142, 142, Image.SCALE_SMOOTH)));
        }
        return preview;
    }

    private void showSelectedPaymentDetails() {
        String selected = String.valueOf(payment.getSelectedItem());
        paymentDetailsLayout.show(paymentDetails, selected.startsWith("GCash") ? "qr" : selected.startsWith("Card") ? "card" : "cash");
    }

    private void addFormField(JPanel form, GridBagConstraints gbc, String title, JComponent field) {
        gbc.gridy++;
        gbc.insets = new Insets("Delivery address".equals(title) ? 0 : 7, 0, 3, 0);
        form.add(Ui.label(title, 10, Font.BOLD, Ui.MUTED), gbc);
        gbc.gridy++;
        gbc.insets = new Insets(0, 0, 0, 0);
        field.setPreferredSize(new Dimension(100, Boolean.TRUE.equals(field.getClientProperty("validatedField")) ? 50
                : field instanceof JScrollPane ? 43 : 30));
        form.add(field, gbc);
    }

    private JPanel validatedField(JComponent field, JLabel error) {
        JPanel wrapper = new JPanel(new BorderLayout(0, 3));
        wrapper.setOpaque(false);
        wrapper.putClientProperty("validatedField", true);
        wrapper.setAlignmentX(Component.LEFT_ALIGNMENT);
        error.setPreferredSize(new Dimension(100, 17));
        wrapper.add(field, BorderLayout.CENTER);
        wrapper.add(error, BorderLayout.SOUTH);
        wrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        return wrapper;
    }

    private void validateCustomerDetails() {
        fullNameError.setText(CheckoutValidation.nameError(fullName.getText()));
        contactError.setText(CheckoutValidation.contactError(contact.getText()));
        if (!fullNameError.getText().isEmpty()) {
            fullName.requestFocusInWindow();
            throw new IllegalArgumentException(fullNameError.getText());
        }
        if (!contactError.getText().isEmpty()) {
            contact.requestFocusInWindow();
            throw new IllegalArgumentException(contactError.getText());
        }
    }

    private void showCheckout() {
        refresh();
        if (visibleItems.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Add at least one product before checking out.", "Cart", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        updateReceipt();
        checkoutLayout.show(checkoutViews, "checkout");
    }

    public void refresh() {
        visibleItems = store.getCart(user.getId());
        model.setRowCount(0);
        double total = 0;
        for (CartItem item : visibleItems) {
            total += item.getSubtotal();
            model.addRow(new Object[]{item.getProduct().getName(), String.format("₱%,.2f", item.getProduct().getPrice()),
                    item.getQuantity(), String.format("₱%,.2f", item.getSubtotal()),
                    loadProductThumbnail(item.getProduct().getImagePath())});
        }
        summary.setText(visibleItems.isEmpty() ? "Your cart is empty" : String.format("Total: ₱%,.2f", total));
        updateReceipt();
    }

    private ImageIcon loadProductThumbnail(String path) {
        if (path == null || path.trim().isEmpty()) return null;
        ImageIcon original;
        if (path.startsWith("/")) {
            java.net.URL url = getClass().getResource(path);
            if (url == null) return null;
            original = new ImageIcon(url);
        } else {
            java.io.File file = new java.io.File(path);
            if (!file.isFile()) return null;
            original = new ImageIcon(path);
        }
        int sourceWidth = original.getIconWidth();
        int sourceHeight = original.getIconHeight();
        if (sourceWidth <= 0 || sourceHeight <= 0) return null;
        double scale = Math.min(64.0 / sourceWidth, 56.0 / sourceHeight);
        int width = Math.max(1, (int) Math.round(sourceWidth * scale));
        int height = Math.max(1, (int) Math.round(sourceHeight * scale));
        return new ImageIcon(original.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH));
    }

    private void removeSelected() {
        int row = table.getSelectedRow();
        if (row < 0 || row >= visibleItems.size()) {
            JOptionPane.showMessageDialog(this, "Select an item to remove.", "Cart", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        store.removeFromCart(user.getId(), visibleItems.get(row).getProduct().getId());
        refresh();
    }

    private void updateReceipt() {
        if (visibleItems == null) return;
        StringBuilder text = new StringBuilder();
        text.append("       HIRAYA CLOTHING\n");
        text.append("          QUEUETEES\n");
        text.append("--------------------------------\n");
        text.append(java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy  h:mm a"))).append("\n");
        text.append("Customer: ").append(fullName.getText().trim().isEmpty() ? "—" : abbreviate(fullName.getText().trim(), 21)).append("\n");
        text.append("Contact:  ").append(contact.getText().isEmpty() ? "—" : "+63" + contact.getText()).append("\n");
        text.append("--------------------------------\n");
        double total = 0;
        for (CartItem item : visibleItems) {
            total += item.getSubtotal();
            text.append(String.format("%-19s x%-2d %8s\n", abbreviate(item.getProduct().getName(), 19), item.getQuantity(),
                    String.format("₱%,.2f", item.getSubtotal())));
        }
        text.append("--------------------------------\n");
        text.append(String.format("%-23s %8s\n", "SUBTOTAL", String.format("₱%,.2f", total)));
        text.append(String.format("%-23s %8s\n", "TOTAL", String.format("₱%,.2f", total)));
        text.append("\nPayment: ").append(paymentDisplay());
        receipt.setText(text.toString());
        receipt.setCaretPosition(0);
    }

    private String abbreviate(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max - 1) + "…";
    }

    private String paymentDisplay() {
        String selected = String.valueOf(payment.getSelectedItem());
        if (selected.startsWith("Card")) {
            String digits = cardNumber.getText().replaceAll("[^0-9]", "");
            return digits.length() >= 4 ? "Card ending " + digits.substring(digits.length() - 4) : selected;
        }
        if (selected.startsWith("GCash") && !qrReference.getText().trim().isEmpty()) {
            return "GCash ref. " + qrReference.getText().trim();
        }
        return selected;
    }

    private void validatePaymentDetails() {
        String selected = String.valueOf(payment.getSelectedItem());
        if (selected.startsWith("GCash")) {
            referenceError.setText(CheckoutValidation.referenceError(qrReference.getText()));
            if (!referenceError.getText().isEmpty()) {
                qrReference.requestFocusInWindow();
                throw new IllegalArgumentException(referenceError.getText());
            }
        } else if (selected.startsWith("Card")) {
            String digits = cardNumber.getText().replaceAll("[^0-9]", "");
            String expiry = cardExpiry.getText().trim();
            char[] cvvChars = cardCvv.getPassword();
            String cvv = new String(cvvChars);
            java.util.Arrays.fill(cvvChars, '\0');
            if (cardholderName.getText().trim().isEmpty()) throw new IllegalArgumentException("Name on card is required.");
            if (!digits.matches("\\d{13,19}")) throw new IllegalArgumentException("Enter a valid 13–19 digit card number.");
            if (!expiry.matches("(0[1-9]|1[0-2])/\\d{2}")) throw new IllegalArgumentException("Enter the expiry as MM/YY.");
            if (!cvv.matches("\\d{3,4}")) throw new IllegalArgumentException("Enter a valid 3 or 4 digit CVV.");
        }
    }

    private void placeOrder() {
        try {
            validateCustomerDetails();
            validatePaymentDetails();
            String savedPayment = paymentDisplay();
            CheckoutDetails details = new CheckoutDetails(fullName.getText().trim(), user.getEmail(), "+63" + contact.getText(),
                    "Delivery", address.getText().trim(),
                    savedPayment, notes.getText().trim());
            Order order = store.checkout(user.getId(), details);
            JOptionPane.showMessageDialog(this, "Order confirmed. Your queue number is Q-" + String.format("%03d", order.getQueueNumber()) + ".",
                    "Order placed", JOptionPane.INFORMATION_MESSAGE);
            refresh();
            cardNumber.setText("");
            cardExpiry.setText("");
            cardCvv.setText("");
            qrReference.setText("");
            checkoutLayout.show(checkoutViews, "cart");
            openTracking.run();
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Unable to place order", JOptionPane.WARNING_MESSAGE);
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
