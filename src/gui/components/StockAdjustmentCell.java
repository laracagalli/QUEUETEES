package gui.components;

import java.awt.*;
import java.util.function.BiConsumer;
import javax.swing.*;
import javax.swing.table.*;
import model.Product;

/** Row-specific minus / quantity / plus controls. Each click requests a confirmed adjustment. */
public final class StockAdjustmentCell extends AbstractCellEditor implements TableCellRenderer, TableCellEditor {
    private final java.util.function.IntFunction<Product> productAt;
    private final BiConsumer<Product, Integer> adjust;
    private final JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 7));
    private final JTextField amount = new JTextField("1");
    private final JButton minus = button("−", "Decrease stock"), plus = button("+", "Increase stock");
    private Product editingProduct;
    public StockAdjustmentCell(java.util.function.IntFunction<Product> productAt, BiConsumer<Product, Integer> adjust) {
        this.productAt = productAt; this.adjust = adjust;
        amount.setHorizontalAlignment(SwingConstants.CENTER);
        amount.setToolTipText("Quantity to add or remove; click − or + to confirm the change");
        amount.setPreferredSize(new Dimension(42, 32));
        amount.getAccessibleContext().setAccessibleName("Stock adjustment quantity");
        panel.add(minus); panel.add(amount); panel.add(plus);
        minus.addActionListener(e -> submit(-1));
        plus.addActionListener(e -> submit(1));
    }
    private static JButton button(String symbol, String label) {
        JButton button = new JButton(symbol);
        button.putClientProperty("queuetees.preserveButtonStyle", true);
        button.setFont(new Font("Segoe UI", Font.BOLD, 16));
        button.setForeground(AppTheme.GREEN); button.setBackground(Color.WHITE);
        button.setBorder(BorderFactory.createLineBorder(AppTheme.LINE));
        button.setPreferredSize(new Dimension(32, 32));
        button.setToolTipText(label); button.getAccessibleContext().setAccessibleName(label);
        return button;
    }
    private void submit(int direction) {
        Product product = editingProduct;
        try {
            int quantity = Integer.parseInt(amount.getText().trim());
            if (quantity <= 0) throw new NumberFormatException();
            stopCellEditing();
            adjust.accept(product, direction * quantity);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(panel, "Enter a positive whole-number quantity.", "Invalid quantity", JOptionPane.WARNING_MESSAGE);
            amount.requestFocusInWindow();
        }
    }
    private Component configure(JTable table, int row, int column, boolean selected) {
        Product product = productAt.apply(table.convertRowIndexToModel(row));
        minus.setEnabled(product.getStock() > 0);
        plus.setEnabled(product.getStock() < Integer.MAX_VALUE);
        panel.setBackground(selected ? table.getSelectionBackground() : row % 2 == 0 ? AppTheme.PAPER : new Color(246,248,241));
        panel.setSize(table.getColumnModel().getColumn(column).getWidth(), table.getRowHeight(row));
        panel.doLayout();
        return panel;
    }
    public Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean focus, int row, int column) {
        amount.setText("1"); return configure(table, row, column, selected);
    }
    public Component getTableCellEditorComponent(JTable table, Object value, boolean selected, int row, int column) {
        editingProduct = productAt.apply(table.convertRowIndexToModel(row));
        amount.setText("1"); return configure(table, row, column, true);
    }
    public Object getCellEditorValue() { return ""; }
}
